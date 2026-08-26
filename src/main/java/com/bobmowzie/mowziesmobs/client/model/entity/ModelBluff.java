package com.bobmowzie.mowziesmobs.client.model.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.bluff.EntityBluff;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ModelBluff extends MowzieGeoModel<EntityBluff> {
    public ModelBluff() {
        super();
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return MMCommon.resource("bluff");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return MMCommon.resource("textures/entity/bluff.png");
    }

    @Override
    public Identifier getAnimationResource(EntityBluff object) {
        return MMCommon.resource("bluff");
    }

    // PORTING NOTE: no longer @Override - see MowzieGeoModel's class javadoc. Kept as a plain method (was called
    // from a renderer's setCustomAnimations chain via super() before; that base no-op no longer exists either, so
    // the super() call was removed).
    public void setCustomAnimations(EntityBluff entity, long instanceId, AnimationTest<EntityBluff> animationState) {
        float frame = entity.frame + animationState.renderState().getPartialTick();
        float ticks = entity.tickCount;

        MowzieGeoBone rotation1 = getMowzieBone("rotation1");
        MowzieGeoBone rotation2 = getMowzieBone("rotation2");
        MowzieGeoBone rotation3 = getMowzieBone("rotation3");
        MowzieGeoBone core = getMowzieBone("core");

        if (entity.isAlive()) {
            rotation1.addRotY((frame % 360 / 4f));
            rotation2.addRotY((frame % 360 / 4f));
            rotation3.addRotY((frame % 360 / 4f));
            core.addRotY((frame % 360 / -4f));
            core.addPosY((float) (Math.sin(frame / 5f) *0.8f));
            core.addRotX((float) (Math.sin(frame / 9f) *1f));
        }

        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone root = getMowzieBone("root");

        // PORTING NOTE: EntityModelData/DataTickets.ENTITY_MODEL_DATA no longer exist in GeckoLib 5 - replaced with
        // the closest available tickets (see ModelSculptor for the same substitution and its caveats).
        float headYaw = Mth.wrapDegrees(animationState.getData(DataTickets.ENTITY_YAW) - animationState.getData(DataTickets.ENTITY_BODY_YAW));
        float headPitch = Mth.wrapDegrees(animationState.getData(DataTickets.ENTITY_PITCH));
        head.addRotX(headPitch * (float) Math.PI / 180F);
        root.addRotY(headYaw * (float) Math.PI / 180F);
    }
}
