package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.client.render.item.RenderSculptorStaff;
import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * Created by BobMowzie on 6/6/2017.
 */
public class ItemSculptorStaff extends Item implements GeoItem {
    public static final String CONTROLLER_NAME = "controller";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation DISAPPEAR_ANIM = RawAnimation.begin().thenPlayAndHold("disappear");
    public static final String DISAPPEAR_ANIM_NAME = "disappear";

    public ItemSculptorStaff(Properties properties) {
        // DiggerItem was removed upstream. Repair-by-BLUFF_ROD is applied later in ItemHandler#modifyComponents
        // (once all items are guaranteed to be registered) instead of overriding isValidRepairItem(ItemStack,
        // ItemStack), which no longer exists - not done here since ItemHandler.BLUFF_ROD's DeferredHolder is not
        // guaranteed to be bound yet while items are still being constructed/registered. The ENCHANTABLE component
        // .hoe() implicitly adds is stripped back off in ItemHandler#modifyComponents too (see below).
        super(properties.hoe(ToolMaterial.STONE, 0F, 0F));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - see ItemHandler#modifyComponents.

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        AbilityHandler.INSTANCE.sendAbilityMessage(player, AbilityHandler.ROCK_SLING);
        player.startUsingItem(hand);
        if (!player.getAbilities().instabuild) player.getItemInHand(hand).hurtAndBreak(2, player, hand.asEquipmentSlot());
        return InteractionResult.SUCCESS;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    // isEnchantable(ItemStack) no longer exists as an Item/IItemExtension override point - non-enchantability is now
    // enforced by stripping DataComponents.ENCHANTABLE in ItemHandler#modifyComponents.

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ItemSculptorStaff>(CONTROLLER_NAME, 0, state -> PlayState.STOP)
                .triggerableAnim(DISAPPEAR_ANIM_NAME, DISAPPEAR_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // PORTING NOTE (GeckoLib 4 -> 5): see ItemEarthrendGauntlet#createGeoRenderer's porting note - same pattern,
    // BlockEntityWithoutLevelRenderer no longer exists, replaced by GeoItem#createGeoRenderer.
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ItemSculptorStaff> itemRenderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (itemRenderer == null) itemRenderer = new RenderSculptorStaff();
                return itemRenderer;
            }
        });
    }

    // Kept as an empty implementation solely because MMClient.java (out of this scope) still registers one via
    // RegisterClientExtensionsEvent#registerItem(new ItemSculptorStaff.ClientExtensions(), ...) - see
    // ItemUmvuthanaMask.ClientExtensions for the same pattern/reasoning.
    public static class ClientExtensions implements IClientItemExtensions {
    }
}
