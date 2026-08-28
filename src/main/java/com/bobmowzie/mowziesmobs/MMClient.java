package com.bobmowzie.mowziesmobs;

import com.bobmowzie.mowziesmobs.client.ClientLayerRegistry;
import com.bobmowzie.mowziesmobs.client.render.entity.FrozenRenderHandler;
import com.bobmowzie.mowziesmobs.server.ability.AbilityClientEventHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MMCommon.MODID, dist = Dist.CLIENT)
public class MMClient {
    public MMClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(ClientLayerRegistry::onAddLayers);
        modBus.addListener(this::init);
        modBus.addListener(this::registerClientExtensions);

        NeoForge.EVENT_BUS.addListener(FrozenRenderHandler::onRenderHand);
        NeoForge.EVENT_BUS.addListener(AbilityClientEventHandler::onRenderTick);

        container.registerConfig(ModConfig.Type.CLIENT, ConfigHandler.CLIENT_CONFIG);
    }

    private void init(FMLLoadCompleteEvent event) {
        /* FIXME 1.21 -> 26.1.2 port: UNRESOLVED, flagged for follow-up.
            net.minecraft.client.renderer.item.ItemProperties / ItemPropertyFunction (the Java API used to reuse
            vanilla's bow "pulling" animation-frame predicate for the Blowgun item) no longer exist at all. Item
            model "predicate overrides" (models/item/*.json "overrides": [{"predicate": {"pulling": ...}}]) were
            replaced project-wide in vanilla by a data-driven item model system
            (net.minecraft.client.renderer.item.ItemModel / assets/<ns>/items/<name>.json, e.g.
            ItemModelUtils.conditional(ItemModelUtils.isUsingItem(), ItemModelUtils.rangeSelect(new UseDuration(false), ...), ...)
            - see ItemModelGenerators#generateBow(Item) in the real decompiled 26.1.2 source for the vanilla bow's
            equivalent). There is no Java call to port this to; the fix belongs in
            src/main/resources/assets/mowziesmobs/models/item/blowgun.json (currently still using the old
            "overrides"/"predicate" format) plus a new assets/mowziesmobs/items/blowgun.json using
            "minecraft:range_dispatch" on "minecraft:use_duration" wrapped in "minecraft:condition" on
            "minecraft:using_item", mirroring vanilla's bow. That's a resource/data-file migration (likely needed
            for other item models in the mod too, not just this one) outside this agent's Java-focused scope -
            flagging for a follow-up pass rather than guessing at a partial fix here.
        */
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(ItemWroughtHelm.ArmorRender.INSTANCE, ItemHandler.WROUGHT_HELMET);
        event.registerItem(new ItemSolVisage.ClientExtensions(), ItemHandler.SOL_VISAGE);
        event.registerItem(new ItemEarthrendGauntlet.ClientExtensions(), ItemHandler.EARTHREND_GAUNTLET);
        event.registerItem(new ItemGeomancerArmor.ClientExtensions(), ItemHandler.GEOMANCER_BEADS, ItemHandler.GEOMANCER_BELT, ItemHandler.GEOMANCER_ROBE, ItemHandler.GEOMANCER_SANDALS);
        event.registerItem(new ItemUmvuthanaMask.ClientExtensions(), ItemHandler.UMVUTHANA_MASK_FURY, ItemHandler.UMVUTHANA_MASK_FEAR, ItemHandler.UMVUTHANA_MASK_RAGE, ItemHandler.UMVUTHANA_MASK_BLISS, ItemHandler.UMVUTHANA_MASK_MISERY, ItemHandler.UMVUTHANA_MASK_FAITH);
        event.registerItem(new ItemSculptorStaff.ClientExtensions(), ItemHandler.SCULPTOR_STAFF);
    }
}
