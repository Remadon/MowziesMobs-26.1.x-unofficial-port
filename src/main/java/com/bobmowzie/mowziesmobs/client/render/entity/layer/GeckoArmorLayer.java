package com.bobmowzie.mowziesmobs.client.render.entity.layer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * PORTING NOTE (26.1.2): the entire custom bypass this class used to implement (`render(...)` reimplementing
 * `HumanoidArmorLayer`'s per-ArmorMaterial-layer texture iteration + trim/glint via
 * {@code mixin/client/HumanoidArmorLayerAccess}, specifically to skip GeckoLib 4's
 * `InternalUtil#tryRenderGeoArmorPiece` mixin hook and to flip on
 * {@code MowzieGeoArmorRenderer#usingCustomPlayerAnimations}) is obsolete:
 * - Vanilla's `HumanoidArmorLayer` was completely rewritten around `EquipmentLayerRenderer`/`EquipmentClientInfo`
 *   (equipment-asset-driven rendering; texture/trim/glint are now a single call, not separate steps to intercept -
 *   see `mixin/client/HumanoidArmorLayerAccess.java` for the full writeup).
 * - GeckoLib 5's `GeoArmorRenderer` is no longer a `net.minecraft.client.model.Model` subtype at all (see
 *   `MowzieGeoArmorRenderer.java`) and is wired in via its own internal mixin
 *   (`GeoArmorRenderer#tryRenderGeoArmorPiece`, documented as "typically only called by an internal mixin") - so
 *   there is nothing left for a custom `HumanoidArmorLayer` subclass to bypass; GeckoLib's own mixin should now
 *   intercept armor rendering correctly on top of a plain vanilla `HumanoidArmorLayer`.
 * This class has therefore been reduced to a trivial passthrough with an updated constructor signature (matching
 * `HumanoidArmorLayer`'s new `ArmorModelSet`/`EquipmentLayerRenderer`-based constructor). **This relies on GeckoLib's
 * own armor-rendering mixin working without this bypass, which could not be verified without running the game** -
 * flagged in the porting report; if GeckoLib-backed armor (Geomancer/SolVisage/UmvuthanaMask) fails to render or
 * doesn't follow the custom-animated player model's pose, start here.
 */
public class GeckoArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>, A extends HumanoidModel<S>> extends HumanoidArmorLayer<S, M, A> {
    public GeckoArmorLayer(RenderLayerParent<S, M> layerParent, ArmorModelSet<A> modelSet, ArmorModelSet<A> babyModelSet, EquipmentLayerRenderer equipmentLayerRenderer) {
        super(layerParent, modelSet, babyModelSet, equipmentLayerRenderer);
    }

    public GeckoArmorLayer(RenderLayerParent<S, M> layerParent, ArmorModelSet<A> modelSet, EquipmentLayerRenderer equipmentLayerRenderer) {
        super(layerParent, modelSet, equipmentLayerRenderer);
    }
}
