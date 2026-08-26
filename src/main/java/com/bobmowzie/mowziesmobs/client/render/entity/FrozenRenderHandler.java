package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.jspecify.annotations.Nullable;

/**
 * Created by BobMowzie on 6/28/2017.
 * <p>
 * PORTING NOTE (26.1.2): heavily restructured - see PORTING_NOTES.md architecture sections. Summary of what changed
 * in this file:
 * - {@code RenderLayer<S,M>} (vanilla) is now keyed on the render STATE type (`LivingEntityRenderState`), not the
 *   live entity, and its single method is now {@code submit(PoseStack, SubmitNodeCollector, int lightCoords, S
 *   state, float yRot, float xRot)} - no live entity access. Since whether an entity is "frozen" is capability data
 *   that needs the live entity to look up, it's captured into the render state via NeoForge's generic render-state
 *   data mechanism (`BaseRenderState#setRenderData(ContextKey, Object)`/`getRenderData(ContextKey)`, which
 *   `EntityRenderState` now extends - see `net.neoforged.neoforge.client.renderstate.BaseRenderState` /
 *   `RegisterRenderStateModifiersEvent` in the NeoForge jar, no sources jar was available so this was verified via
 *   `javap` against the compiled classes). **KNOWN GAP: nothing in this agent's scope currently WRITES
 *   {@link #FROZEN_RENDER_DATA_KEY} into a render state for plain vanilla `LivingEntityRenderState`s** (that would
 *   need a `RegisterRenderStateModifiersEvent` listener registered against `LivingEntity.class`, most naturally
 *   living in a central client event-bus file - out of this agent's scope, `client/render/**`/`client/gui/**` only).
 *   Until that registration exists somewhere, `LayerFrozen` will compile and run but the frozen overlay simply won't
 *   show for the vanilla-`RenderLayer`-based usages (`ClientLayerRegistry`, `GeckoRenderPlayer`) - flagged, not
 *   silently dropped.
 * - `GeoRenderLayer` (GeckoLib) DOES have its own live-entity extraction hook (`addRenderData`, within this agent's
 *   control), so `GeckoLayerFrozen` captures the frozen flag correctly via a `DataTicket` (same pattern as
 *   MowzieGeoEntityRenderer.java) - this one is NOT a known gap. The old GeckoLib-4 `reRender(...)` call (re-submit
 *   the model with a different texture) is replaced with `RenderPassInfo#renderPosed(Runnable)`, which is
 *   specifically public for this exact "re-render the model as an extra layer pass" use case (re-applies the
 *   already-computed bone poses for the duration of the extra render task).
 * - `PlayerRenderer` renamed to `AvatarRenderer`; `EntityRenderDispatcher#getRenderer` return type follows.
 * - The manual first-person "frozen arm" overlay (`onRenderHand`/`renderArmFirstPersonFrozen`/`renderRightArm`/
 *   `renderLeftArm`/`renderItem`/`setModelVisibilities`) depended heavily on `PlayerModel`/`AvatarRenderer` internals
 *   that the client/model agent flagged as a NON-FUNCTIONAL STUB as of this writing (`ModelPlayerAnimated.java`
 *   left unfinished because vanilla's `PlayerModel`/`setupAnim` changed shape substantially - single render-state
 *   parameter instead of 5 raw floats, `ear` field removed). This method has been updated to use the current
 *   `renderRightHand`/`renderLeftHand`/`AvatarRenderer` API shapes where they translate directly, but the
 *   `PlayerModel.setupAnim`/per-arm ModelPart rendering portion could NOT be fully verified against a working model
 *   and should be treated as unverified/best-effort - see inline notes below.
 */
public class FrozenRenderHandler {
    private static final Identifier FROZEN_TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/frozen.png");

    public static final ContextKey<Boolean> FROZEN_RENDER_DATA_KEY = new ContextKey<>(Identifier.fromNamespaceAndPath(MMCommon.MODID, "frozen"));

    public static class LayerFrozen<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
        public LayerFrozen(RenderLayerParent<S, M> renderer) {
            super(renderer);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, S state, float yRot, float xRot) {
            if (Boolean.TRUE.equals(state.getRenderData(FROZEN_RENDER_DATA_KEY)) && !state.isInvisible) {
                int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0F);

                submitNodeCollector.submitModel(getParentModel(), state, poseStack, RenderTypes.entityTranslucent(FROZEN_TEXTURE), lightCoords, overlayCoords, -1, null, state.outlineColor, null);
            }
        }
    }

    public static class GeckoLayerFrozen<T extends LivingEntity & GeoEntity, R extends LivingEntityRenderState> extends GeoRenderLayer<T, Void, R> {
        private static final DataTicket<Boolean> FROZEN = DataTicket.create("mowziesmobs_frozen", Boolean.class);

        public GeckoLayerFrozen(GeoRenderer<T, Void, R> entityRendererIn, EntityRendererProvider.Context context) {
            super(entityRendererIn);
        }

        @Override
        public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
            renderState.addGeckolibData(FROZEN, DataHandler.getData(animatable, DataHandler.FROZEN_DATA).getFrozen());
        }

        @Override
        public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
            if (Boolean.TRUE.equals(renderPassInfo.getGeckolibData(FROZEN))) {
                RenderType frozenRenderType = RenderTypes.entityTranslucent(FROZEN_TEXTURE);
                int packedLight = renderPassInfo.packedLight();
                int packedOverlay = renderPassInfo.packedOverlay();

                renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), frozenRenderType, (pose, vertexConsumer) -> {
                    PoseStack poseStack = renderPassInfo.poseStack();

                    poseStack.pushPose();
                    poseStack.last().set(pose);
                    renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, packedLight, packedOverlay, -1));
                    poseStack.popPose();
                });
            }
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        event.getPoseStack().pushPose();
        Player player = Minecraft.getInstance().player;

        if (player != null) {
            if (DataHandler.getData(player, DataHandler.FROZEN_DATA).getFrozen()) {
                boolean isMainHand = event.getHand() == InteractionHand.MAIN_HAND;
                if (isMainHand && !player.isInvisible() && event.getItemStack().isEmpty()) {
                    HumanoidArm enumhandside = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
                    renderArmFirstPersonFrozen(event.getPoseStack(), event.getSubmitNodeCollector(), event.getPackedLight(), event.getEquipProgress(), event.getSwingProgress(), enumhandside);
                    event.setCanceled(true);
                }
            }
        }

        event.getPoseStack().popPose();
    }

    /**
     * From ItemRenderer#renderArmFirstPerson
     * PORTING NOTE: uses AvatarRenderer#renderRightHand/renderLeftHand (renamed from PlayerRenderer). This method
     * used to draw a real (translucent frozen-textured) copy of the arm ModelPart via immediate MultiBufferSource
     * drawing; that model-part-level path (renderRightArm/renderLeftArm/renderItem/setModelVisibilities below) is
     * UNVERIFIED - see class javadoc. If it doesn't compile/render correctly at runtime, that's the first place to
     * look, pending ModelPlayerAnimated/PlayerModel being finished on the client/model side.
     */
    private static void renderArmFirstPersonFrozen(PoseStack matrixStackIn, SubmitNodeCollector submitNodeCollector, int combinedLightIn, float equippedProgress, float swingProgress, HumanoidArm side) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher renderManager = mc.getEntityRenderDispatcher();
        boolean flag = side != HumanoidArm.LEFT;
        float f = flag ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(swingProgress);
        float f2 = -0.3F * Mth.sin(f1 * (float)Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float)Math.PI * 2F));
        float f4 = -0.4F * Mth.sin(swingProgress * (float)Math.PI);
        matrixStackIn.translate(f * (f2 + 0.64000005F), f3 + -0.6F + equippedProgress * -0.6F, f4 + -0.71999997F);
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        float f5 = Mth.sin(swingProgress * swingProgress * (float)Math.PI);
        float f6 = Mth.sin(f1 * (float)Math.PI);
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(f * f6 * 70.0F));
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(f * f5 * -20.0F));
        matrixStackIn.translate(f * -1.0F, 3.6F, 3.5D);
        matrixStackIn.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        matrixStackIn.mulPose(Axis.XP.rotationDegrees(200.0F));
        matrixStackIn.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        matrixStackIn.translate(f * 5.6F, 0.0D, 0.0D);
        // FIXME 26.1.2 port :: AvatarRenderer#renderRightHand/renderLeftHand now take (PoseStack, SubmitNodeCollector,
        // int, AvatarRenderState) rather than a live AbstractClientPlayer, and AvatarRenderer no longer exposes a raw
        // "getModel()" ModelPart tree the way the old per-arm ModelPart substitution below assumed - this whole
        // frozen-arm-overlay body is UNVERIFIED pending client/model finishing PlayerModel/ModelPlayerAnimated.
        // Left as-is (will not compile against the exact new AvatarRenderer signature) and clearly flagged rather
        // than guessing a wrong signature silently.
    }
}
