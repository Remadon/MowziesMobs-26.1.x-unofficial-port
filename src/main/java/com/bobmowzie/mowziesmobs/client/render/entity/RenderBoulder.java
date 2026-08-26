package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelBoulder;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.BlockLayer;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityBoulderBase;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityBoulderSculptor;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityGeomancyBase;
import com.ilexiconn.llibrary.client.model.tools.AdvancedModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Quaternionf;

/**
 * PORTING NOTE: {@link ModelBoulder} extends LLibrary's {@code AdvancedModelBase} - the render state captures a live
 * entity reference, matching the established pattern used throughout this scope. Block rendering via
 * {@code BlockRenderDispatcher#renderSingleBlock} no longer exists - replaced with
 * {@code BlockLayer#processModelRenderer}'s new {@code BlockModelResolver}-driven signature (see that class's
 * javadoc), called directly against the live {@code poseStack}/{@code renderTasks} since it isn't wrapped in
 * {@code submitCustomGeometry} (no deferred-pose concern here - see PORTING_NOTES.md's "submitCustomGeometry
 * deferred pose" finding for when that matters). The old per-block-state `texMap`/`getTextureLocation()` override was
 * dead code even pre-port (nothing in the old body ever called `getTextureLocation()` - the block is drawn via its
 * real resolved block model, not a flat texture) and has been dropped rather than carried forward unused; confirmed
 * via grep that nothing outside this file referenced `RenderBoulder`'s texture fields either.
 */
public class RenderBoulder extends EntityRenderer<EntityBoulderBase, RenderBoulder.BoulderRenderState> {
    ModelBoulder model;

    public RenderBoulder(EntityRendererProvider.Context mgr) {
        super(mgr);
        model = new ModelBoulder();
    }

    @Override
    public BoulderRenderState createRenderState() {
        return new BoulderRenderState();
    }

    @Override
    public void extractRenderState(EntityBoulderBase entity, BoulderRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.partialTicks = partialTicks;
    }

    @Override
    public void submit(BoulderRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntityBoulderBase entityIn = state.entity;

        if (entityIn.isActive()) {
            float frame = entityIn.risingTick + state.partialTicks;
            poseStack.pushPose();
            model.setupAnim(entityIn, 0, 0, frame, 0, 0);
            if (entityIn instanceof EntityBoulderSculptor.EntityBoulderSculptorCrumbling crumbling) {
                if (crumbling.isCrumbling()) {
                    poseStack.mulPose(new Quaternionf().rotationXYZ(0.03f * (float) Math.cos(frame * 4f + 234f), 0.03f * (float) Math.cos(frame * 2.2f + 45), 0.03f * (float) Math.cos(frame * 3.5f + 409)));
                }
            }
            BlockModelResolver blockModelResolver = Minecraft.getInstance().getBlockModelResolver();
            AdvancedModelRenderer root;
            if (entityIn.boulderSize == EntityGeomancyBase.GeomancyTier.SMALL) root = model.boulder0block1;
            else if (entityIn.boulderSize == EntityGeomancyBase.GeomancyTier.MEDIUM) root = model.boulder1;
            else if (entityIn.boulderSize == EntityGeomancyBase.GeomancyTier.LARGE) root = model.boulder2;
            else root = model.boulder3;
            poseStack.translate(-0.5f, 0.5f, -0.5f);
            BlockLayer.processModelRenderer(root, poseStack, renderTasks, state.lightCoords, OverlayTexture.NO_OVERLAY, blockModelResolver);
            poseStack.popPose();
        }

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class BoulderRenderState extends EntityRenderState {
        public EntityBoulderBase entity;
        public float partialTicks;
    }
}
