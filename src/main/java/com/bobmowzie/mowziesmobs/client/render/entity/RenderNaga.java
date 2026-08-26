package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelNaga;
import com.bobmowzie.mowziesmobs.client.render.MowzieRenderUtils;
import com.bobmowzie.mowziesmobs.server.entity.naga.EntityNaga;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section): {@link ModelNaga}
 * extends LLibrary's {@code AdvancedModelBase}, which can no longer be the model type parameter of
 * {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>}. Mouth-socket position
 * capture uses the same "re-pose the shared model synchronously before reading" technique as
 * {@code RenderFrostmaw.java} - see that class's javadoc for the full reasoning.
 * <p>
 * Also resolves the {@code Entity#getBoundingBoxForCulling()} FIXME left in {@code EntityNaga.java} (see
 * PORTING_NOTES.md "Entity#getBoundingBoxForCulling() REMOVED" section): the old override (inflating the culling box
 * by 12 blocks so the naga's long body doesn't pop out of view early) moved here, onto
 * {@link #getBoundingBoxForCulling}, the new client-side-only override point - value confirmed via git history of
 * the pre-port {@code EntityNaga#getBoundingBoxForCulling()}.
 */
public class RenderNaga extends EntityRenderer<EntityNaga, RenderNaga.NagaRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/naga.png");

    private final ModelNaga<EntityNaga> model = new ModelNaga<>();

    public RenderNaga(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    protected AABB getBoundingBoxForCulling(EntityNaga entity) {
        return super.getBoundingBoxForCulling(entity).inflate(12.0D);
    }

    @Override
    public NagaRenderState createRenderState() {
        return new NagaRenderState();
    }

    @Override
    public void extractRenderState(EntityNaga entity, NagaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(NagaRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        EntityNaga entity = state.entity;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        renderTasks.submitCustomGeometry(poseStack, model.renderType(TEXTURE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);

        if (entity.getAnimation() == EntityNaga.SPIT_ANIMATION && entity.mouthPos != null && entity.mouthPos.length > 0) {
            // NOTE: re-pose the shared model synchronously so this read reflects the current frame - see
            // RenderFrostmaw.java's javadoc for the full reasoning (same technique).
            model.setupAnim(entity, 0, 0, state.ageInTicks, 0, 0);
            entity.mouthPos[0] = MowzieRenderUtils.getWorldPosFromModel(entity, state.yRot, model.mouthSocket);
        }
    }

    public static class NagaRenderState extends EntityRenderState {
        public EntityNaga entity;
        public float yRot;
    }
}
