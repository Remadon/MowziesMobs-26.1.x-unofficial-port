package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelIceBall;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntityIceBall;
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
 * PORTING NOTE (see PORTING_NOTES.md "Plain vanilla EntityRenderer<T> subclasses" section for the general pattern):
 * {@code EntityRenderer<T>} lost its old single-method {@code render(T, float, float, PoseStack, MultiBufferSource,
 * int)} override point in favor of {@code extractRenderState}/{@code submit} split across a second RenderState type
 * parameter. {@code ModelIceBall} extends LLibrary's {@code AdvancedModelBase} (not vanilla's {@code Model}), whose
 * {@code setupAnim}/{@code renderToBuffer} signatures are unaffected by the vanilla changes - it just still wants a
 * live entity reference, which is now captured into {@link IceBallRenderState} during extraction.
 */
public class RenderIceBall extends EntityRenderer<EntityIceBall, RenderIceBall.IceBallRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/ice_ball.png");
    public ModelIceBall<EntityIceBall> model;

    public RenderIceBall(EntityRendererProvider.Context mgr) {
        super(mgr);
        model = new ModelIceBall<>();
    }

    @Override
    public IceBallRenderState createRenderState() {
        return new IceBallRenderState();
    }

    @Override
    public void extractRenderState(EntityIceBall entity, IceBallRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(IceBallRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YN.rotation(state.yRot * (float) Math.PI / 180f));
        poseStack.translate(0, -0.5f, 0);

        RenderType renderType = RenderTypes.entityTranslucent(TEXTURE);

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

    public static class IceBallRenderState extends EntityRenderState {
        public EntityIceBall entity;
        public float yRot;
    }
}
