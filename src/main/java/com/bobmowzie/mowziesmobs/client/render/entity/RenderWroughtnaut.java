package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelWroughtnaut;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.ItemLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.WroughtnautEyesLayer;
import com.bobmowzie.mowziesmobs.server.entity.wroughtnaut.EntityWroughtnaut;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;

/**
 * PORTING NOTE (see PORTING_NOTES.md "MobRenderer-based ones using LLibrary models" section and
 * {@code client/render/entity/layer/ItemLayer.java}/{@code WroughtnautEyesLayer.java}'s javadocs for the layer-side
 * half of this fix): {@link ModelWroughtnaut} extends LLibrary's {@code AdvancedModelBase}, which can no longer be
 * the model type parameter of {@code MobRenderer<T,S,M>} - ported to a plain {@code EntityRenderer<T,XRenderState>}.
 * {@link ItemLayer}/{@link WroughtnautEyesLayer} are no longer real {@code RenderLayer}s so both are now called
 * directly from this class's {@link #submit}.
 * <p>
 * <b>Dropped, not silently</b>: the old debug hitbox-line overlay (forward/body-facing indicator vectors, drawn only
 * when {@code F3+B} hitboxes are on) relied on {@code EntityRenderDispatcher#shouldRenderHitBoxes()}, which no
 * longer exists anywhere in vanilla (grep-confirmed zero hits in the full 26.1.2 source). Per-entity-renderer hitbox
 * debug drawing was replaced engine-wide by a generic {@code net.minecraft.client.renderer.debug
 * .EntityHitboxDebugRenderer} (a {@code DebugRenderer.SimpleDebugRenderer}, driven by the new {@code Gizmos}/
 * {@code GizmoStyle} debug-draw API) that already draws hitbox/orientation gizmos for every entity automatically -
 * this mod's bespoke duplicate is now fully redundant rather than needing a replacement, so it was removed outright.
 */
public class RenderWroughtnaut extends EntityRenderer<EntityWroughtnaut, RenderWroughtnaut.WroughtnautRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/wroughtnaut.png");

    private final ModelWroughtnaut<EntityWroughtnaut> model = new ModelWroughtnaut<>();
    private final WroughtnautEyesLayer<EntityWroughtnaut> eyesLayer = new WroughtnautEyesLayer<>();
    private final ItemLayer<WroughtnautRenderState, EntityWroughtnaut> swordLayer = new ItemLayer<>(
            state -> state.entity, model.sword, () -> Items.DIAMOND_SWORD.getDefaultInstance(), ItemDisplayContext.GROUND);

    public RenderWroughtnaut(EntityRendererProvider.Context mgr) {
        super(mgr);
    }

    @Override
    public WroughtnautRenderState createRenderState() {
        return new WroughtnautRenderState();
    }

    @Override
    public void extractRenderState(EntityWroughtnaut entity, WroughtnautRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        state.entity = entity;
        state.yRot = entity.getYRot(partialTicks);
    }

    @Override
    public void submit(WroughtnautRenderState state, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState) {
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

        renderTasks.submitCustomGeometry(poseStack, WroughtnautEyesLayer.renderType(TEXTURE), (pose, vertexConsumer) -> {
            poseStack.pushPose();
            poseStack.last().set(pose);
            eyesLayer.render(poseStack, vertexConsumer, state.lightCoords, OverlayTexture.NO_OVERLAY, state.entity, 0, 0, state.partialTick, state.ageInTicks, 0, 0);
            poseStack.popPose();
        });

        swordLayer.submit(poseStack, renderTasks, state.lightCoords, state);

        poseStack.popPose();

        super.submit(state, poseStack, renderTasks, cameraState);
    }

    public static class WroughtnautRenderState extends EntityRenderState {
        public EntityWroughtnaut entity;
        public float yRot;
    }
}
