package com.bobmowzie.mowziesmobs.client.model;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.armor.WroughtHelmModel;
import com.bobmowzie.mowziesmobs.client.render.block.GongRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class LayerHandler {
    public static final ModelLayerLocation WROUGHT_HELM_LAYER = register("wrought_helm", "main");
    public static final ModelLayerLocation GONG_LAYER = register("gong", "main");

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WROUGHT_HELM_LAYER, WroughtHelmModel::createArmorLayer);
        event.registerLayerDefinition(GONG_LAYER, GongRenderer::createBodyLayer);
    }

    private static ModelLayerLocation register(String model, String layer) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(MMCommon.MODID, model), layer);
    }
}