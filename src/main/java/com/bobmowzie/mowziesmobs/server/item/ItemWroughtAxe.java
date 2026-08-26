package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.bobmowzie.mowziesmobs.server.capability.PlayerData;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Consumer;

public class ItemWroughtAxe extends AxeItem {

    public ItemWroughtAxe(Item.Properties properties) {
        // AxeItem now takes explicit attack damage/speed baselines; attributes are re-applied explicitly from
        // config in ItemHandler#modifyComponents so the baseline passed here is irrelevant.
        super(ToolMaterial.IRON, 0F, 0F, properties);
    }

    // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point. The old "only repairable while
    // breakable" behavior has no direct DataComponents.REPAIRABLE equivalent - flagged as unresolved, needs follow-up.

    // isEnchantable(ItemStack) no longer exists as an override point; AxeItem's properties already grant
    // enchantability from the tool material's enchantment value, matching the old "return true" behavior.

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity instanceof Player) {
            PlayerData data = DataHandler.getData(entity, DataHandler.PLAYER_DATA);
            return !data.getAxeCanAttack() && data.getUntilAxeSwing() > 0;
        }

        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player) {
            return DataHandler.getData(entity, DataHandler.PLAYER_DATA).getUntilAxeSwing() > 0;
        }

        return false;
    }

    @Override
    public void hurtEnemy(ItemStack heldItemStack, LivingEntity entityHit, LivingEntity attacker) {
        if (ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.breakable.get()) heldItemStack.hurtAndBreak(2, attacker, InteractionHand.MAIN_HAND.asEquipmentSlot());
        if (!entityHit.level().isClientSide()) {
            entityHit.playSound(SoundEvents.ANVIL_LAND, 0.3F, 0.5F);
        }
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.getAttackStrengthScale(0.5F) == 1.0f) {
            PlayerData data = DataHandler.getData(player, DataHandler.PLAYER_DATA);

            if (data.getUntilAxeSwing() <= 0) {
                boolean verticalAttack = player.isShiftKeyDown() && player.onGround();
                if (verticalAttack)
                    AbilityHandler.INSTANCE.sendAbilityMessage(player, AbilityHandler.WROUGHT_AXE_SLAM_ABILITY);
                else
                    AbilityHandler.INSTANCE.sendAbilityMessage(player, AbilityHandler.WROUGHT_AXE_SWING_ABILITY);
                data.setVerticalSwing(verticalAttack);
                data.setUntilAxeSwing(30);
                player.startUsingItem(hand);
                if (ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.breakable.get() && !player.getAbilities().instabuild) player.getItemInHand(hand).hurtAndBreak(2, player, hand.asEquipmentSlot());
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(world, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.AXE_OF_A_THOUSAND_METALS.breakable.get()) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        }
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.2").setStyle(ItemHandler.TOOLTIP_STYLE));
    }
}
