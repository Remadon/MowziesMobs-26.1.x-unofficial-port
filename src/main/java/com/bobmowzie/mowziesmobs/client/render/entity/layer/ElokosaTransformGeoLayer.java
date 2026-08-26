package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosa;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): re-render pattern ported the same way as
 * {@link ElokosaHandSymbolGeoLayer} (see its javadoc / FrozenRenderHandler.GeckoLayerFrozen) - {@code render(...)}
 * split into {@code preRender} (captures the opacity value) + {@code submitRenderTask} (re-draws the model
 * translucent with that opacity as the alpha channel).
 * <p>
 * The opacity is derived from {@code MowzieGeoModel#getControllerValue(String)}, which reads a bone's live
 * {@code frameSnapshot} (see MowzieGeoBone.java "CRITICAL GeckoLib-5 semantics" note in PORTING_NOTES.md) - it is
 * read here from {@code preRender}, which runs at the same early point in the render pass as
 * {@code MowzieGeoEntityRenderer#registerBonePositionListeners} (before the frame's bone snapshots have been
 * (re)computed), matching the exact same read-timing already established elsewhere in this port for identical
 * "read a controller-driving bone's value outside an active render traversal" cases (e.g.
 * {@code RenderElokosa#getShadowRadius}, {@code RenderSculptor#registerBonePositionListeners}'s
 * {@code disappearController} read). Per those existing call sites' own caveats, this may read a stale/default
 * (previous-frame-cleaned-up) snapshot rather than the current frame's true value - a known, already-accepted
 * cross-cutting GeckoLib-5 timing tradeoff, not something newly introduced here.
 */
public class ElokosaTransformGeoLayer<R extends GeoRenderState> extends GeoRenderLayer<EntityElokosa, Void, R> {
    private float opacity = 0;

    public ElokosaTransformGeoLayer(GeoRenderer<EntityElokosa, Void, R> entityRendererIn) {
        super(entityRendererIn);
    }

    protected Identifier getTextureResource() {
        return MMCommon.resource("textures/entity/elokosa_transforming.png");
    }

    @Override
    public void preRender(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        @SuppressWarnings("unchecked")
        MowzieGeoModel<EntityElokosa> model = (MowzieGeoModel<EntityElokosa>) getGeoModel();
        opacity = model.isInitialized() ? -model.getControllerValue("transformTextureController") : 0;
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (!renderPassInfo.willRender()) return;

        RenderType renderTypeTranslucent = RenderTypes.entityTranslucent(getTextureResource());
        int packedLight = renderPassInfo.packedLight();
        int packedOverlay = renderPassInfo.packedOverlay();
        int color = ARGB.colorFromFloat(opacity, 1, 1, 1f);

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderTypeTranslucent, (pose, vertexConsumer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();

            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, packedLight, packedOverlay, color));
            poseStack.popPose();
        });
    }
}
