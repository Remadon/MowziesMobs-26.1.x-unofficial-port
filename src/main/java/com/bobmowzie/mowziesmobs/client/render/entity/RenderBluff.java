package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelBluff;
import com.bobmowzie.mowziesmobs.server.entity.bluff.EntityBluff;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

// PORTING NOTE: old getTextureLocation(EntityBluff) override removed - it just duplicated the default
// GeoRendererInternals#getTextureLocation(R renderState) behavior (getGeoModel().getTextureResource(renderState)),
// which GeckoLib 5 now provides for free.
public class RenderBluff extends MowzieGeoEntityRenderer<EntityBluff, LivingEntityRenderState> {
    public RenderBluff(EntityRendererProvider.Context mgr) {
        super(mgr, new ModelBluff());
        shadowRadius = 0.5F;
    }
}
