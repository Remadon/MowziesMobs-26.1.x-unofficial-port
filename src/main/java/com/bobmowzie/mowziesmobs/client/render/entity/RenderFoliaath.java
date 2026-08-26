package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelFoliaath;
import com.bobmowzie.mowziesmobs.server.entity.foliaath.EntityFoliaath;
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
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section): {@link ModelFoliaath}
 * extends LLibrary's {@code AdvancedModelBase}, which can no longer be the model type parameter of
 * {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>} carrying a live entity
 * reference, same pattern as the other LLibrary-model renderers in this scope.
 */
public class RenderFoliaath extends EntityRenderer<EntityFoliaath, RenderFoliaath.FoliaathRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/foliaath.png");

    private final ModelFoliaath<EntityFoliaath> model = new ModelFoliaath<>();

    public RenderFoliaath(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public FoliaathRenderState createRenderState() {
        return new FoliaathRenderState();
    }

    @Override
    public void extractRenderState(EntityFoliaath entity, FoliaathRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(FoliaathRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
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

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class FoliaathRenderState extends EntityRenderState {
        public EntityFoliaath entity;
        public float yRot;
    }
}
