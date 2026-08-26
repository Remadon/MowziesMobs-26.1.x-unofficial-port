package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosa;
import com.geckolib.constant.dataticket.DataTicket;
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
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port, see PORTING_NOTES.md architecture section): the old single {@code render}
 * override (which called {@code getRenderer().reRender(...)} to redraw the whole baked model with an alternate
 * translucent texture) is gone - the new equivalent is {@code submitRenderTask}, which redraws the model by manually
 * re-invoking {@code RenderPassInfo#renderPosed(Runnable)} + {@code SubmitNodeCollector#submitCustomGeometry(...)},
 * mirroring the exact pattern used by {@code FrozenRenderHandler.GeckoLayerFrozen} (a working, already-ported
 * sibling layer in this codebase - see client/render/entity/FrozenRenderHandler.java). The texture depends on the
 * live entity's moon phase, which is captured into the render state via a DataTicket in {@code addRenderData} since
 * {@code submitRenderTask} no longer has live-entity access.
 */
public class ElokosaHandSymbolGeoLayer<R extends GeoRenderState> extends GeoRenderLayer<EntityElokosa, Void, R> {
    private static final DataTicket<Identifier> HAND_TEXTURE = DataTicket.create("mowziesmobs_elokosa_hand_texture", Identifier.class);

    public ElokosaHandSymbolGeoLayer(GeoRenderer<EntityElokosa, Void, R> entityRendererIn) {
        super(entityRendererIn);
    }

    protected Identifier getTextureResource(EntityElokosa animatable) {
        return switch (animatable.level().environmentAttributes().getValue(net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, animatable.blockPosition()).index()) {
            case 0 -> MMCommon.resource("textures/entity/elokosa_paw_full.png");
            case 1, 7 -> MMCommon.resource("textures/entity/elokosa_paw_gibbous.png");
            case 2, 6 -> MMCommon.resource("textures/entity/elokosa_paw_half.png");
            default -> MMCommon.resource("textures/entity/elokosa_paw_crescent.png");
        };
    }

    @Override
    public void addRenderData(EntityElokosa animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(HAND_TEXTURE, getTextureResource(animatable));
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (!renderPassInfo.willRender()) return;

        Identifier texture = renderPassInfo.getGeckolibData(HAND_TEXTURE);
        if (texture == null) return;

        RenderType renderTypeTranslucent = RenderTypes.entityTranslucent(texture);
        int packedLight = renderPassInfo.packedLight();
        int packedOverlay = renderPassInfo.packedOverlay();
        int color = ARGB.colorFromFloat(1, 1, 1, 1f);

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderTypeTranslucent, (pose, vertexConsumer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();

            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, packedLight, packedOverlay, color));
            poseStack.popPose();
        });
    }
}
