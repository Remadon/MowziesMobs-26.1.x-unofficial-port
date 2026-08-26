package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigurableArmorMaterial;
import net.minecraft.util.Util;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;

// PORTING NOTE (1.21.1 -> 26.1.2): ArmorMaterial is no longer a registry-backed type - there is NO
// Registries.ARMOR_MATERIAL / BuiltInRegistries.ARMOR_MATERIAL entry anymore (confirmed by grepping
// core/registries/Registries.java and BuiltInRegistries.java - neither mentions armor material at all).
// It's a plain record now (net.minecraft.world.item.equipment.ArmorMaterial), passed directly into
// Item.Properties#humanoidArmor(ArmorMaterial, ArmorType) - see vanilla Item.java's Properties inner class.
// This class therefore no longer needs (or can use) a DeferredRegister<ArmorMaterial>/DeferredHolder wrapper;
// the fields are now plain static final ArmorMaterial instances, and MaterialHandler has nothing to register
// on the mod bus anymore.
// CROSS-SCOPE FLAG: MMCommon.java:64 still calls `MaterialHandler.MM_ARMOR_MATERIALS.register(modBus);` -
// that field no longer exists (see below) and that line needs to be deleted by whoever owns MMCommon.java
// (out of server/item/** and server/ability/** scope, not touched by this pass).
public class MaterialHandler { // FIXME 1.21 :: unsure if or where the layer resources are needed
    // Toughness and defense gets set as the base value, the configurable multipliers are applied through the 'ArmorMaterialMixin'
    // NOTE: durability field is the per-piece "unit durability multiplier" (matches the old ArmorItem.Type.getDurability(x) call site in ItemHandler).
    // assetId currently reuses the closest vanilla equipment asset as a placeholder since these items are rendered client-side via a
    // custom GeoArmorRenderer (see ClientExtensions#getHumanoidArmorModel in each item class) and never use the vanilla equipment texture
    // pipeline - a dedicated equipment asset JSON (assets/mowziesmobs/equipment/*.json) should still be authored for full correctness.
    public static final ArmorMaterial SOL_VISAGE_MATERIAL = build(() -> {
        ArmorMaterial material = new ArmorMaterial(
                7,
                Util.make(new EnumMap<>(ArmorType.class), map -> {
                    map.put(ArmorType.HELMET, ArmorMaterials.GOLD.defense().get(ArmorType.HELMET));
                }),
                ArmorMaterials.GOLD.enchantmentValue(),
                ArmorMaterials.GOLD.equipSound(),
                ArmorMaterials.GOLD.toughness(),
                ArmorMaterials.GOLD.knockbackResistance(),
                ArmorMaterials.GOLD.repairIngredient(),
                EquipmentAssets.GOLD
        );

        // Need to trick the compiler
        ConfigurableArmorMaterial configurable = (ConfigurableArmorMaterial) (Object) material;
        configurable.mowziesmobs$setConfig(ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SOL_VISAGE.armorConfig);

        return material;
    });

    public static final ArmorMaterial UMVUTHANA_MASK_MATERIAL = build(() -> {
        ArmorMaterial material = new ArmorMaterial(
                5,
                Util.make(new EnumMap<>(ArmorType.class), map -> {
                    map.put(ArmorType.HELMET, ArmorMaterials.LEATHER.defense().get(ArmorType.HELMET));
                }),
                ArmorMaterials.LEATHER.enchantmentValue(),
                ArmorMaterials.LEATHER.equipSound(),
                ArmorMaterials.LEATHER.toughness(),
                ArmorMaterials.LEATHER.knockbackResistance(),
                // Not actually repairable (previously repair ingredient was Items.AIR) - the REPAIRABLE component is stripped
                // back off in ItemHandler#modifyComponents for the umvuthana mask items.
                ArmorMaterials.LEATHER.repairIngredient(),
                EquipmentAssets.LEATHER
        );

        ConfigurableArmorMaterial configurable = (ConfigurableArmorMaterial) (Object) material;
        configurable.mowziesmobs$setConfig(ConfigHandler.COMMON.TOOLS_AND_ABILITIES.UMVUTHANA_MASK.armorConfig);

        return material;
    });

    public static final ArmorMaterial ARMOR_WROUGHT_HELM = build(() -> {
        ArmorMaterial material = new ArmorMaterial(
                15,
                Util.make(new EnumMap<>(ArmorType.class), map -> {
                    map.put(ArmorType.HELMET, ArmorMaterials.IRON.defense().get(ArmorType.HELMET));
                }),
                ArmorMaterials.IRON.enchantmentValue(),
                ArmorMaterials.IRON.equipSound(),
                ArmorMaterials.IRON.toughness(),
                0.1f,
                ArmorMaterials.IRON.repairIngredient(),
                EquipmentAssets.IRON
        );

        ConfigurableArmorMaterial configurable = (ConfigurableArmorMaterial) (Object) material;
        configurable.mowziesmobs$setConfig(ConfigHandler.COMMON.TOOLS_AND_ABILITIES.WROUGHT_HELM.armorConfig);

        return material;
    });

    public static final ArmorMaterial GEOMANCER_ARMOR_MATERIAL = build(() -> {
        ArmorMaterial material = new ArmorMaterial(
                33,
                Util.make(new EnumMap<>(ArmorType.class), map -> {
                    map.put(ArmorType.BOOTS, 2);
                    map.put(ArmorType.LEGGINGS, 6);
                    map.put(ArmorType.CHESTPLATE, 7);
                    map.put(ArmorType.HELMET, 2);
                }),
                ArmorMaterials.IRON.enchantmentValue(),
                ArmorMaterials.IRON.equipSound(),
                ArmorMaterials.IRON.toughness(),
                0,
                // Placeholder tag - actual single-item repair (BLUFF_ROD) is applied via Item.Properties#repairable(Item)
                // on top of this in ItemHandler, since ArmorMaterial only supports tag-based repair ingredients now.
                ArmorMaterials.IRON.repairIngredient(),
                EquipmentAssets.IRON
        );

        ConfigurableArmorMaterial configurable = (ConfigurableArmorMaterial) (Object) material;
        configurable.mowziesmobs$setConfig(ConfigHandler.COMMON.TOOLS_AND_ABILITIES.GEOMANCER_ARMOR.armorConfig);

        return material;
    });

    // Trivial passthrough kept so each field above can stay a self-contained lambda block (matches the old
    // DeferredRegister#register(name, Supplier) call shape) even though there's no registry to register into anymore.
    private static ArmorMaterial build(java.util.function.Supplier<ArmorMaterial> factory) {
        return factory.get();
    }
}
