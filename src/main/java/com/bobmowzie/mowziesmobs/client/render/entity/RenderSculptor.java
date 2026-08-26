package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelSculptor;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoSunblockLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.ItemLayerSculptorStaff;
import com.bobmowzie.mowziesmobs.server.entity.sculptor.EntitySculptor;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3d;

/**
 * PORTING NOTE: `render(...)`/`renderUpdates(...)` (old GeckoLib-4 style bone-position reads after rendering) are
 * replaced with {@link #registerBonePositionListeners}, using {@link RenderPassInfo#addBonePositionListener} for
 * bone WORLD/MODEL positions - see MowzieGeoEntityRenderer.java javadoc and PORTING_NOTES.md. The `modelPos` value
 * the listener provides is the direct, correctly-computed replacement for the old `GeoBone#getModelPosition()`
 * (which no longer exists). `frontClothRot` (a bone ROTATION, not a position) has no listener equivalent - kept as
 * a direct (potentially stale, see PORTING_NOTES.md "MowzieGeoBone is now a WRAPPER") read via `MowzieGeoBone`,
 * same caveat as RenderUmvuthana's rattle-sound rotation read. `getRenderColor` now returns a packed ARGB `int`
 * instead of GeckoLib's old `Color` object.
 */
public class RenderSculptor extends MowzieGeoEntityRenderer<EntitySculptor, LivingEntityRenderState> {
    public final Identifier staff_geo_location = Identifier.fromNamespaceAndPath(MMCommon.MODID, "sculptor_staff");
    public final Identifier staff_tex_location = Identifier.fromNamespaceAndPath(MMCommon.MODID, "textures/item/sculptor_staff.png");

    public RenderSculptor(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelSculptor());
        this.withRenderLayer(new FrozenRenderHandler.GeckoLayerFrozen<>(this, renderManager));
        this.withRenderLayer(new GeckoSunblockLayer<>(this, renderManager));
        this.withRenderLayer(new ItemLayerSculptorStaff(this, "backItem"));
        this.withRenderLayer(new ItemLayerSculptorStaff(this,"itemHandLeft"));
        this.withRenderLayer(new ItemLayerSculptorStaff(this,"itemHandRight"));
        this.shadowRadius = 0.7f;
    }

    @Override
    protected void registerBonePositionListeners(RenderPassInfo<LivingEntityRenderState> renderPassInfo, EntitySculptor entity) {
        renderPassInfo.addBonePositionListener("calfRight", (world, model, local) -> entity.calfRPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("calfLeft", (world, model, local) -> entity.calfLPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("thighRight", (world, model, local) -> entity.thighRPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("thighLeft", (world, model, local) -> entity.thighLPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtEndRight", (world, model, local) -> entity.skirtEndRPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtEndLeft", (world, model, local) -> entity.skirtEndLPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtFrontLocRight", (world, model, local) -> entity.skirtLocFrontRPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtFrontLocLeft", (world, model, local) -> entity.skirtLocFrontLPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtBackLocRight", (world, model, local) -> entity.skirtLocBackRPos = toVector3d(model));
        renderPassInfo.addBonePositionListener("skirtBackLocLeft", (world, model, local) -> entity.skirtLocBackLPos = toVector3d(model));

        MowzieGeoBone disappearControllerBone = getMowzieGeoModel().getMowzieBone("disappearController");
        if (disappearControllerBone != null) entity.disappearController = disappearControllerBone.getPosX();

        MowzieGeoBone frontCloth = getMowzieGeoModel().getMowzieBone("clothFront");
        if (frontCloth != null) entity.frontClothRot = frontCloth.getModelRotationMat();

        this.shadowRadius = 0.7f * (1.0f - entity.disappearController);
    }

    private static Vector3d toVector3d(net.minecraft.world.phys.Vec3 vec) {
        return vec == null ? null : new Vector3d(vec.x(), vec.y(), vec.z());
    }

    @Override
    public int getRenderColor(EntitySculptor animatable, @Nullable Void relatedObject, float partialTick) {
        return ARGB.colorFromFloat(1.0f - animatable.disappearController, 1, 1, 1);
    }

    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        // NOTE: `disappearController` is entity-instance state; reading it directly off the live entity here would
        // require capturing it into the render state (it already is, via `addRenderData` inherited behavior is not
        // used here - `registerBonePositionListeners` runs early enough each pass to keep `this.shadowRadius`/entity
        // fields updated, but `getRenderType` only has the renderState). Kept simple: always use entityTranslucent
        // when the model's local `disappearController` bone value (captured onto the entity each pass) is nonzero.
        MowzieGeoBone disappearControllerBone = getMowzieGeoModel().isInitialized() ? getMowzieGeoModel().getMowzieBone("disappearController") : null;
        if (disappearControllerBone != null && disappearControllerBone.getPosX() > 0) {
            return RenderTypes.entityTranslucent(texture);
        }
        else {
            return super.getRenderType(renderState, texture);
        }
    }
}
