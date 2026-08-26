package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.render.item.RenderUmvuthanaMaskArmor;
import com.bobmowzie.mowziesmobs.client.render.item.RenderUmvuthanaMaskItem;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthanaCraneToPlayer;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthanaFollowerToPlayer;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.MaskType;
import com.bobmowzie.mowziesmobs.server.sound.MMSounds;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ItemUmvuthanaMask extends Item implements UmvuthanaMask, GeoItem {
    private final MaskType type;

    public String controllerName = "controller";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ItemUmvuthanaMask(MaskType type, Item.Properties properties) {
        // ArmorItem was removed upstream. This mask is intentionally never repairable (old override always
        // returned false) - the placeholder REPAIRABLE component .humanoidArmor() adds is stripped back off in
        // ItemHandler#modifyComponents.
        super(properties.humanoidArmor(MaterialHandler.UMVUTHANA_MASK_MATERIAL, ArmorType.HELMET));
        this.type = type;
    }

    public Holder<MobEffect> getPotion() {
        return type.potion;
    }

    // isValidRepairItem(ItemStack, ItemStack) no longer exists as an override point - see constructor above.

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.getItem() instanceof ItemSolVisage) {
            if (ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SOL_VISAGE.breakable.get() && !player.isCreative()) headStack.hurtAndBreak(2, player, hand.asEquipmentSlot());
            boolean didSpawn = spawnUmvuthana(type, stack, player,(float)stack.getDamageValue() / (float)stack.getMaxDamage());
            if (didSpawn) {
                if (!player.isCreative()) stack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return super.use(world, player, hand);
    }

    private boolean spawnUmvuthana(MaskType mask, ItemStack stack, Player player, float durability) {
        if (DataHandler.getData(player, DataHandler.PLAYER_DATA).getPackSize() < ConfigHandler.COMMON.TOOLS_AND_ABILITIES.SOL_VISAGE.maxFollowers.get()) {
            player.playSound(MMSounds.ENTITY_UMVUTHI_BELLY.get(), 1.5f, 1);
            player.playSound(MMSounds.ENTITY_UMVUTHANA_BLOWDART.get(), 1.5f, 0.5f);
            double angle = player.getYHeadRot();
            if (angle < 0) {
                angle = angle + 360;
            }
            EntityUmvuthanaFollowerToPlayer umvuthana;
            if (mask == MaskType.FAITH) umvuthana = new EntityUmvuthanaCraneToPlayer(EntityHandler.UMVUTHANA_CRANE_TO_PLAYER.get(), player.level(), player);
            else umvuthana = new EntityUmvuthanaFollowerToPlayer(EntityHandler.UMVUTHANA_FOLLOWER_TO_PLAYER.get(), player.level(), player);
//            property.addPackMember(umvuthana);
            if (!player.level().isClientSide()) {
                if (mask != MaskType.FAITH) {
                    int weapon;
                    if (mask != MaskType.FURY) weapon = umvuthana.randomizeWeapon();
                    else weapon = 0;
                    umvuthana.setWeapon(weapon);
                }
                umvuthana.snapTo(player.getX() + 1 * Math.sin(-angle * (Math.PI / 180)), player.getY() + 1.5, player.getZ() + 1 * Math.cos(-angle * (Math.PI / 180)), (float) angle, 0);
                umvuthana.setActive(false);
                umvuthana.active = false;
                player.level().addFreshEntity(umvuthana);
                double vx = 0.5 * Math.sin(-angle * Math.PI / 180);
                double vy = 0.5;
                double vz = 0.5 * Math.cos(-angle * Math.PI / 180);
                umvuthana.setDeltaMovement(vx, vy, vz);
                umvuthana.setHealth((1.0f - durability) * umvuthana.getMaxHealth());
                umvuthana.setMask(mask);
                umvuthana.setStoredMask(stack.copy());
                if (stack.has(DataComponents.CUSTOM_NAME))
                    umvuthana.setCustomName(stack.getHoverName());
            }
            return true;
        }
        return false;
    }

    public MaskType getMaskType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    private static final RawAnimation UMVUTHANA_ANIM = RawAnimation.begin().thenLoop("umvuthana");
    private static final RawAnimation PLAYER_ANIM = RawAnimation.begin().thenLoop("player");
    public <P extends Item & GeoItem> PlayState predicate(AnimationTest<P> event) {
        // UNRESOLVED: GeckoLib 5.5.2 removed DataTickets.ENTITY (no replacement ticket found in
        // com.geckolib.constant.DataTickets for "the entity currently wearing/using this item"), so this predicate
        // can no longer distinguish an EntityUmvuthana wearer from a Player wearer. Falling back to the player
        // animation unconditionally (previously only the non-Umvuthana branch) until a real replacement is found -
        // needs follow-up once GeckoLib's per-wearer render-state plumbing for GeoItem controllers is understood.
        event.controller().setAnimation(PLAYER_ANIM);
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ItemUmvuthanaMask>(controllerName, 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // PORTING NOTE (GeckoLib 4 -> 5): replaces the old ClientExtensions#getHumanoidArmorModel/getCustomRenderer
    // wiring (BlockEntityWithoutLevelRenderer for the in-hand model, GeoArmorRenderer returned from
    // getHumanoidArmorModel for the worn model) - both mechanisms are gone in this MC/GeckoLib version:
    // BlockEntityWithoutLevelRenderer no longer exists anywhere in vanilla (confirmed by grepping the full vanilla
    // source tree), and GeckoLib 5's armor rendering bypasses IClientItemExtensions#getHumanoidArmorModel entirely
    // via its own internal mixin (see ItemGeomancerArmor#createGeoRenderer's porting note for the full citation
    // trail). Both the held/inventory render (GeoItemRenderer) and the worn-armor render (GeoArmorRenderer) are now
    // supplied together through this single GeoItem hook.
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ItemUmvuthanaMask> itemRenderer;
            private GeoArmorRenderer<ItemUmvuthanaMask, ?> armorRenderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (itemRenderer == null) itemRenderer = new RenderUmvuthanaMaskItem();
                return itemRenderer;
            }

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot equipmentSlot) {
                if (armorRenderer == null) armorRenderer = new RenderUmvuthanaMaskArmor();
                return armorRenderer;
            }
        });
    }

    // Kept as a near-empty implementation solely because MMClient.java (out of this scope) still registers one via
    // RegisterClientExtensionsEvent#registerItem(new ItemUmvuthanaMask.ClientExtensions(), ...). getArmorTexture is
    // likely vestigial now too (GeckoLib resolves its own armor texture via the GeoModel, not this vanilla hook),
    // but kept since it's a harmless, cheap override in case some fallback path still consults it.
    public static class ClientExtensions implements IClientItemExtensions {
        @Override
        public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
            MaskType maskType = ((ItemUmvuthanaMask) stack.getItem()).getMaskType();
            return Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/umvuthana_mask_" + maskType.name + ".png");
        }
    }
}
