package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelLantern;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.LanternGelLayer;
import com.bobmowzie.mowziesmobs.server.entity.lantern.EntityLantern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Created by BobMowzie on 5/8/2017.
 * <p>
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section, and
 * {@code client/render/entity/layer/LanternGelLayer.java}'s javadoc for the layer-side half of this fix):
 * {@link ModelLantern} extends LLibrary's {@code AdvancedModelBase}, which can no longer be the model type parameter
 * of {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>}. {@link LanternGelLayer}
 * is no longer a real {@code RenderLayer} (see its own javadoc) so its {@code render(...)} is now called directly
 * from this class's {@link #submit}, wrapped in its own {@code submitCustomGeometry} call with the gel's own
 * translucent render type.
 */
public class RenderLantern extends EntityRenderer<EntityLantern, RenderLantern.LanternRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/mmlantern.png");

    private final ModelLantern<EntityLantern> model = new ModelLantern<>();
    private final LanternGelLayer<EntityLantern> gelLayer = new LanternGelLayer<>();

    public RenderLantern(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    protected int getBlockLightLevel(EntityLantern entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public LanternRenderState createRenderState() {
        return new LanternRenderState();
    }

    @Override
    public void extractRenderState(EntityLantern entity, LanternRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(LanternRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        renderTasks.submitCustomGeometry(poseStack, model.renderType(TEXTURE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(state.entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        renderTasks.submitCustomGeometry(poseStack, LanternGelLayer.renderType(TEXTURE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            gelLayer.render(poseStack, vertexConsumer, state.lightCoords, state.entity, 0, 0, state.partialTick, state.ageInTicks, 0, 0);
            poseStack.popPose();
        });

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class LanternRenderState extends EntityRenderState {
        public EntityLantern entity;
        public float yRot;
    }
}
