package com.bobmowzie.mowziesmobs.client;

import com.bobmowzie.mowziesmobs.MMCommon;
import net.minecraft.resources.Identifier;

/* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up - this whole class's technique is obsolete.
    This class used to runtime-wrap vanilla's flat item BakedModel (net.minecraft.client.resources.model.BakedModel,
    keyed by net.minecraft.client.resources.model.ModelResourceLocation, with an ItemOverrides property) so that
    HAND_MODEL_ITEMS (wrought_axe, spear, earthrend_gauntlet, sculptor_staff) and the umvuthana-mask/sol-visage
    "frame" overlay models could swap to a different underlying baked model depending on ItemDisplayContext
    (first/third-person hand vs. GUI/fixed), subscribed via NeoForge's ModelEvent.ModifyBakingResult
    (event.getModels() : Map<ModelResourceLocation, BakedModel>) and ModelEvent.RegisterAdditional
    (modelRegistryEvent.register(Identifier)).

    NONE of BakedModel, ModelResourceLocation, ItemOverrides, or ModelEvent.RegisterAdditional exist anymore in the
    real 26.1.2 vanilla/NeoForge source. Confirmed by reading:
      - net/minecraft/client/renderer/item/ItemModel.java (real decompiled 26.1.2 source): items now resolve to a
        single `ItemModel` per registry Identifier (no more per-context ModelResourceLocation baked variants) with
        `void update(ItemStackRenderState, ItemStack, ItemModelResolver, ItemDisplayContext, ClientLevel, ItemOwner,
        int)` - a render-state-extraction API, not a flat quad list.
      - net/minecraft/client/resources/model/ModelBakery.java: ModelEvent.ModifyBakingResult's
        ModelBakery.BakingResult now exposes separate `blockStateModels()` (Map<BlockState,BlockStateModel>),
        `itemStackModels()` (Map<Identifier,ItemModel>), `itemProperties()`, and `standaloneModels()` - there is no
        unified Map<ModelResourceLocation,BakedModel> anymore.
      - net/neoforged/neoforge/client/event/ModelEvent.java (NeoForge 26.1.2.95 sources): RegisterAdditional is gone;
        the closest analog is ModelEvent.RegisterStandalone, which registers a StandaloneModelKey<T> +
        UnbakedStandaloneModel<T> (baked to a raw BlockStateModel/QuadCollection/etc via
        net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel, NOT to an ItemModel), retrieved
        later via ModelManager#getStandaloneModel(key) - a fundamentally different retrieval path than looking a
        model up out of the baking-result map inside ModifyBakingResult.

    Vanilla itself achieves this same "different model per ItemDisplayContext" behavior entirely differently now -
    via data-driven item model composition, e.g. ItemModelUtils.conditional(...)/rangeSelect(...) building a
    ConditionalItemModel/SelectItemModel from assets/<ns>/items/<item>.json (see ItemModelGenerators#generateBow for
    the vanilla analog: a "minecraft:condition" on "minecraft:using_item" switching between plain and pulling-stage
    models). The correct port of this class's feature is almost certainly to move it out of Java entirely and into
    each affected item's assets/mowziesmobs/items/*.json using a "minecraft:condition" keyed on display context
    (there's a NeoForge/vanilla condition property for this - needs locating in the real source) selecting between
    the base model and the "_in_hand" / "_frame" model, mirroring how MMClient.java's now-removed bow-pulling
    ItemProperties registration needs the same kind of data-driven replacement (see the FIXME there).

    Implementing a Java-side custom ItemModel/ItemModel.Unbaked to reproduce this exactly was deliberately NOT
    attempted here - it requires understanding ItemStackRenderState/ItemModelResolver internals well enough to
    build a correct render state (verified they exist and roughly what they do, but not enough to implement
    correctly without guessing, which the porting rules for this pass explicitly disallow). Flagging for a
    follow-up pass with room to prototype/test against a running client.

    `prefixed()` is kept (still just an Identifier-returning helper, no broken types) since MaskType-related code
    elsewhere may still reference model resource ids by name; `HAND_MODEL_ITEMS` is kept as the source-of-truth list
    of which items need this treatment once it's re-implemented. The registration call sites in
    ClientEventBusSubscriber.onRegisterModels (ModelEvent.RegisterAdditional) and this class's former
    onModelBakeEvent (ModelEvent.ModifyBakingResult) subscriber have been removed since they no longer compile
    against any existing type - see ClientEventBusSubscriber.java for the matching removal.
*/
public class MMModels {
    public static final String[] HAND_MODEL_ITEMS = new String[] {"wrought_axe", "spear", "earthrend_gauntlet", "sculptor_staff"};
    // Can only register 'standalone' models which do not have this prefix
    public static final String PREFIX = "item/";

    public static Identifier prefixed(String path) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, PREFIX + path);
    }
}
