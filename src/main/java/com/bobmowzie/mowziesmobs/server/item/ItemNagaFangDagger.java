package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class ItemNagaFangDagger extends Item {
    public ItemNagaFangDagger(Item.Properties properties) {
        // DiggerItem was removed upstream. Repair-by-ItemNagaFang is applied later in
        // ItemHandler#modifyComponents (once all items are guaranteed to be registered) via the
        // DataComponents.REPAIRABLE component, instead of overriding isValidRepairItem(ItemStack, ItemStack),
        // which no longer exists. Not done here in the constructor since ItemHandler.NAGA_FANG's DeferredHolder
        // is not guaranteed to be bound yet while items are still being constructed/registered.
        super(properties.hoe(ToolMaterial.STONE, 0F, 0F));
    }

    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override // Enchantment table
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SWEEPING_EDGE)) {
            return false;
        }

        return super.isPrimaryItemFor(stack, enchantment);
    }

    @Override // Anvil / Enchant command / Enchant randomly function
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SWEEPING_EDGE)) {
            return false;
        }

        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, ConfigHandler.COMMON.TOOLS_AND_ABILITIES.NAGA_FANG_DAGGER.poisonDuration.get(), 3, false, true));
    }

    // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - repair-by-ItemNagaFang is now
    // set via DataComponents.REPAIRABLE in ItemHandler#modifyComponents.

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
    }
}
