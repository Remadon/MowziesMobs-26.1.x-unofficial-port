package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelDynamicsTester;
import com.bobmowzie.mowziesmobs.server.entity.EntityDynamicsTester;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Created by BobMowzie on 8/30/2018.
 * <p>
 * PORTING NOTE: {@link ModelDynamicsTester} extends LLibrary's {@code AdvancedModelBase} (not vanilla's
 * {@code EntityModel}), which can no longer be the model type parameter of {@code MobRenderer<T,S,M>} (see
 * PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section) - ported to a plain
 * {@code EntityRenderer<T,XRenderState>} carrying a live entity reference, same pattern as the other LLibrary-model
 * renderers in this scope.
 */
public class RenderDynamicsTester extends EntityRenderer<EntityDynamicsTester, RenderDynamicsTester.DynamicsTesterRenderState> {
    private static final Identifier TEXTURE_STONE = Identifier.withDefaultNamespace("textures/blocks/stone.png");
    private final ModelDynamicsTester<EntityDynamicsTester> model = new ModelDynamicsTester<>();

    public RenderDynamicsTester(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public DynamicsTesterRenderState createRenderState() {
        return new DynamicsTesterRenderState();
    }

    @Override
    public void extractRenderState(EntityDynamicsTester entity, DynamicsTesterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
    }

    @Override
    public void submit(DynamicsTesterRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();

        // NOTE: submitCustomGeometry's callback runs later (deferred) - see RenderPoisonBall.java's javadoc /
        // PORTING_NOTES.md's "submitCustomGeometry deferred pose" finding for why this restores `pose` explicitly.
        renderTasks.submitCustomGeometry(poseStack, model.renderType(TEXTURE_STONE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(state.entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class DynamicsTesterRenderState extends EntityRenderState {
        public EntityDynamicsTester entity;
    }
}
