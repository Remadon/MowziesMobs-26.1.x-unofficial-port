package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.client.render.item.RenderGeomancerArmor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ItemGeomancerArmor extends Item implements GeoItem {
    public String controllerName = "controller";
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ItemGeomancerArmor(ArmorType slot, Properties builderIn) {
        // ArmorItem was removed upstream. Repairable-by-BLUFF_ROD is applied later in ItemHandler#modifyComponents
        // (once all items are guaranteed to be registered), overriding the placeholder tag-based repair component
        // .humanoidArmor() sets here (ArmorMaterial only supports tag-based repair now) - not done here in the
        // constructor since ItemHandler.BLUFF_ROD's DeferredHolder is not guaranteed to be bound yet while items
        // are still being constructed/registered.
        // PORTING NOTE (1.21.1 -> 26.1.2): MaterialHandler.GEOMANCER_ARMOR_MATERIAL is now a plain ArmorMaterial
        // (no DeferredHolder wrapper any more - ArmorMaterial isn't registry-backed in this version, see
        // MaterialHandler.java's class javadoc), so no .get() here.
        super(builderIn.humanoidArmor(MaterialHandler.GEOMANCER_ARMOR_MATERIAL, slot));
    }

    private PlayState predicate(AnimationTest<ItemGeomancerArmor> state) {
        return PlayState.STOP;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ItemGeomancerArmor>(controllerName, 0, this::predicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // PORTING NOTE (GeckoLib 4 -> 5): the old "supply a GeoArmorRenderer via IClientItemExtensions#
    // getHumanoidArmorModel" wiring is gone - IClientItemExtensions#getHumanoidArmorModel(ItemStack, LayerType,
    // Model) now returns a plain vanilla net.minecraft.client.model.Model (used by vanilla's HumanoidArmorLayer),
    // which a GeoArmorRenderer is NOT (confirmed: GeoArmorRenderer.tryRenderGeoArmorPiece is documented as
    // "typically only called by an internal mixin" - see PORTING_NOTES.md's GeoArmorRenderer architecture section).
    // GeckoLib 5's own internal armor-layer mixin instead calls `GeoRenderProvider#getGeoArmorRenderer(ItemStack,
    // EquipmentSlot)` (confirmed by reading GeoArmorRenderer.StackForRender#find and RenderUtil#
    // getGeckoLibArmorRenderer in the real GeckoLib 5.5.2 source), which is obtained per-item via this GeoItem's
    // own `createGeoRenderer(Consumer<GeoRenderProvider>)` hook (declared on SingletonGeoAnimatable, default no-op,
    // only ever invoked client-side - see AnimatableInstanceCache's lazy renderProvider supplier, guarded by
    // GeckoLibServices.PLATFORM.isPhysicalClient()). This item has no distinct in-hand/inventory GeoItemRenderer
    // (no RenderGeomancerItem class exists alongside RenderGeomancerArmor, unlike the mask/visage items below), so
    // getGeoItemRenderer() is intentionally left at its default (null - falls back to a normal flat icon).
    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoArmorRenderer<ItemGeomancerArmor, ?> armorRenderer;

            @Override
            public GeoArmorRenderer<?, ?> getGeoArmorRenderer(ItemStack stack, EquipmentSlot equipmentSlot) {
                if (armorRenderer == null) armorRenderer = new RenderGeomancerArmor();
                return armorRenderer;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    // Kept as a trivial no-override implementation solely because MMClient.java (out of this scope) still
    // registers one via RegisterClientExtensionsEvent#registerItem(new ItemGeomancerArmor.ClientExtensions(), ...).
    // All the actual GeckoLib armor-rendering wiring now happens through createGeoRenderer() above instead - see
    // that method's porting note for why IClientItemExtensions#getHumanoidArmorModel is no longer the right hook.
    public static class ClientExtensions implements IClientItemExtensions {
    }
}
