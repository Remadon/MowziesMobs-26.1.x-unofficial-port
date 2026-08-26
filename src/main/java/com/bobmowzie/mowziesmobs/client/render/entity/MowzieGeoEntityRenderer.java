package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.tools.dynamics.GeckoDynamicChain;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.MowzieGeckoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 architecture change - see PORTING_NOTES.md "render/gui agent" section):
 * GeckoLib 5 mirrors vanilla's own "render state extraction" overhaul: {@code GeoEntityRenderer} now takes a second
 * type parameter for the vanilla {@link LivingEntityRenderState}, the old single {@code render(...)} override point
 * is gone (split into {@code extractRenderState}/{@code addRenderData} for live-entity access and
 * {@code submit}/render-layer methods that only ever see the (already captured) RenderState), and the entire
 * renderer-level per-bone/per-cube override system ({@code actuallyRender}, {@code renderRecursively},
 * {@code renderChildBones}, {@code renderCube(s)}, {@code checkAndRefreshBuffer}, {@code applyRenderLayersForBone})
 * has been removed entirely - per-bone positioning/rendering is now internal to {@code GeoBone}/{@code BakedGeoModel}.
 * <p>
 * This class has been restructured accordingly:
 * - {@link #addRenderData} captures the live entity + its dynamic chain array into the RenderState via DataTicket,
 *   since nothing after extraction has live-entity access any more.
 * - {@link #registerBonePositionListeners} is the new, GeckoLib-5-correct replacement for the old pattern of reading
 *   a bone's world position after {@code render()} returned. Bone pose data (`GeoBone#frameSnapshot`) is now
 *   strictly ephemeral - only valid for the duration of the actual render traversal - so
 *   {@link RenderPassInfo#addBonePositionListener} (fired live, during that traversal) is the only reliable way to
 *   observe it. Subclasses that used to override {@code renderUpdates} to copy a bone's world position onto the
 *   entity should override this instead.
 * - The old "dynamic chain" (physics-simulated bone chain) rendering, previously driven from
 *   {@code actuallyRender}/{@code renderRecursively}, is reimplemented as an internal {@link GeoRenderLayer}
 *   ({@link DynamicChainLayer}) that renders the chain's floating "dynamic" bone clones directly, since they were
 *   never part of the baked model's own bone tree. NOTE: this depends on
 *   {@code client/model/tools/dynamics/GeckoDynamicChain.java} (out of this agent's scope) being updated to match
 *   the new {@code MowzieGeoBone} API (it still calls now-removed methods like {@code getWorldPosition()}/
 *   {@code setWorldSpaceMatrix()} as of this writing) - flagged in the porting report.
 */
public abstract class MowzieGeoEntityRenderer<T extends MowzieGeckoEntity, R extends LivingEntityRenderState & GeoRenderState> extends GeoEntityRenderer<T, R> {
    // Package-visibility-widened (was private) so that subclasses overriding adjustModelBonesForRender - the
    // GeckoLib-5-correct replacement for the removed "setCustomAnimations" per-frame hook, see GeckoRenderPlayer.java
    // for the reference pattern - can read the live entity back out of the render state themselves. See
    // RenderUmvuthana.java for the first real usage of this.
    protected static final DataTicket<MowzieGeckoEntity> LIVE_ANIMATABLE = DataTicket.create("mowziesmobs_live_animatable", MowzieGeckoEntity.class);
    private static final DataTicket<GeckoDynamicChain[]> DYNAMIC_CHAINS = DataTicket.create("mowziesmobs_dynamic_chains", GeckoDynamicChain[].class);

    /** Mirrors the old GeckoLib-4-era "model render matrix", captured once the main model has been posed for this render pass. */
    private Matrix4f modelRenderMatrix = new Matrix4f();

    protected MowzieGeoEntityRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> modelProvider) {
        super(renderManager, modelProvider);
        withRenderLayer(new DynamicChainLayer<>(this));
    }

    @Override
    public @Nullable RenderType getRenderType(R renderState, Identifier texture) {
        // old GeckoLib 4 default was RenderType.entityCutoutNoCull(texture); RenderTypes.entityCutout(...) is the
        // new (no-cull) equivalent - see PORTING_NOTES.md RenderType/RenderTypes split section.
        return RenderTypes.entityCutout(texture);
    }

    @SuppressWarnings("unchecked")
    public MowzieGeoModel<T> getMowzieGeoModel() {
        return (MowzieGeoModel<T>) getGeoModel();
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState renderState) {
        return 0;
    }

    /// Captures the live entity (and its dynamic bone chains) into the RenderState. Nothing past this point in the
    /// render pass has access to the live entity any more - see class javadoc.
    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(LIVE_ANIMATABLE, animatable);
        renderState.addGeckolibData(DYNAMIC_CHAINS, animatable.dynamicChains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void preRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        MowzieGeckoEntity entity = renderPassInfo.getGeckolibData(LIVE_ANIMATABLE);

        if (entity != null) {
            registerBonePositionListeners(renderPassInfo, (T) entity);
        }
    }

    /// Override to call {@link RenderPassInfo#addBonePositionListener(String, RenderPassInfo.BonePositionListener)}
    /// for any bones whose rendered world position needs to be copied back onto the entity each frame (e.g. for use
    /// as a projectile spawn point). This is the replacement for the old pattern of reading a `GeoBone`'s world
    /// position after `render()` returned - see class javadoc.
    protected void registerBonePositionListeners(RenderPassInfo<R> renderPassInfo, T entity) {
    }

    /// @return the model-space-to-world PoseStack matrix as of the end of this renderer's most recent render pass.
    /// Used by {@link GeckoDynamicChain} to orient chain segments relative to the entity's current rotation.
    public Matrix4f getModelRenderMatrix() {
        return this.modelRenderMatrix;
    }

    /// Internal render layer that drives GeckoLib-4-style "dynamic" (physics-simulated) bone chains, reimplemented
    /// on top of GeckoLib 5's API - see class javadoc.
    private static class DynamicChainLayer<T extends MowzieGeckoEntity, R extends LivingEntityRenderState & GeoRenderState> extends GeoRenderLayer<T, Void, R> {
        private final MowzieGeoEntityRenderer<T, R> mowzieRenderer;

        DynamicChainLayer(MowzieGeoEntityRenderer<T, R> renderer) {
            super(renderer);

            this.mowzieRenderer = renderer;
        }

        private static GeckoDynamicChain[] chains(RenderPassInfo<?> renderPassInfo) {
            GeckoDynamicChain[] chains = renderPassInfo.getGeckolibData(DYNAMIC_CHAINS);

            return chains != null ? chains : new GeckoDynamicChain[0];
        }

        @Override
        public void preRender(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
            for (GeckoDynamicChain chain : chains(renderPassInfo)) {
                if (chain.chainOrig != null) {
                    for (MowzieGeoBone bone : chain.chainOrig) {
                        bone.setHidden(true);
                    }
                }
            }
        }

        @Override
        public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
            this.mowzieRenderer.modelRenderMatrix = new Matrix4f(renderPassInfo.getModelRenderMatrixState());

            for (GeckoDynamicChain chain : chains(renderPassInfo)) {
                chain.setChain();
                //noinspection rawtypes
                chain.updateChain(renderPassInfo.renderState().getPartialTick(), this.mowzieRenderer);

                if (chain.chainDynamic != null) {
                    Identifier texture = this.mowzieRenderer.getTextureLocation(renderPassInfo.renderState());
                    RenderType renderType = this.mowzieRenderer.getRenderType(renderPassInfo.renderState(), texture);

                    if (renderType != null) {
                        final int packedLight = renderPassInfo.packedLight();
                        final int packedOverlay = renderPassInfo.packedOverlay();
                        final int renderColor = renderPassInfo.renderColor();

                        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderType, (pose, vertexConsumer) -> {
                            PoseStack poseStack = renderPassInfo.poseStack();

                            poseStack.pushPose();
                            poseStack.last().set(pose);

                            for (MowzieGeoBone bone : chain.chainDynamic) {
                                if (bone != null) {
                                    // PORTING NOTE (RESOLVED, see PORTING_NOTES.md "GeoBone wrapper vs render() -
                                    // RESOLVED"): MowzieGeoBone WRAPS a real GeoBone rather than extending it, so
                                    // rendering goes through this thin delegating passthrough (which itself just
                                    // calls the wrapped bone.positionAndRender(...)) instead of MowzieGeoBone trying
                                    // to implement GeoBone's abstract render(...) itself.
                                    bone.positionAndRender(renderPassInfo, vertexConsumer, packedLight, packedOverlay, renderColor);
                                }
                            }

                            poseStack.popPose();
                        });
                    }
                }

                if (chain.chainOrig != null) {
                    for (MowzieGeoBone bone : chain.chainOrig) {
                        bone.setHidden(false);
                    }
                }
            }
        }
    }
}
