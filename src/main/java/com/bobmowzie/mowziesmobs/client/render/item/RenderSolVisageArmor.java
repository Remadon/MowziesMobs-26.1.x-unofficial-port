package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.armor.SolVisageModel;
import com.bobmowzie.mowziesmobs.client.render.entity.MowzieGeoArmorRenderer;
import com.bobmowzie.mowziesmobs.server.item.ItemSolVisage;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

public class RenderSolVisageArmor extends MowzieGeoArmorRenderer<ItemSolVisage, HumanoidRenderState> {

    public RenderSolVisageArmor() {
        super(new SolVisageModel());
    }
}
