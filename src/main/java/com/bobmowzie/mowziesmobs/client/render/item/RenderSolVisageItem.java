package com.bobmowzie.mowziesmobs.client.render.item;

import com.bobmowzie.mowziesmobs.client.model.armor.SolVisageModel;
import com.bobmowzie.mowziesmobs.server.item.ItemSolVisage;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public class RenderSolVisageItem extends GeoItemRenderer<ItemSolVisage> {

    public RenderSolVisageItem() {
        super(new SolVisageModel());
    }

    @Override
    public @Nullable RenderType getRenderType(GeoRenderState renderState, Identifier texture) {
        return RenderTypes.armorCutoutNoCull(texture);
    }
}
