package com.bobmowzie.mowziesmobs.mixin;

import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigurableArmorMaterial;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

// NOTE (1.21.1 -> 26.1.2 port): ArmorMaterial used to be a regular class with virtual
// `getDefense(ArmorType)`/`toughness()` methods that this mixin overrode via ModifyReturnValue.
// It is now a Java record (net.minecraft.world.item.equipment.ArmorMaterial) whose ONLY consumer of the
// defense/toughness values - createAttributes(ArmorType) - reads the private final `defense`/`toughness`
// record fields directly (this.defense / this.toughness), NOT through the public record accessor methods.
// That means a mixin on the `defense()`/`toughness()` accessors would never actually be invoked by the
// code that matters and would silently do nothing. Instead, this mixin now hooks the RETURN of
// createAttributes(ArmorType) itself (a real, non-bypassed instance method) and rescales the ARMOR /
// ARMOR_TOUGHNESS attribute modifier amounts it produces. createAttributes(...) is invoked exactly once,
// from Item.Properties#humanoidArmor(...)/wolfArmor(...)/etc. at item-registration time (baked into the
// item's default EQUIPPABLE/attribute data components), which is after MaterialHandler sets the config via
// mowziesmobs$setConfig(...) on the freshly constructed material, so ordering is preserved.
@Mixin(ArmorMaterial.class)
public abstract class ArmorMaterialMixin implements ConfigurableArmorMaterial {
    @Unique @Nullable private ConfigHandler.ArmorConfig mowziesmobs$config;

    @Override
    public void mowziesmobs$setConfig(ConfigHandler.ArmorConfig config) {
        this.mowziesmobs$config = config;
    }

    @ModifyReturnValue(method = "createAttributes", at = @At("RETURN"))
    private ItemAttributeModifiers mowziesmobs$configurableAttributes(ItemAttributeModifiers original, ArmorType type) {
        // PORTING NOTE (1.21.1 -> 26.1.2): createAttributes(...) is invoked exactly once, from
        // Item.Properties#humanoidArmor(...) at item-registration time - but registration now happens BEFORE
        // config files are loaded (confirmed: reading a ModConfigSpec value here throws "Cannot get config value
        // before config is loaded" at startup). Since this hook only ever fires once and always during that early
        // window, config-based armor damage/toughness scaling can no longer take effect at all - falling back to
        // unscaled (1.0x) values rather than crashing. This is a real, confirmed behavior loss, not a workaround.
        if (mowziesmobs$config == null || !ConfigHandler.COMMON_CONFIG.isLoaded()) {
            return original;
        }

        float defenseMultiplier = mowziesmobs$config.damageReductionMultiplier.get().floatValue();
        float toughnessMultiplier = mowziesmobs$config.toughnessMultiplier.get().floatValue();

        List<ItemAttributeModifiers.Entry> newEntries = new ArrayList<>(original.modifiers().size());
        for (ItemAttributeModifiers.Entry entry : original.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            if (entry.attribute() == Attributes.ARMOR) {
                modifier = new AttributeModifier(modifier.id(), modifier.amount() * defenseMultiplier, modifier.operation());
            } else if (entry.attribute() == Attributes.ARMOR_TOUGHNESS) {
                modifier = new AttributeModifier(modifier.id(), modifier.amount() * toughnessMultiplier, modifier.operation());
            }
            newEntries.add(new ItemAttributeModifiers.Entry(entry.attribute(), modifier, entry.slot(), entry.display()));
        }

        return new ItemAttributeModifiers(newEntries);
    }
}
