package com.bobmowzie.mowziesmobs.client.model.item;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.item.ItemEarthrendGauntlet;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class ModelEarthrendGauntlet extends GeoModel<ItemEarthrendGauntlet> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/earthrend_gauntlet.png");

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "earthrend_gauntlet");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(ItemEarthrendGauntlet animatable) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "earthrend_gauntlet");
    }
}
