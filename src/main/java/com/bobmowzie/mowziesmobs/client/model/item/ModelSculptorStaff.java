package com.bobmowzie.mowziesmobs.client.model.item;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.item.ItemSculptorStaff;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class ModelSculptorStaff extends GeoModel<ItemSculptorStaff> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "sculptor_staff");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/sculptor_staff.png");
    }

    @Override
    public Identifier getAnimationResource(ItemSculptorStaff animatable) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "sculptor");
    }
}
