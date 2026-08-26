package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelSuperNova;
import com.bobmowzie.mowziesmobs.client.render.MMRenderType;
import com.bobmowzie.mowziesmobs.server.entity.effects.EntitySuperNova;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * PORTING NOTE: see RenderPoisonBall.java / PORTING_NOTES.md for the general LLibrary-model live-entity-capture
 * pattern. {@code getTextureLocation(EntitySuperNova)} used {@code entity.tickCount} to cycle through 16 frame
 * textures - captured into the render state during extraction instead of re-derived from a render-state-only method,
 * since the new {@code EntityRenderer} base no longer has a {@code getTextureLocation} override point at all.
 */
public class RenderSuperNova extends EntityRenderer<EntitySuperNova, RenderSuperNova.SuperNovaRenderState> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova.png");
    public static final Identifier[] TEXTURES = new Identifier[] {
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_1.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_2.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_3.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_4.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_5.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_6.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_7.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_8.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_9.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_10.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_11.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_12.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_13.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_14.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_15.png"),
            Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/effects/super_nova_16.png")
    };
    public ModelSuperNova<EntitySuperNova> model;

    public RenderSuperNova(EntityRendererProvider.Context mgr) {
        super(mgr);
        model = new ModelSuperNova<>();
    }

    @Override
    public SuperNovaRenderState createRenderState() {
        return new SuperNovaRenderState();
    }

    @Override
    public void extractRenderState(EntitySuperNova entity, SuperNovaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.texture = TEXTURES[entity.tickCount % TEXTURES.length];
    }

    @Override
    public void submit(SuperNovaRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();

        RenderType renderType = MMRenderType.getGlowingEffect(state.texture);

        // NOTE: submitCustomGeometry's callback runs later (deferred) - see RenderPoisonBall.java's javadoc /
        // PORTING_NOTES.md's "submitCustomGeometry deferred pose" finding for why this restores `pose` explicitly.
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

    public static class SuperNovaRenderState extends EntityRenderState {
        public EntitySuperNova entity;
        public Identifier texture;
    }
}
