package com.bobmowzie.mowziesmobs.server.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Created by BobMowzie on 1/10/2019.
 */
public class ItemBluffRod extends Item {
    public ItemBluffRod(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull final ItemStack stack, @NotNull final TooltipContext context, @NotNull final TooltipDisplay display, @NotNull final Consumer<Component> tooltipComponents, @NotNull final TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, display, tooltipComponents, tooltipFlag);
        tooltipComponents.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
    }
}
