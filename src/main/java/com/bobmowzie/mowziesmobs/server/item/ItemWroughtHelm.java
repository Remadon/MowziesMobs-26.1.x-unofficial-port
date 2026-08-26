package com.bobmowzie.mowziesmobs.server.item;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.LayerHandler;
import com.bobmowzie.mowziesmobs.client.model.armor.WroughtHelmModel;
import com.bobmowzie.mowziesmobs.server.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ItemWroughtHelm extends Item {
    public ItemWroughtHelm(Item.Properties properties) {
        // ArmorItem was removed upstream - repair-while-breakable is now handled via a conditional
        // DataComponents.REPAIRABLE removal in ItemHandler#modifyComponents instead of overriding
        // isValidRepairItem(ItemStack, ItemStack), which no longer exists. .humanoidArmor() already grants
        // enchantability from the material's enchantment value, matching the old "isEnchantable() -> true" override.
        super(properties.humanoidArmor(MaterialHandler.ARMOR_WROUGHT_HELM, ArmorType.HELMET));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, context, display, tooltip, flagIn);
        tooltip.accept(Component.translatable(getDescriptionId() + ".text.0").setStyle(ItemHandler.TOOLTIP_STYLE));
    }

    public static final class ArmorRender implements IClientItemExtensions {
        public static final ArmorRender INSTANCE = new ArmorRender();
        private static HumanoidModel<?> MODEL;

        @Override
        public Model getHumanoidArmorModel(ItemStack itemStack, EquipmentClientInfo.LayerType layerType, Model original) {
            if (MODEL == null) {
                EntityModelSet models = Minecraft.getInstance().getEntityModels();
                ModelPart root = models.bakeLayer(LayerHandler.WROUGHT_HELM_LAYER);
                MODEL = new WroughtHelmModel<>(root);
            }
            return MODEL;
        }

        @Override
        public @Nullable Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier _default) {
            return Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/wrought_helmet.png");
        }
    }
}
