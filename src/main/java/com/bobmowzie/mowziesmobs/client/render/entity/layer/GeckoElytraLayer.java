package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * PORTING NOTE (26.1.2): same reasoning as {@link GeckoCapeLayer} in this package (read that file's javadoc for the
 * full writeup - "matrix mode" is gone from {@code ModelPlayerAnimated}, and the vanilla layer this wraps was
 * rewritten to be render-state-keyed with no live entity access) - reduced to a trivial passthrough.
 * <p>
 * Vanilla's elytra layer was also renamed: {@code net.minecraft.client.renderer.entity.layers.ElytraLayer} ->
 * {@code net.minecraft.client.renderer.entity.layers.WingsLayer} (grep-confirmed - {@code ElytraLayer.java} no
 * longer exists in the vanilla source tree at all, {@code WingsLayer.java} is its direct replacement with the same
 * constructor shape plus an extra {@code EquipmentLayerRenderer} parameter, matching the equipment-asset-driven
 * rendering flow documented in PORTING_NOTES.md's "HumanoidArmorLayer full rewrite" section).
 * <p>
 * <b>Currently unused / orphaned</b>: see {@link GeckoCapeLayer}'s javadoc - identical situation, {@code
 * GeckoRenderPlayer} no longer has an {@code addLayer}/{@code layers} mechanism to host this on, and nothing
 * constructs a {@code GeckoElytraLayer} any more (grep-confirmed). Left in a real, compiling, constructible state
 * against the actual vanilla {@code AvatarRenderer<AbstractClientPlayer>} rather than deleted.
 */
public class GeckoElytraLayer extends WingsLayer<AvatarRenderState, PlayerModel> {
    public GeckoElytraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> rendererIn, EntityModelSet modelSet, EquipmentLayerRenderer equipmentRenderer) {
        super(rendererIn, modelSet, equipmentRenderer);
    }
}
