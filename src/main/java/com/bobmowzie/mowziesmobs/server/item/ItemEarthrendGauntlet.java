package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.client.render.item.RenderEarthrendGauntlet;
import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.SingletonGeoAnimatable;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

/**
 * Created by BobMowzie on 6/6/2017.
 */
public class ItemEarthrendGauntlet extends Item implements GeoItem {
    public static final String CONTROLLER_NAME = "controller";
    public static final String CONTROLLER_IDLE_NAME = "controller_idle";

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation OPEN_ANIM = RawAnimation.begin().thenLoop("open");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("attack");
    public static final String IDLE_ANIM_NAME = "idle";
    public static final String OPEN_ANIM_NAME = "open";
    public static final String ATTACK_ANIM_NAME = "attack";

    public ItemEarthrendGauntlet(Properties properties) {
        // DiggerItem was removed upstream - tools are now plain Items configured via Properties#pickaxe/tool.
        // Attack attributes are re-applied explicitly in ItemHandler#modifyComponents from config, and the
        // ENCHANTABLE component this implicitly adds is stripped back off there too (see isEnchantable() below).
        super(properties.pickaxe(ToolMaterial.STONE, 0F, 0F));

        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public InteractionResult use(Level worldIn, Player player, InteractionHand handIn) {
        ItemStack stack = player.getItemInHand(handIn);
        player.startUsingItem(handIn);
        if (stack.getDamageValue() + 5 < stack.getMaxDamage() || ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.breakable.get()) {
            if (!worldIn.isClientSide()) AbilityHandler.INSTANCE.sendAbilityMessage(player, AbilityHandler.TUNNELING_ABILITY);
            player.startUsingItem(handIn);
            return InteractionResult.SUCCESS;
        }
        else {
            DataHandler.getData(player, DataHandler.ABILITY_DATA).getAbilityMap().get(AbilityHandler.TUNNELING_ABILITY).end();
        }
        return super.use(worldIn, player, handIn);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.durability.get();
    }

    // isEnchantable(ItemStack) no longer exists as an Item/IItemExtension override point - non-enchantability is now
    // enforced by stripping DataComponents.ENCHANTABLE in ItemHandler#modifyComponents.

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        if (ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.enableTunneling.get()) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
        }
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.2").setStyle(ItemHandler.TOOLTIP_STYLE));
        if (!ConfigHandler.COMMON.TOOLS_AND_ABILITIES.EARTHREND_GAUNTLET.breakable.get()) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".text.3").setStyle(ItemHandler.TOOLTIP_STYLE));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ItemEarthrendGauntlet>(CONTROLLER_IDLE_NAME, 3, this::predicateIdle));
        controllers.add(new AnimationController<ItemEarthrendGauntlet>(CONTROLLER_NAME, 3, state -> PlayState.STOP)
                .triggerableAnim(IDLE_ANIM_NAME, IDLE_ANIM)
                .triggerableAnim(OPEN_ANIM_NAME, OPEN_ANIM)
                .triggerableAnim(ATTACK_ANIM_NAME, ATTACK_ANIM));
    }

    public <P extends Item & GeoItem> PlayState predicateIdle(AnimationTest<P> event) {
        event.controller().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (DataHandler.getData(entity, DataHandler.ABILITY_DATA).getActiveAbility() == null) {
            if (entity.getUseItem() != stack) {
                if (entity.level() instanceof ServerLevel) {
                    triggerAnim(entity, GeoItem.getOrAssignId(stack, (ServerLevel) entity.level()), CONTROLLER_NAME, ATTACK_ANIM_NAME);
                }
            }
        }
        return super.onEntitySwing(stack, entity, hand);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (player.getUseItem() != stack) {
            if (entity.level() instanceof ServerLevel) {
                triggerAnim(entity, GeoItem.getOrAssignId(stack, (ServerLevel) entity.level()), CONTROLLER_NAME, ATTACK_ANIM_NAME);
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // PORTING NOTE (GeckoLib 4 -> 5): BlockEntityWithoutLevelRenderer no longer exists anywhere in vanilla
    // (confirmed by grepping the full vanilla source tree). The old ClientExtensions#getCustomRenderer wiring for
    // "this GeoItem needs a custom in-hand/inventory model" is replaced by GeoItem#createGeoRenderer(Consumer
    // <GeoRenderProvider>) - see ItemUmvuthanaMask#createGeoRenderer's porting note for the full citation trail
    // (GeckoLib's own GeckolibItemSpecialRenderer resolves the renderer via GeoRenderProvider.of(item)
    // .getGeoItemRenderer(), driven by a data-generated "special model" item model rather than the old BEWLR path).
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ItemEarthrendGauntlet> itemRenderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (itemRenderer == null) itemRenderer = new RenderEarthrendGauntlet();
                return itemRenderer;
            }
        });
    }

    // Kept as an empty implementation solely because MMClient.java (out of this scope) still registers one via
    // RegisterClientExtensionsEvent#registerItem(new ItemEarthrendGauntlet.ClientExtensions(), ...) - see
    // ItemUmvuthanaMask.ClientExtensions for the same pattern/reasoning.
    public static class ClientExtensions implements IClientItemExtensions {
    }
}
