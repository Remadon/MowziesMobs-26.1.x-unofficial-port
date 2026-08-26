package com.bobmowzie.mowziesmobs.client.model.armor;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.item.ItemGeomancerArmor;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoArmorRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

// PORTING NOTE: this now extends MowzieGeoModel (was GeoModel directly) so that getMowzieBone(...) - which returns a
// mutable-capable bone wrapper - is available. In GeckoLib 4.x, plain GeoBone was itself directly mutable, so this
// file used to call getBone(name).orElse(null) and mutate the plain GeoBone; GeckoLib 5's GeoBone is immutable, so
// the mutable wrapper is required now - see MowzieGeoBone's class javadoc.
public class ModelGeomancerArmor extends MowzieGeoModel<ItemGeomancerArmor> {
    // PORTING NOTE: DataTickets.ENTITY (the armor wearer) no longer exists in GeckoLib 5. The wearer is available as
    // the "related object" GeckoLib's GeoArmorRenderer passes through the render pass (a GeoArmorRenderer.RenderData
    // record containing the LivingEntity), captured here via addAdditionalStateData - same pattern used elsewhere
    // for data that used to come from a removed DataTicket. See GeoArmorRenderer.RenderData in the real 5.5.2 source.
    private static final DataTicket<Entity> WEARER = DataTickets.create("mowziesmobs_geomancer_armor_wearer", Entity.class);

    @Override
    public void addAdditionalStateData(ItemGeomancerArmor animatable, Object relatedObject, GeoRenderState renderState) {
        if (relatedObject instanceof GeoArmorRenderer.RenderData renderData) {
            renderState.addGeckolibData(WEARER, renderData.entity());
        }
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "geomancer_armor");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/geomancer_armor.png");
    }

    @Override
    public Identifier getAnimationResource(ItemGeomancerArmor animatable) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "empty");
    }

    // PORTING NOTE: no longer @Override - GeoModel has no getRenderType method anymore (moved to the renderer) -
    // see ModelRockSling for the same situation.
    public RenderType getRenderType(ItemGeomancerArmor animatable, Identifier texture) {
        return RenderTypes.entityCutout(texture);
    }

    // PORTING NOTE: no longer @Override - see MowzieGeoModel's class javadoc. super.setCustomAnimations(...) call
    // removed since GeoModel no longer has any such method to call.
    public void setCustomAnimations(ItemGeomancerArmor animatable, long instanceId, AnimationTest<ItemGeomancerArmor> animationState) {
        Entity entity = animationState.getData(WEARER);
        if (entity instanceof Player playerEntity) {
            MowzieGeoBone cloth = getMowzieBone("cloth");
            MowzieGeoBone backCloth = getMowzieBone("backCloth");
            float partialTick = animationState.renderState().getPartialTick();
            if (cloth != null) {
                boolean flag = playerEntity.getFallFlyingTicks() > 4;
                float f = 1.0F;
                if (flag) {
                    f = (float)playerEntity.getDeltaMovement().lengthSqr();
                    f = f / 0.2F;
                    f = f * f * f;
                }

                if (f < 1.0F) {
                    f = 1.0F;
                }

                boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());
                float f8_limbSwingAmount = 0.0F;
                float f5_limbSwing = 0.0F;
                if (!shouldSit && entity.isAlive()) {
                    f8_limbSwingAmount = playerEntity.walkAnimation.speed(partialTick);
                    f5_limbSwing = playerEntity.walkAnimation.position(partialTick);
                    if (playerEntity.isBaby()) {
                        f5_limbSwing *= 3.0F;
                    }

                    if (f8_limbSwingAmount > 1.0F) {
                        f8_limbSwingAmount = 1.0F;
                    }
                }
                cloth.setRotX(Math.abs(Mth.cos(f5_limbSwing * 0.6662F + (float) Math.PI) * 2.0F * f8_limbSwingAmount * 0.5F / f) + f8_limbSwingAmount * 0.5f);
            }

            // PORTING NOTE: the old raw xCloak/yCloak/zCloak/oBob/bob/walkDist(O) fields moved off Player entirely -
            // they now live on the client-only net.minecraft.client.entity.ClientAvatarState (obtained via
            // ClientAvatarEntity#avatarState(), implemented by AbstractClientPlayer), with the partial-tick lerp
            // already baked into its getInterpolatedXxx(partialTick) accessors. See PORTING_NOTES.md.
            if (backCloth != null && playerEntity instanceof ClientAvatarEntity avatarEntity) {
                ClientAvatarState avatarState = avatarEntity.avatarState();
                double d0 = avatarState.getInterpolatedCloakX(partialTick) - Mth.lerp((double)partialTick, playerEntity.xo, playerEntity.getX());
                double d1 = avatarState.getInterpolatedCloakY(partialTick) - Mth.lerp((double)partialTick, playerEntity.yo, playerEntity.getY());
                double d2 = avatarState.getInterpolatedCloakZ(partialTick) - Mth.lerp((double)partialTick, playerEntity.zo, playerEntity.getZ());
                float f = Mth.rotLerp(partialTick, playerEntity.yBodyRotO, playerEntity.yBodyRot);
                double d3 = (double)Mth.sin(f * ((float)Math.PI / 180F));
                double d4 = (double)(-Mth.cos(f * ((float)Math.PI / 180F)));
                float f1 = (float)d1 * 10.0F;
                f1 = Mth.clamp(f1, -6.0F, 32.0F);
                float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
                f2 = Mth.clamp(f2, 0.0F, 150.0F);
                float f3 = (float)(d0 * d4 - d2 * d3) * 100.0F;
                f3 = Mth.clamp(f3, -20.0F, 20.0F);
                if (f2 < 0.0F) {
                    f2 = 0.0F;
                }

                float f4 = avatarState.getInterpolatedBob(partialTick);
                f1 += Mth.sin(avatarState.getInterpolatedWalkDistance(partialTick) * 6.0F) * 32.0F * f4;
                if (playerEntity.isCrouching()) {
                    f1 += 25.0F;
                }

                backCloth.setRotX(0 - (float) Math.toRadians(6.0F + f2 / 2.0F + f1));
//                backCloth.setRotZ((float) Math.toRadians(f3 / 2.0F));
//                backCloth.setRotY((float) Math.toRadians(180.0F - f3 / 2.0F));
            }
        }
    }
}
