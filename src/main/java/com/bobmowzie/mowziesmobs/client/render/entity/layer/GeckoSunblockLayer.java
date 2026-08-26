package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE (GeckoLib 4 -> 5 full port): re-render pattern ported the same way as
 * {@link ElokosaHandSymbolGeoLayer} / {@code FrozenRenderHandler.GeckoLayerFrozen} (see their javadocs). Whether the
 * entity currently "has sunblock" is capability data needing the live entity, so it is captured into the render
 * state via a DataTicket in {@code addRenderData}. The "time" value driving the swirl animation offset used to be
 * {@code animatable.tickCount + partialTick}; the closest GeckoLib-5-native equivalent available from a
 * {@code RenderPassInfo} alone is {@code GeoRenderState#getAnimatableAge()} (backed by
 * {@code DataTickets.TICK}/{@code ClientUtil.getCurrentTick(partialTick)}, GeckoLib's own designated replacement for
 * this exact "current animatable age in ticks" concept).
 */
public class GeckoSunblockLayer<T extends LivingEntity & GeoEntity, R extends GeoRenderState> extends GeoRenderLayer<T, Void, R> {
    private static final Identifier SUNBLOCK_ARMOR = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/entity/sunblock_glow.png");
    private static final DataTicket<Boolean> HAS_SUNBLOCK = DataTicket.create("mowziesmobs_has_sunblock", Boolean.class);

    public GeckoSunblockLayer(GeoRenderer<T, Void, R> entityRendererIn, EntityRendererProvider.Context context) {
        super(entityRendererIn);
    }

    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        renderState.addGeckolibData(HAS_SUNBLOCK, DataHandler.getData(animatable, DataHandler.LIVING_DATA).getHasSunblock());
    }

    @Override
    public void submitRenderTask(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        if (!renderPassInfo.willRender() || !Boolean.TRUE.equals(renderPassInfo.getGeckolibData(HAS_SUNBLOCK))) return;

        float f = (float) renderPassInfo.renderState().getAnimatableAge();
        RenderType renderTypeSwirl = RenderTypes.energySwirl(getTextureLocation(), xOffset(f), f * 0.01F);
        int packedLight = renderPassInfo.packedLight();
        int color = ARGB.colorFromFloat(0.1f, 1, 1, 1);

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), renderTypeSwirl, (pose, vertexConsumer) -> {
            PoseStack poseStack = renderPassInfo.poseStack();

            poseStack.pushPose();
            poseStack.last().set(pose);
            renderPassInfo.renderPosed(() -> renderPassInfo.model().render(renderPassInfo, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, color));
            poseStack.popPose();
        });
    }

    protected float xOffset(float p_225634_1_) {
        return p_225634_1_ * 0.02F;
    }

    protected Identifier getTextureLocation() {
        return SUNBLOCK_ARMOR;
    }
}
