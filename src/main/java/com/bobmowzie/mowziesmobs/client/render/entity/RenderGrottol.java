package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelGrottol;
import com.bobmowzie.mowziesmobs.server.entity.grottol.EntityGrottol;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Created by BobMowzie on 5/8/2017.
 * <p>
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section): {@link ModelGrottol}
 * extends LLibrary's {@code AdvancedModelBase}, which can no longer be the model type parameter of
 * {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>} carrying a live entity
 * reference, same pattern as the other LLibrary-model renderers in this scope. The texture selection
 * (blackpink/deepslate variants) is live-entity-derived - captured directly during extraction.
 */
public class RenderGrottol extends EntityRenderer<EntityGrottol, RenderGrottol.GrottolRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/grottol.png");
    private static final Identifier TEXTURE_DEEPSLATE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/grottol_deepslate.png");
    private static final Identifier TEXTURE_BLACKPINK = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/grottol_blackpink.png");
    private static final Identifier TEXTURE_DEEPSLATE_BLACKPINK = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/grottol_deepslate_blackpink.png");

    private final ModelGrottol<EntityGrottol> model = new ModelGrottol<>();

    public RenderGrottol(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    private static Identifier getTextureLocation(EntityGrottol entity) {
        if (entity.getBlackpink()) {
            return entity.getDeepslate() ? TEXTURE_DEEPSLATE_BLACKPINK : TEXTURE_BLACKPINK;
        } else {
            return entity.getDeepslate() ? TEXTURE_DEEPSLATE : TEXTURE;
        }
    }

    @Override
    public GrottolRenderState createRenderState() {
        return new GrottolRenderState();
    }

    @Override
    public void extractRenderState(EntityGrottol entity, GrottolRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.texture = getTextureLocation(entity);
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(GrottolRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        renderTasks.submitCustomGeometry(poseStack, model.renderType(state.texture), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            model.setupAnim(state.entity, 0, 0, state.ageInTicks, 0, 0);
            model.renderToBuffer(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
        });

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class GrottolRenderState extends EntityRenderState {
        public EntityGrottol entity;
        public Identifier texture;
        public float yRot;
    }
}
