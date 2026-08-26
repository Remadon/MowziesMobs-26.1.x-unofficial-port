package com.bobmowzie.mowziesmobs.client.model.armor;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.render.entity.RenderUmvuthi;
import com.bobmowzie.mowziesmobs.server.item.ItemSolVisage;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class SolVisageModel extends GeoModel<ItemSolVisage> {

	@Override
	public Identifier getModelResource(GeoRenderState renderState) {
		return Identifier.fromNamespaceAndPath(MMCommon.MODID, "sol_visage");
	}

	@Override
	public Identifier getTextureResource(GeoRenderState renderState) {
		return RenderUmvuthi.TEXTURE;
	}

	@Override
	public Identifier getAnimationResource(ItemSolVisage animatable) {
		return Identifier.fromNamespaceAndPath(MMCommon.MODID, "sol_visage");
	}

	// PORTING NOTE: no longer @Override - see ModelRockSling for why (GeoModel#getRenderType removed in GeckoLib 5).
	public RenderType getRenderType(ItemSolVisage animatable, Identifier texture) {
		return RenderTypes.entityCutout(texture);
	}
}
