package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelPoisonBall;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityPoisonBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE (see PORTING_NOTES.md "Plain vanilla EntityRenderer<T> subclasses" / "CORRECTION/refinement: LLibrary
 * models are UNAFFECTED by the vanilla Model.setupAnim(S) change" sections): {@link ModelPoisonBall} extends
 * LLibrary's {@code AdvancedModelBase}, which still wants a live entity - captured directly into the custom render
 * state below rather than decomposed into fields.
 */
public class RenderPoisonBall extends EntityRenderer<EntityPoisonBall, RenderPoisonBall.PoisonBallRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/poison_ball.png");
    public ModelPoisonBall model;

    public RenderPoisonBall(EntityRendererProvider.Context mgr) {
        super(mgr);
        model = new ModelPoisonBall();
    }

    @Override
    public PoisonBallRenderState createRenderState() {
        return new PoisonBallRenderState();
    }

    @Override
    public void extractRenderState(EntityPoisonBall entity, PoisonBallRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(PoisonBallRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotationDegrees(state.yRot));

        RenderType renderType = RenderTypes.entityTranslucent(TEXTURE);

        // NOTE: submitCustomGeometry's callback runs later (deferred) - `pose` is a snapshot of poseStack.last()
        // taken at call time, not a live view of the outer `poseStack`. Restore it onto a fresh pushed pose before
        // calling into APIs (like model.renderToBuffer) that read PoseStack.last() themselves. See
        // PORTING_NOTES.md's "submitCustomGeometry deferred pose" finding.
        renderTasks.submitCustomGeometry(poseStack, renderType, (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(state.entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class PoisonBallRenderState extends EntityRenderState {
        public EntityPoisonBall entity;
        public float yRot;
    }
}
