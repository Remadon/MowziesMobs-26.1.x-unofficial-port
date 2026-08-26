package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.armor.UmvuthanaMaskModel;
import com.bobmowzie.mowziesmobs.client.render.entity.MowzieGeoArmorRenderer;
import com.bobmowzie.mowziesmobs.server.item.ItemUmvuthanaMask;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class RenderUmvuthanaMaskArmor extends MowzieGeoArmorRenderer<ItemUmvuthanaMask, HumanoidRenderState> {

    public RenderUmvuthanaMaskArmor() {
        super(new UmvuthanaMaskModel());
    }
}
