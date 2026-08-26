package com.bobmowzie.mowziesmobs.mixin.client;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import org.spongepowered.asm.mixin.Mixin;

/* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up.
    This accessor mixin used to expose HumanoidArmorLayer's private `renderTrim(Holder<ArmorMaterial>,
    PoseStack, MultiBufferSource, int, ArmorTrim, Model, boolean)` and `renderGlint(PoseStack,
    MultiBufferSource, int, Model)` methods via @Invoker, so that
    client/render/entity/layer/GeckoArmorLayer.java (custom GeckoLib-backed armor layer, out of scope for
    this porting pass) could reuse vanilla's trim-overlay and enchantment-glint rendering logic when drawing
    Mowzie's Mobs armor.

    In the current vanilla client (net/minecraft/client/renderer/entity/layers/HumanoidArmorLayer.java in
    the real decompiled 26.1.2 source), BOTH `renderTrim` and `renderGlint` are gone entirely, along with
    the old `getArmorModel`/`renderArmorPiece` step-by-step rendering flow this mixin's targets belonged to.
    Armor (including trims and glint) is now rendered by a single call to
    `EquipmentLayerRenderer#renderLayers(EquipmentClientInfo.LayerType, Identifier assetId, Model, S state,
    ItemStack, PoseStack, SubmitNodeCollector, int light, int outlineColor)` (see HumanoidArmorLayer#
    renderArmorPiece(...)) - trims/glint are baked into that single call via the equipment asset's client
    info rather than being separate invokable steps. There is no mechanical 1:1 replacement to accessor-mixin
    here anymore; also note vanilla's rendering itself moved from immediate PoseStack/MultiBufferSource
    drawing to submitting draw requests via SubmitNodeCollector (part of the broader render-state-extraction
    overhaul - see GuiGraphicsExtractor notes).

    ACTION NEEDED (cross-scope): client/render/entity/layer/GeckoArmorLayer.java, which is the sole
    consumer of this accessor mixin (calls `((HumanoidArmorLayerAccess) this).mowziesmobs$renderTrim(...)`
    and `mowziesmobs$renderGlint(...)`), will need its whole approach to custom armor rendering redesigned
    around EquipmentLayerRenderer/EquipmentClientInfo instead of reusing HumanoidArmorLayer's old private
    helper methods. That file is outside this agent's assigned scope (client/render) - flagging for the
    agent/session owning it.

    This mixin class is left registered (empty, no @Invoker members, so nothing to fail to match) so
    mowziesmobs.mixins.json's "client" list doesn't need touching; it currently exposes nothing.
*/
@Mixin(HumanoidArmorLayer.class)
public interface HumanoidArmorLayerAccess {
}
