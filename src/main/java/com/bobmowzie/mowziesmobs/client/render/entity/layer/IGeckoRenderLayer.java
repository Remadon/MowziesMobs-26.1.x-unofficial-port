package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.geckolib.cache.model.GeoBone;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): the renderer-level per-bone recursive-render override system this interface's
 * {@code renderRecursively} hook plugged into ({@code GeoRenderer#renderRecursively}/{@code renderChildBones}) was
 * removed entirely in GeckoLib 5 - per-bone cube rendering now lives inside {@code GeoBone}/{@code BakedGeoModel}
 * itself (see PORTING_NOTES.md architecture section). No caller in this codebase still invokes
 * {@code renderRecursively} on an {@code IGeckoRenderLayer} (grep-confirmed), so this is kept only as a vestigial,
 * no-op default method so any out-of-scope implementor (e.g. {@code GeckoParrotOnShoulderLayer},
 * {@code GeckoPlayerItemInHandLayer}) keeps compiling unchanged; the method itself is never called by the new
 * rendering pipeline any more.
 */
public interface IGeckoRenderLayer {
    default void renderRecursively(GeoBone bone, PoseStack matrixStack, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, int color) {

    }
}
