package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.client.render.entity.RenderUmvuthi;
import com.bobmowzie.mowziesmobs.client.render.item.RenderSolVisageArmor;
import com.bobmowzie.mowziesmobs.client.render.item.RenderSolVisageItem;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Created by BobMowzie on 8/15/2016.
 */
public class ItemSolVisage extends Item implements UmvuthanaMask, GeoItem {
    public String controllerName = "controller";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ItemSolVisage(Item.Properties properties) {
        // ArmorItem was removed upstream - repair-while-breakable is now handled via a conditional
        // DataComponents.REPAIRABLE removal in ItemHandler#modifyComponents instead of overriding
        // isValidRepairItem(ItemStack, ItemStack), which no longer exists. .humanoidArmor() already grants
        // enchantability from the material's enchantment value, matching the old "isEnchantable() -> true" override.
        super(properties.humanoidArmor(MaterialHandler.SOL_VISAGE_MATERIAL, ArmorType.HELMET));
    }

    @Override
    public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.1").setStyle(ItemHandler.TOOLTIP_STYLE));
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.2").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    private PlayState predicate(AnimationTest<ItemSolVisage> state) {
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ItemSolVisage>(controllerName, 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // PORTING NOTE (GeckoLib 4 -> 5): see ItemUmvuthanaMask#createGeoRenderer's porting note for the full
    // explanation - GeckoLib 5 supplies both the held/inventory GeoItemRenderer and the worn GeoArmorRenderer
    // through this single GeoItem hook now, instead of the old ClientExtensions#getHumanoidArmorModel/
    // getCustomRenderer(BlockEntityWithoutLevelRenderer) pair (BlockEntityWithoutLevelRenderer no longer exists at
    // all in this MC version).
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<ItemSolVisage> itemRenderer;
            private GeoArmorRenderer<ItemSolVisage, ?> armorRenderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (itemRenderer == null) itemRenderer = new RenderSolVisageItem();
                return itemRenderer;
            }

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot equipmentSlot) {
                if (armorRenderer == null) armorRenderer = new RenderSolVisageArmor();
                return armorRenderer;
            }
        });
    }

    // Kept as a near-empty implementation solely because MMClient.java (out of this scope) still registers one via
    // RegisterClientExtensionsEvent#registerItem(new ItemSolVisage.ClientExtensions(), ...) - see
    // ItemUmvuthanaMask.ClientExtensions for the same pattern/reasoning.
    public static class ClientExtensions implements IClientItemExtensions {
        @Override
        public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
            return RenderUmvuthi.TEXTURE;
        }
    }
}
