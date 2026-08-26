package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.elokosa.PawType;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.MaskType;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import com.google.common.collect.ImmutableList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemHandler {
    // Default attribute values
    private static final int NEGATE_ATTACK_DAMAGE = -2; // 2 as base set for players
    private static final int NEGATE_ATTACK_SPEED = -4; // 4 as base from the attribute

    public static Style TOOLTIP_STYLE = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GRAY));

    // PORTING NOTE (1.21.1 -> 26.1.2): plain DeferredRegister.create(Registries.ITEM, ...) no longer auto-assigns
    // an id to constructed Items - Item.Properties#build() now requires setId(ResourceKey<Item>) to have been
    // called, throwing "Item id not set" at RegisterEvent time otherwise. DeferredRegister.createItems(...) is the
    // specialized subclass whose register(name, supplier) overrides thread the id through automatically - confirmed
    // this is a drop-in replacement (same register(String, Supplier<? extends I>) call sites below all still work).
    public static final DeferredRegister<Item> REG = DeferredRegister.createItems(MMCommon.MODID);

    public static final DeferredHolder<Item, ItemFoliaathSeed> FOLIAATH_SEED = REG.register("foliaath_seed", () -> new ItemFoliaathSeed(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("foliaath_seed")))));
    public static final DeferredHolder<Item, ItemMobRemover> MOB_REMOVER = REG.register("mob_remover", () -> new ItemMobRemover(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("mob_remover")))));
    public static final DeferredHolder<Item, ItemWroughtAxe> WROUGHT_AXE = REG.register("wrought_axe", () -> new ItemWroughtAxe(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("wrought_axe"))).rarity(Rarity.UNCOMMON)));
    // NOTE: durability is now baked into ItemXxx's constructor via Properties#humanoidArmor(material, slot) (ArmorItem
    // was removed upstream), which pulls the same per-piece multiplier that used to be passed here explicitly.
    public static final DeferredHolder<Item, ItemWroughtHelm> WROUGHT_HELMET = REG.register("wrought_helmet", () -> new ItemWroughtHelm(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("wrought_helmet"))).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_FURY = REG.register("umvuthana_mask_fury", () -> new ItemUmvuthanaMask(MaskType.FURY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_fury")))));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_FEAR = REG.register("umvuthana_mask_fear", () -> new ItemUmvuthanaMask(MaskType.FEAR, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_fear")))));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_RAGE = REG.register("umvuthana_mask_rage", () -> new ItemUmvuthanaMask(MaskType.RAGE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_rage")))));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_BLISS = REG.register("umvuthana_mask_bliss", () -> new ItemUmvuthanaMask(MaskType.BLISS, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_bliss")))));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_MISERY = REG.register("umvuthana_mask_misery", () -> new ItemUmvuthanaMask(MaskType.MISERY, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_misery")))));
    public static final DeferredHolder<Item, ItemUmvuthanaMask> UMVUTHANA_MASK_FAITH = REG.register("umvuthana_mask_faith", () -> new ItemUmvuthanaMask(MaskType.FAITH, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_mask_faith")))));
    public static final DeferredHolder<Item, ItemSolVisage> SOL_VISAGE = REG.register("sol_visage", () -> new ItemSolVisage(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("sol_visage"))).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, ItemDart> DART = REG.register("dart", () -> new ItemDart(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("dart")))));
    public static final DeferredHolder<Item, ItemSpear> SPEAR = REG.register("spear", () -> new ItemSpear(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("spear"))).stacksTo(1)));
    public static final DeferredHolder<Item, ItemBlowgun> BLOWGUN = REG.register("blowgun", () -> new ItemBlowgun(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("blowgun"))).stacksTo(1).durability(300)));
    public static final DeferredHolder<Item, ItemGrantSunsBlessing> GRANT_SUNS_BLESSING = REG.register("grant_suns_blessing", () -> new ItemGrantSunsBlessing(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("grant_suns_blessing"))).stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, ItemIceCrystal> ICE_CRYSTAL = REG.register("ice_crystal", () -> new ItemIceCrystal(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("ice_crystal"))).durability(ConfigHandler.COMMON.TOOLS_AND_ABILITIES.ICE_CRYSTAL.durabilityValue).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, ItemCapturedGrottol> CAPTURED_GROTTOL = REG.register("captured_grottol", () -> new ItemCapturedGrottol(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("captured_grottol"))).stacksTo(1)));
    public static final DeferredHolder<Item, ItemGlowingJelly> GLOWING_JELLY = REG.register("glowing_jelly", () -> new ItemGlowingJelly(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("glowing_jelly"))).food(ItemGlowingJelly.GLOWING_JELLY_FOOD, ItemGlowingJelly.GLOWING_JELLY_CONSUMABLE)));
    public static final DeferredHolder<Item, ItemNagaFang> NAGA_FANG = REG.register("naga_fang", () -> new ItemNagaFang(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("naga_fang")))));
    public static final DeferredHolder<Item, ItemNagaFangDagger> NAGA_FANG_DAGGER = REG.register("naga_fang_dagger", () -> new ItemNagaFangDagger(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("naga_fang_dagger")))));
    public static final DeferredHolder<Item, ItemEarthrendGauntlet> EARTHREND_GAUNTLET = REG.register("earthrend_gauntlet", () -> new ItemEarthrendGauntlet(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("earthrend_gauntlet"))).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, ItemSculptorStaff> SCULPTOR_STAFF = REG.register("sculptor_staff", () -> new ItemSculptorStaff(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("sculptor_staff"))).rarity(Rarity.RARE)));
    public static final DeferredHolder<Item, ItemSandRake> SAND_RAKE = REG.register("sand_rake", () -> new ItemSandRake(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("sand_rake"))).durability(64)));
    public static final DeferredHolder<Item, ItemGeomancerArmor> GEOMANCER_BEADS = REG.register("geomancer_beads", () -> new ItemGeomancerArmor(ArmorType.HELMET, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("geomancer_beads"))).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, ItemGeomancerArmor> GEOMANCER_ROBE = REG.register("geomancer_robe", () -> new ItemGeomancerArmor(ArmorType.CHESTPLATE, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("geomancer_robe"))).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, ItemGeomancerArmor> GEOMANCER_BELT = REG.register("geomancer_belt", () -> new ItemGeomancerArmor(ArmorType.LEGGINGS, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("geomancer_belt"))).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, ItemGeomancerArmor> GEOMANCER_SANDALS = REG.register("geomancer_sandals", () -> new ItemGeomancerArmor(ArmorType.BOOTS, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("geomancer_sandals"))).rarity(Rarity.UNCOMMON)));
    public static final DeferredHolder<Item, ItemBluffRod> BLUFF_ROD = REG.register("bluff_rod", () -> new ItemBluffRod(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("bluff_rod")))));
    public static final DeferredHolder<Item, ItemElokosaPaw> ELOKOSA_PAW_FULL = REG.register("elokosa_paw_full", () -> new ItemElokosaPaw(PawType.FULL, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_paw_full")))));
    public static final DeferredHolder<Item, ItemElokosaPaw> ELOKOSA_PAW_GIBBOUS = REG.register("elokosa_paw_gibbous", () -> new ItemElokosaPaw(PawType.GIBBOUS, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_paw_gibbous")))));
    public static final DeferredHolder<Item, ItemElokosaPaw> ELOKOSA_PAW_HALF = REG.register("elokosa_paw_half", () -> new ItemElokosaPaw(PawType.HALF, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_paw_half")))));
    public static final DeferredHolder<Item, ItemElokosaPaw> ELOKOSA_PAW_CRESCENT = REG.register("elokosa_paw_crescent", () -> new ItemElokosaPaw(PawType.CRESCENT, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_paw_crescent")))));
    public static final DeferredHolder<Item, ItemElokosaPaw> ELOKOSA_PAW_NEW = REG.register("elokosa_paw_new", () -> new ItemElokosaPaw(PawType.NEW, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_paw_new")))));
    public static final ImmutableList<DeferredHolder<Item, ItemElokosaPaw>> ELOKOSA_PAWS = ImmutableList.of(
            ELOKOSA_PAW_FULL,
            ELOKOSA_PAW_GIBBOUS,
            ELOKOSA_PAW_HALF,
            ELOKOSA_PAW_CRESCENT,
            ELOKOSA_PAW_NEW
    );

    public static final DeferredHolder<Item, Item> LOGO = REG.register("logo", () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("logo")))));
    public static final DeferredHolder<Item, Item> PETIOLE_MUSIC_DISC = REG.register("music_disc_petiole", () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("music_disc_petiole"))).stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(MMSounds.JUKEBOX_PETIOLE)));

    // PORTING NOTE (1.21.1 -> 26.1.2): NeoForge's DeferredSpawnEggItem (constructor taking an EntityType holder plus
    // two int colors) no longer exists at all (confirmed: grepped every jar/class in the installed NeoForge
    // 26.1.2.95 distribution for "SpawnEgg" - only vanilla SpawnEggItem remains, whose constructor is now just
    // `SpawnEggItem(Item.Properties)`; the entity type is attached via `Item.Properties#spawnEgg(EntityType)`
    // instead of a constructor arg (see vanilla Items#registerItem(..., SpawnEggItem::new, new Item.Properties().
    // spawnEgg(type)) for the confirmed real usage pattern). The two primary/secondary egg colors have NO surviving
    // Java-side home any more - grepped the whole vanilla source tree for any "primaryColor"/"secondaryColor"/
    // "SpawnEggColors" concept and found nothing; spawn egg tinting is presumably now fully resource-pack/model
    // driven. The color ints below are therefore dropped rather than silently misapplied - flagged here in case the
    // egg icons render un-tinted and need a client asset (item model tint layer) authored to restore them.
    public static final DeferredHolder<Item, SpawnEggItem> FOLIAATH_SPAWN_EGG = REG.register("foliaath_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("foliaath_spawn_egg"))).spawnEgg(EntityHandler.FOLIAATH.get())));
    public static final DeferredHolder<Item, SpawnEggItem> WROUGHTNAUT_SPAWN_EGG = REG.register("wroughtnaut_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("wroughtnaut_spawn_egg"))).spawnEgg(EntityHandler.WROUGHTNAUT.get())));
    public static final DeferredHolder<Item, SpawnEggItem> UMVUTHANA_SPAWN_EGG = REG.register("umvuthana_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_spawn_egg"))).spawnEgg(EntityHandler.UMVUTHANA_MINION.get())));
    public static final DeferredHolder<Item, SpawnEggItem> UMVUTHANA_RAPTOR_SPAWN_EGG = REG.register("umvuthana_raptor_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_raptor_spawn_egg"))).spawnEgg(EntityHandler.UMVUTHANA_RAPTOR.get())));
    public static final DeferredHolder<Item, SpawnEggItem> UMVUTHANA_CRANE_SPAWN_EGG = REG.register("umvuthana_crane_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthana_crane_spawn_egg"))).spawnEgg(EntityHandler.UMVUTHANA_CRANE.get())));
    public static final DeferredHolder<Item, SpawnEggItem> UMVUTHI_SPAWN_EGG = REG.register("umvuthi_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("umvuthi_spawn_egg"))).spawnEgg(EntityHandler.UMVUTHI.get())));
    public static final DeferredHolder<Item, SpawnEggItem> FROSTMAW_SPAWN_EGG = REG.register("frostmaw_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("frostmaw_spawn_egg"))).spawnEgg(EntityHandler.FROSTMAW.get())));
    public static final DeferredHolder<Item, SpawnEggItem> GROTTOL_SPAWN_EGG = REG.register("grottol_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("grottol_spawn_egg"))).spawnEgg(EntityHandler.GROTTOL.get())));
    public static final DeferredHolder<Item, SpawnEggItem> LANTERN_SPAWN_EGG = REG.register("lantern_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("lantern_spawn_egg"))).spawnEgg(EntityHandler.LANTERN.get())));
    public static final DeferredHolder<Item, SpawnEggItem> NAGA_SPAWN_EGG = REG.register("naga_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("naga_spawn_egg"))).spawnEgg(EntityHandler.NAGA.get())));
    public static final DeferredHolder<Item, SpawnEggItem> SCULPTOR_SPAWN_EGG = REG.register("sculptor_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("sculptor_spawn_egg"))).spawnEgg(EntityHandler.SCULPTOR.get())));
    public static final DeferredHolder<Item, SpawnEggItem> BLUFF_SPAWN_EGG = REG.register("bluff_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("bluff_spawn_egg"))).spawnEgg(EntityHandler.BLUFF.get())));
    public static final DeferredHolder<Item, SpawnEggItem> ELOKOSA_SPAWN_EGG = REG.register("elokosa_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_spawn_egg"))).spawnEgg(EntityHandler.ELOKOSA_FOLLOWER_TO_HOWLER.get())));
    public static final DeferredHolder<Item, SpawnEggItem> ELOKOSA_HOWLER_SPAWN_EGG = REG.register("elokosa_howler_spawn_egg", () -> new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, MMCommon.resource("elokosa_howler_spawn_egg"))).spawnEgg(EntityHandler.ELOKOSA_HOWLER.get())));


    // TODO: Some tools missing
    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        if (!ConfigHandler.COMMON_CONFIG.isLoaded()) {
            // Should only be the case for data generation
            MMCommon.LOGGER.error("Could not modify default components due to config not being loaded yet");
            return;
        }

        event.modify(WROUGHT_AXE.get(), builder -> {
            if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.breakable.get()) {
                builder.set(DataComponents.MAX_DAMAGE, null);
                builder.set(DataComponents.DAMAGE, null);
            }

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributes(
                    ToolMaterial.IRON,
                    NEGATE_ATTACK_DAMAGE + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.toolConfig.attackDamage.get().floatValue(),
                    NEGATE_ATTACK_SPEED + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.toolConfig.attackSpeed.get().floatValue()
            ));
        });

        event.modify(SPEAR.get(), builder -> {
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributes(
                    ToolMaterial.STONE,
                    NEGATE_ATTACK_DAMAGE + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SPEAR.toolConfig.attackDamage.get().floatValue(),
                    NEGATE_ATTACK_SPEED + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SPEAR.toolConfig.attackSpeed.get().floatValue()
            ).withModifierAdded(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(ItemSpear.SPEAR_REACH_ID, 1.5, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            ));
        });

        event.modify(NAGA_FANG_DAGGER.get(), builder -> {
            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributes(
                    ToolMaterial.STONE,
                    NEGATE_ATTACK_DAMAGE + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.NAGA_FANG_DAGGER.toolConfig.attackDamage.get().floatValue(),
                    NEGATE_ATTACK_SPEED + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.NAGA_FANG_DAGGER.toolConfig.attackSpeed.get().floatValue()
            ));
            // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - repair-by-ItemNagaFang
            // is applied here instead, now that every item is guaranteed to be registered.
            builder.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(NAGA_FANG.get().builtInRegistryHolder())));
        });

        event.modify(EARTHREND_GAUNTLET.get(), builder -> {
            // DiggerItem (via Properties#pickaxe) implicitly grants enchantability from the tool material - strip it
            // back off to match the old "isEnchantable() -> false" override, which no longer exists as an override point.
            builder.set(DataComponents.ENCHANTABLE, null);

            if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.breakable.get()) {
                builder.set(DataComponents.MAX_DAMAGE, null);
                builder.set(DataComponents.DAMAGE, null);
            }

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributes(
                    ToolMaterial.STONE,
                    NEGATE_ATTACK_DAMAGE + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.toolConfig.attackDamage.get().floatValue(),
                    NEGATE_ATTACK_SPEED + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.toolConfig.attackSpeed.get().floatValue()
            ));
        });

        event.modify(SOL_VISAGE.get(), builder -> {
            if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SOL_VISAGE.breakable.get()) {
                builder.set(DataComponents.MAX_DAMAGE, null);
                builder.set(DataComponents.DAMAGE, null);
                // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - repair-only-while-
                // breakable is now expressed by stripping the REPAIRABLE component .humanoidArmor() implicitly added.
                builder.set(DataComponents.REPAIRABLE, null);
            }
        });

        event.modify(WROUGHT_HELMET.get(), builder -> {
            if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.WROUGHT_HELM.breakable.get()) {
                builder.set(DataComponents.MAX_DAMAGE, null);
                builder.set(DataComponents.DAMAGE, null);
                builder.set(DataComponents.REPAIRABLE, null);
            }
        });

        // Never repairable (old isValidRepairItem(ItemStack, ItemStack) override always returned false, which no
        // longer exists as an override point) - strip the placeholder REPAIRABLE component .humanoidArmor() added.
        for (DeferredHolder<Item, ItemUmvuthanaMask> mask : java.util.List.of(
                UMVUTHANA_MASK_FURY, UMVUTHANA_MASK_FEAR, UMVUTHANA_MASK_RAGE, UMVUTHANA_MASK_BLISS, UMVUTHANA_MASK_MISERY, UMVUTHANA_MASK_FAITH)) {
            event.modify(mask.get(), builder -> builder.set(DataComponents.REPAIRABLE, null));
        }

        // Durability initially gets set in 'TieredItem' based on the tier
        event.modify(SCULPTOR_STAFF.get(), builder -> {
            // DiggerItem (via Properties#hoe) implicitly grants enchantability from the tool material - strip it
            // back off to match the old "isEnchantable() -> false" override, which no longer exists as an override point.
            builder.set(DataComponents.ENCHANTABLE, null);

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, createToolAttributes(
                    ToolMaterial.STONE,
                    NEGATE_ATTACK_DAMAGE + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SCULPTOR_STAFF.toolConfig.attackDamage.get().floatValue(),
                    NEGATE_ATTACK_SPEED + ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SCULPTOR_STAFF.toolConfig.attackSpeed.get().floatValue()
            ));
            builder.set(DataComponents.MAX_DAMAGE, 200);
            // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - repair-by-BLUFF_ROD is
            // applied here instead, now that every item is guaranteed to be registered.
            builder.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(BLUFF_ROD.get().builtInRegistryHolder())));
        });

        // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - repair-by-BLUFF_ROD is
        // applied here instead of in the constructor (overriding the placeholder tag-based repair component that
        // Properties#humanoidArmor sets), now that every item is guaranteed to be registered.
        for (DeferredHolder<Item, ItemGeomancerArmor> geomancerPiece : java.util.List.of(GEOMANCER_BEADS, GEOMANCER_ROBE, GEOMANCER_BELT, GEOMANCER_SANDALS)) {
            event.modify(geomancerPiece.get(), builder ->
                    builder.set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(BLUFF_ROD.get().builtInRegistryHolder()))));
        }

        for (DeferredHolder<Item, ItemElokosaPaw> item : ELOKOSA_PAWS) {
            event.modify(item.get(), builder -> {
                builder.set(DataComponents.MAX_DAMAGE, ConfigHandler.COMMON.TOOLS_AND_ABILITIES.ELOKOSA_PAW.numberOfUses.getAsInt());
            });
        }
    }

    /**
     * Replacement for the old {@code DiggerItem.createAttributes}/{@code AxeItem.createAttributes} static helpers,
     * which no longer exist now that DiggerItem/PickaxeItem/SwordItem etc. were removed upstream in favor of plain
     * Item + ToolMaterial. Mirrors {@code ToolMaterial#createToolAttributes} (private in vanilla).
     */
    private static ItemAttributeModifiers createToolAttributes(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamageBaseline + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeedBaseline, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    public static void initializeDispenserBehaviors() {
        DispenserBlock.registerBehavior(DART.get(), new ProjectileDispenseBehavior(DART.get()));
    }
}