package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelEarthSpike;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoBlockLayer;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityEarthSpike;
import com.geckolib.constant.DataTickets;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * PORTING NOTE: two old override points no longer exist and were replaced as follows (see porting report for the
 * reasoning):
 * - old empty {@code renderCube(...)} (suppressed the model's own cube geometry so only {@link GeckoBlockLayer}'s
 *   block-bone substitution would render) -> no per-cube renderer hook exists any more; {@link #getRenderType}
 *   returning {@code null} suppresses the main model geometry submission while still letting registered
 *   {@code GeoRenderLayer}s (GeckoBlockLayer) run normally (see {@code GeoRenderer#performRenderPass}).
 * - old {@code applyRotations(T, PoseStack, ageInTicks, rotationYaw, partialTick, nativeScale)} (substituted a raw
 *   yRot lerp instead of GeckoLib's default body-yaw calculation, since this is a non-living effect entity) -> the
 *   new {@code applyRotations} only receives an already-captured {@code RenderPassInfo}, no raw yaw parameter to
 *   override, so the substitution is now done earlier by overwriting the {@code ENTITY_BODY_YAW} data ticket during
 *   {@link #captureDefaultRenderState}.
 */
public class RenderEarthSpike extends RenderGeomancyBase<EntityEarthSpike, EntityRenderState> {
    private static final Identifier TEXTURE_DIRT = Identifier.withDefaultNamespace("textures/block/dirt.png");

    public RenderEarthSpike(EntityRendererProvider.Context mgr) {
        super(mgr, new ModelEarthSpike());
        this.withRenderLayer(new GeckoBlockLayer<>(this,
                (bone, animatable) -> {
                    if (bone.name().contains("block")) {
                        return animatable.getBlock();
                    }
                    return null;
                }));
    }

    @Override
    public Identifier getTextureLocation(EntityRenderState renderState) {
        return TEXTURE_DIRT;
    }

    @Override
    public @Nullable RenderType getRenderType(EntityRenderState renderState, Identifier texture) {
        return null;
    }

    @Override
    public void captureDefaultRenderState(EntityEarthSpike animatable, @Nullable Void relatedObject, EntityRenderState renderState, float partialTick) {
        super.captureDefaultRenderState(animatable, relatedObject, renderState, partialTick);

        renderState.addGeckolibData(DataTickets.ENTITY_BODY_YAW, Mth.rotLerp(partialTick, animatable.yRotO, animatable.getYRot()));
    }
}
