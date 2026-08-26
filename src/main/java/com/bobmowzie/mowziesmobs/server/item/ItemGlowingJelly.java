package com.bobmowzie.mowziesmobs.server.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by BobMowzie on 7/30/2018.
 */

public class ItemGlowingJelly extends Item {
    // PORTING NOTE (1.21.1 -> 26.1.2): FoodProperties.Builder#effect(...) no longer exists - FoodProperties
    // (net.minecraft.world.food.FoodProperties) dropped its effects list entirely and is now just
    // (nutrition, saturation, canAlwaysEat). Consumed-on-eat effects moved to a separate
    // net.minecraft.world.item.component.Consumable component (see vanilla Consumables.java for the real pattern -
    // e.g. Consumables.GOLDEN_APPLE/PUFFERFISH build a Consumable via defaultFood().onConsume(new
    // ApplyStatusEffectsConsumeEffect(...))). Item.Properties#food(FoodProperties, Consumable) is the matching
    // 2-arg overload used in ItemHandler to attach both together (see GLOWING_JELLY registration).
    public static FoodProperties GLOWING_JELLY_FOOD = new FoodProperties.Builder().nutrition(1).saturationModifier(0.1f).build();
    public static Consumable GLOWING_JELLY_CONSUMABLE = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0), 1.0f))
            .build();

    public ItemGlowingJelly(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
    }
}
