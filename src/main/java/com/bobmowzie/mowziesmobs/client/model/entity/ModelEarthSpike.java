package com.bobmowzie.mowziesmobs.client.model.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.effects.geomancy.EntityEarthSpike;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;

public class ModelEarthSpike extends MowzieGeoModel<EntityEarthSpike> {
    private static final Identifier MODEL = MMCommon.resource("earth_spike");
    private static final Identifier TEXTURE = MMCommon.resource("textures/entity/umvuthi.png");
    private static final Identifier ANIMATION = MMCommon.resource("earth_spike");

    public ModelEarthSpike() {
        super();
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(EntityEarthSpike object) {
        return ANIMATION;
    }
}