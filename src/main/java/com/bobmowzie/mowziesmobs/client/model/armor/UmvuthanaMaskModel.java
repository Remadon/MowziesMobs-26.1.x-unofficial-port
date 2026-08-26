package com.bobmowzie.mowziesmobs.client.model.armor;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.MaskType;
import com.bobmowzie.mowziesmobs.server.item.ItemUmvuthanaMask;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class UmvuthanaMaskModel extends GeoModel<ItemUmvuthanaMask> {
    // PORTING NOTE: getModelResource/getTextureResource(GeoRenderState) no longer receive the live animatable, but
    // this single shared model instance is used for every MaskType variant of ItemUmvuthanaMask (a separate Item
    // instance is registered per mask type), so the mask type needs to be captured up front - see
    // ModelElokosa/ModelUmvuthana for the same pattern.
    private static final DataTicket<MaskType> MASK_TYPE = DataTickets.create("mowziesmobs_umvuthana_mask_item_type", MaskType.class);

    @Override
    public void addAdditionalStateData(ItemUmvuthanaMask animatable, Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(MASK_TYPE, animatable.getMaskType());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "mask_" + renderState.getGeckolibData(MASK_TYPE).name);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/umvuthana_mask_" + renderState.getGeckolibData(MASK_TYPE).name + ".png");
    }

    @Override
    public Identifier getAnimationResource(ItemUmvuthanaMask animatable) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "umvuthana_mask");
    }

    // PORTING NOTE: no longer @Override - see ModelRockSling for why (GeoModel#getRenderType removed in GeckoLib 5).
    public RenderType getRenderType(ItemUmvuthanaMask animatable, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }
}
