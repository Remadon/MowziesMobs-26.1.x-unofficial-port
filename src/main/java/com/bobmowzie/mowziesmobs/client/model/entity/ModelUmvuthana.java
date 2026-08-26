package com.bobmowzie.mowziesmobs.client.model.entity;

import com.bobmowzie.mowziesmobs.MMCommon;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.MaskType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderState;

public class ModelUmvuthana extends MowzieGeoModel<EntityUmvuthana> {
    // PORTING NOTE: getTextureResource(GeoRenderState) no longer receives the live animatable (see below), so the
    // mask type needed to pick a texture is captured into the render state up front via addAdditionalStateData
    // (the intended GeckoLib 5 mechanism for this) using this custom DataTicket, and read back in getTextureResource.
    private static final DataTicket<MaskType> MASK_TYPE = DataTickets.create("mowziesmobs_umvuthana_mask_type", MaskType.class);

    public ModelUmvuthana() {
        super();
    }

    @Override
    public void addAdditionalStateData(EntityUmvuthana animatable, Object relatedObject, GeoRenderState renderState) {
        renderState.addGeckolibData(MASK_TYPE, animatable.getMaskType());
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "umvuthana");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        MaskType maskType = renderState.getGeckolibData(MASK_TYPE);
        boolean isElite = maskType == MaskType.FAITH || maskType == MaskType.FURY;
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, isElite ? "textures/entity/umvuthana_elite.png" : "textures/entity/umvuthana.png");
    }

    @Override
    public Identifier getAnimationResource(EntityUmvuthana object) {
        return Identifier.fromNamespaceAndPath(MMCommon.MODID, "umvuthana");
    }

    // PORTING NOTE: no longer @Override - see MowzieGeoModel's class javadoc.
    public void setCustomAnimations(EntityUmvuthana entity, long instanceId, AnimationTest<EntityUmvuthana> animationState) {
        boolean isRaptor = entity.getMaskType() == MaskType.FURY;
        boolean isElite = entity.getMaskType() == MaskType.FAITH || isRaptor;
        getMowzieBone("crestRight").setHidden(!isElite);
        getMowzieBone("crestLeft").setHidden(!isElite);
        getMowzieBone("crest1").setHidden(!isElite);
        getMowzieBone("leftIndexTalon").setHidden(!isRaptor);
        getMowzieBone("leftIndexClaw").setHidden(isRaptor);
        getMowzieBone("rightIndexTalon").setHidden(!isRaptor);
        getMowzieBone("rightIndexClaw").setHidden(isRaptor);
        MowzieGeoBone root = getMowzieBone("root");
        if (isElite) {
            root.multiplyScale(0.93f, 0.93f, 0.93f);
        }
        else {
            root.multiplyScale(0.83f, 0.83f, 0.83f);
        }

        MowzieGeoBone mask = getMowzieBone("mask");
        MowzieGeoBone hips = getMowzieBone("hips");
        if (entity.getActiveAbilityType() != EntityUmvuthana.TELEPORT_ABILITY) {
            mask.setScale(1.0f / (float) hips.getScale().x, 1.0f / (float) hips.getScale().y, 1.0f / (float) hips.getScale().z);
        }

        // REMOVED (see class javadoc note below the method... actually see PORTING follow-up): head/neck
        // look-at-target rotation used to be applied here via head.addRotX/addRotY + neck.addRotX/addRotY, but
        // "head" and "neck" already have full keyframe tracks in idle_neutral/walk_neutral/idle_aggressive/
        // walk_aggressive (umvuthana.animation.json) - adding more rotation on top of an already-complete
        // keyframe-driven pose every frame is exactly the kind of double-animation that was causing the reported
        // wild tilting during movement. Dropped rather than re-enabled now that setCustomAnimations is actually
        // wired up (previously dead code, so this conflict was never visible before).

        MowzieGeoBone maskHand = getMowzieBone("maskHand");
        MowzieGeoBone maskTwitcher = getMowzieBone("maskTwitcher");
        float maskPlaceSwitch = getControllerValue("maskPlacementSwitchController");
        if (maskPlaceSwitch == 1.0) {
            maskTwitcher.setHidden(true);
            maskHand.setHidden(false);
        }
        else {
            maskTwitcher.setHidden(false);
            maskHand.setHidden(true);
        }

        // NOTE (corrected from an earlier, wrong assumption): the idle_neutral/walk_neutral/etc. keyframe tracks on
        // hips/thighs/shins/etc. are each a single STATIC pose (a fixed rotation/position vector, no per-frame
        // interpolation or Molang time-based motion) - they establish a base walking stance only. The actual
        // cyclic swing/step motion has always come from this procedural code layered on top via addRotX/addPosY.
        // Removing it (as a previous pass here did) leaves legs frozen in that static stance while the entity
        // still translates through the world - i.e. "sliding" rather than stepping. Restored.
        float animSpeed = 1.4f;
        // PORTING NOTE: AnimationState#getLimbSwing()/getLimbSwingAmount() no longer exist in GeckoLib 5 (no
        // DataTicket equivalent either) - read directly from the vanilla LivingEntityRenderState's
        // walkAnimationPos/walkAnimationSpeed fields instead (the modern vanilla name for the same data).
        float limbSwing = 0f;
        float limbSwingAmount = 0f;
        if (animationState.renderState() instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState livingEntityRenderState) {
            limbSwing = livingEntityRenderState.walkAnimationPos;
            limbSwingAmount = livingEntityRenderState.walkAnimationSpeed;
        }

        double forward = Mth.lerp(animationState.renderState().getPartialTick(), entity.prevMoveDirForward, entity.moveDirForward);
        double backward = Mth.lerp(animationState.renderState().getPartialTick(), entity.prevMoveDirBackward, entity.moveDirBackward);
        double left = Mth.lerp(animationState.renderState().getPartialTick(), entity.prevMoveDirLeft, entity.moveDirLeft);
        double right = Mth.lerp(animationState.renderState().getPartialTick(), entity.prevMoveDirRight, entity.moveDirRight);
        limbSwingAmount *= 2;
        limbSwingAmount = Math.min(0.7f, limbSwingAmount);
        float locomotionAnimController = getControllerValue("locomotionAnimController");
        float runAnimBlend = getControllerValue("walkRunSwitchController");
        float walkAnim = 1.0f - runAnimBlend;
        walkForwardAnim((float) (forward * locomotionAnimController * walkAnim), limbSwing, limbSwingAmount, animSpeed);
        walkBackwardAnim((float) (backward * locomotionAnimController * walkAnim), limbSwing, limbSwingAmount, animSpeed);
        walkLeftAnim((float) (left * locomotionAnimController * walkAnim), limbSwing, limbSwingAmount, animSpeed);
        walkRightAnim((float) (right * locomotionAnimController * walkAnim), limbSwing, limbSwingAmount, animSpeed);

        runAnim(locomotionAnimController * runAnimBlend, limbSwing, limbSwingAmount, animSpeed);
    }

    private void runAnim(float blend, float limbSwing, float limbSwingAmount, float speed) {
        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone neck = getMowzieBone("neck");
        MowzieGeoBone hips = getMowzieBone("hips");
        MowzieGeoBone stomach = getMowzieBone("stomach");
        MowzieGeoBone chest = getMowzieBone("chest");
        MowzieGeoBone leftThigh = getMowzieBone("leftThigh");
        MowzieGeoBone leftShin = getMowzieBone("leftShin");
        MowzieGeoBone leftAnkle = getMowzieBone("leftAnkle");
        MowzieGeoBone leftFoot = getMowzieBone("leftFoot");
        MowzieGeoBone leftToesBack = getMowzieBone("leftToesBack");
        MowzieGeoBone rightThigh = getMowzieBone("rightThigh");
        MowzieGeoBone rightShin = getMowzieBone("rightShin");
        MowzieGeoBone rightAnkle = getMowzieBone("rightAnkle");
        MowzieGeoBone rightFoot = getMowzieBone("rightFoot");
        MowzieGeoBone rightToesBack = getMowzieBone("rightToesBack");
        MowzieGeoBone leftArm = getMowzieBone("leftArm");
        MowzieGeoBone leftForeArm = getMowzieBone("leftForeArm");
        MowzieGeoBone leftHand = getMowzieBone("leftHand");
        MowzieGeoBone rightArm = getMowzieBone("rightArm");
        MowzieGeoBone rightForeArm = getMowzieBone("rightForeArm");
        MowzieGeoBone rightHand = getMowzieBone("rightHand");

        float globalHeight = 1.5f;
        float globalDegree = 1.7f;
        speed *= 0.8;

        hips.addPosY(blend * (float) (Math.cos(limbSwing * speed - 1.7) * 2f * globalHeight + 4 * globalHeight) * limbSwingAmount);
        hips.addRotX(blend * -0.4f * limbSwingAmount * globalHeight);
        hips.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        chest.addRotY(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.2f * globalHeight) * limbSwingAmount);
        stomach.addRotX(blend * -(float) (Math.cos(limbSwing * speed + 1.4 - 1.7) * 0.025 * globalHeight) * limbSwingAmount);
        neck.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        neck.addRotX(blend * -(float) (Math.cos(limbSwing * speed - 1.5) * 0.25 * globalHeight - 0.2 * globalHeight) * limbSwingAmount);
        head.addRotX(blend * (float) (Math.cos(limbSwing * speed + 0.175 - 1.7) * 0.25 * globalHeight + 0.2 * globalHeight) * limbSwingAmount);

        leftThigh.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree) * limbSwingAmount);
        leftThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree - 0.2f) * limbSwingAmount);
        leftShin.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.50) * -0.7f * globalDegree) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.50) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 3.5) * -1f * globalDegree - 1.1f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree + 1.8f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightThigh.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree) * limbSwingAmount);
        rightThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree + 0.2f) * limbSwingAmount);
        rightShin.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 2.50) * -0.7f * globalDegree) * limbSwingAmount);
        rightAnkle.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 2.50) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 3.5) * -1f * globalDegree - 1.1f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree + 1.8f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (-Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        leftArm.addRotY(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.09 * globalHeight) * limbSwingAmount);
        leftArm.addRotZ(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.09 * globalHeight) * limbSwingAmount);
        leftForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.05 * globalHeight) * limbSwingAmount);

        rightArm.addRotY(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.09 * globalHeight) * limbSwingAmount);
        rightArm.addRotZ(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.09 * globalHeight) * limbSwingAmount);
        rightForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.05 * globalHeight) * limbSwingAmount);
    }

    private void walkForwardAnim(float blend, float limbSwing, float limbSwingAmount, float speed) {
        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone neck = getMowzieBone("neck");
        MowzieGeoBone hips = getMowzieBone("hips");
        MowzieGeoBone stomach = getMowzieBone("stomach");
        MowzieGeoBone chest = getMowzieBone("chest");
        MowzieGeoBone leftThigh = getMowzieBone("leftThigh");
        MowzieGeoBone leftShin = getMowzieBone("leftShin");
        MowzieGeoBone leftAnkle = getMowzieBone("leftAnkle");
        MowzieGeoBone leftFoot = getMowzieBone("leftFoot");
        MowzieGeoBone leftToesBack = getMowzieBone("leftToesBack");
        MowzieGeoBone rightThigh = getMowzieBone("rightThigh");
        MowzieGeoBone rightShin = getMowzieBone("rightShin");
        MowzieGeoBone rightAnkle = getMowzieBone("rightAnkle");
        MowzieGeoBone rightFoot = getMowzieBone("rightFoot");
        MowzieGeoBone rightToesBack = getMowzieBone("rightToesBack");
        MowzieGeoBone leftArm = getMowzieBone("leftArm");
        MowzieGeoBone leftForeArm = getMowzieBone("leftForeArm");
        MowzieGeoBone leftHand = getMowzieBone("leftHand");
        MowzieGeoBone rightArm = getMowzieBone("rightArm");
        MowzieGeoBone rightForeArm = getMowzieBone("rightForeArm");
        MowzieGeoBone rightHand = getMowzieBone("rightHand");

        float globalHeight = 1.5f;
        float globalDegree = 1.5f;

        hips.addPosY(blend * (float) (Math.cos(limbSwing * speed) * 1.5f * globalHeight) * limbSwingAmount);
        hips.addRotX(blend * -0.18f * limbSwingAmount * globalHeight);
        hips.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        chest.addRotY(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.2f * globalHeight) * limbSwingAmount);
        stomach.addRotX(blend * -(float) (Math.cos(limbSwing * speed + 1.4) * 0.025 * globalHeight) * limbSwingAmount);
        neck.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        neck.addRotX(blend * -(float) (Math.cos(limbSwing * speed) * 0.175 * globalHeight) * limbSwingAmount);
        head.addRotX(blend * (float) (Math.cos(limbSwing * speed + 0.175) * 0.175 * globalHeight + 0.18 * globalHeight) * limbSwingAmount);

        leftThigh.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree) * limbSwingAmount);
        leftThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree - 0.15f) * limbSwingAmount);
        leftShin.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree - 0.4f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree + 1.4f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightThigh.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree) * limbSwingAmount);
        rightThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree + 0.15f) * limbSwingAmount);
        rightShin.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        rightAnkle.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree - 0.1f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree + 0.4f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree - 1.4f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        leftArm.addRotY(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftArm.addRotZ(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);

        rightArm.addRotY(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightArm.addRotZ(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);
    }

    private void walkBackwardAnim(float blend, float limbSwing, float limbSwingAmount, float speed) {
        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone neck = getMowzieBone("neck");
        MowzieGeoBone hips = getMowzieBone("hips");
        MowzieGeoBone stomach = getMowzieBone("stomach");
        MowzieGeoBone chest = getMowzieBone("chest");
        MowzieGeoBone leftThigh = getMowzieBone("leftThigh");
        MowzieGeoBone leftShin = getMowzieBone("leftShin");
        MowzieGeoBone leftAnkle = getMowzieBone("leftAnkle");
        MowzieGeoBone leftFoot = getMowzieBone("leftFoot");
        MowzieGeoBone leftToesBack = getMowzieBone("leftToesBack");
        MowzieGeoBone rightThigh = getMowzieBone("rightThigh");
        MowzieGeoBone rightShin = getMowzieBone("rightShin");
        MowzieGeoBone rightAnkle = getMowzieBone("rightAnkle");
        MowzieGeoBone rightFoot = getMowzieBone("rightFoot");
        MowzieGeoBone rightToesBack = getMowzieBone("rightToesBack");
        MowzieGeoBone leftArm = getMowzieBone("leftArm");
        MowzieGeoBone leftForeArm = getMowzieBone("leftForeArm");
        MowzieGeoBone leftHand = getMowzieBone("leftHand");
        MowzieGeoBone rightArm = getMowzieBone("rightArm");
        MowzieGeoBone rightForeArm = getMowzieBone("rightForeArm");
        MowzieGeoBone rightHand = getMowzieBone("rightHand");

        float globalHeight = 1.5f;
        float globalDegree = 1.5f;

        hips.addPosY(blend * (float) (Math.cos(limbSwing * speed) * 1.5f * globalHeight) * limbSwingAmount);
        hips.addRotX(blend * 0.18f * limbSwingAmount * globalHeight);
        hips.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * 0.1f * globalHeight) * limbSwingAmount);
        chest.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.2f * globalHeight) * limbSwingAmount);
        stomach.addRotX(blend * -(float) (Math.cos(limbSwing * speed + 1.4) * 0.025 * globalHeight) * limbSwingAmount);
        neck.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * 0.1f * globalHeight) * limbSwingAmount);
        neck.addRotX(blend * -(float) (Math.cos(limbSwing * speed) * 0.175 * globalHeight) * limbSwingAmount);
        head.addRotX(blend * (float) (Math.cos(limbSwing * speed + 0.175) * 0.175 * globalHeight - 0.18 * globalHeight) * limbSwingAmount);

        leftThigh.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 1.5) * 0.55f * globalDegree - 0.3 * globalDegree) * limbSwingAmount);
        leftThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 1.5) * 0.1f * globalDegree - 0.15f) * limbSwingAmount);
        leftShin.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 2.40) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 2.5) * -1.3f * globalDegree - 0.4f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 3.1) * 1.6f * globalDegree + 1.4f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightThigh.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 - 1.5) * 0.55f * globalDegree + 0.3 * globalDegree) * limbSwingAmount);
        rightThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 - 1.5) * 0.1f * globalDegree + 0.15f) * limbSwingAmount);
        rightShin.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 - 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        rightAnkle.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 - 2.40) * 1.1f * globalDegree - 0.1f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 - 2.5) * -1.3f * globalDegree + 0.4f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 - 3.1) * 1.6f * globalDegree - 1.4f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        leftArm.addRotY(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftArm.addRotZ(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);

        rightArm.addRotY(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightArm.addRotZ(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);
    }

    private void walkLeftAnim(float blend, float limbSwing, float limbSwingAmount, float speed) {
        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone neck = getMowzieBone("neck");
        MowzieGeoBone hips = getMowzieBone("hips");
        MowzieGeoBone stomach = getMowzieBone("stomach");
        MowzieGeoBone chest = getMowzieBone("chest");
        MowzieGeoBone leftThigh = getMowzieBone("leftThigh");
        MowzieGeoBone leftShin = getMowzieBone("leftShin");
        MowzieGeoBone leftAnkle = getMowzieBone("leftAnkle");
        MowzieGeoBone leftFoot = getMowzieBone("leftFoot");
        MowzieGeoBone leftToesBack = getMowzieBone("leftToesBack");
        MowzieGeoBone rightThigh = getMowzieBone("rightThigh");
        MowzieGeoBone rightShin = getMowzieBone("rightShin");
        MowzieGeoBone rightAnkle = getMowzieBone("rightAnkle");
        MowzieGeoBone rightFoot = getMowzieBone("rightFoot");
        MowzieGeoBone rightToesBack = getMowzieBone("rightToesBack");
        MowzieGeoBone leftArm = getMowzieBone("leftArm");
        MowzieGeoBone leftForeArm = getMowzieBone("leftForeArm");
        MowzieGeoBone leftHand = getMowzieBone("leftHand");
        MowzieGeoBone rightArm = getMowzieBone("rightArm");
        MowzieGeoBone rightForeArm = getMowzieBone("rightForeArm");
        MowzieGeoBone rightHand = getMowzieBone("rightHand");

        float globalHeight = 1.5f;
        float globalDegree = 1.5f;

        hips.addPosY(blend * (float) (Math.cos(limbSwing * speed) * 1.5f * globalHeight) * limbSwingAmount);
        hips.addRotX(blend * -0.1f * limbSwingAmount * globalHeight);
        hips.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        hips.addRotZ(blend * 0.08f * limbSwingAmount * globalHeight);
        chest.addRotY(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.2f * globalHeight) * limbSwingAmount);
        stomach.addRotX(blend * -(float) (Math.cos(limbSwing * speed + 1.4) * 0.025 * globalHeight) * limbSwingAmount);
        stomach.addRotZ(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 1.4) * 0.02 * globalHeight) * limbSwingAmount);
        stomach.addRotZ(blend * -(float) (Math.cos(limbSwing * speed - 0.5) * 0.02 * globalHeight) * limbSwingAmount);
        neck.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        neck.addRotX(blend * -(float) (Math.cos(limbSwing * speed) * 0.175 * globalHeight) * limbSwingAmount);
        head.addRotX(blend * (float) (Math.cos(limbSwing * speed + 0.175) * 0.175 * globalHeight + 0.1 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.4) * 0.02 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * (float) (Math.cos(limbSwing * speed - 0.5) * 0.02 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * -0.03f * limbSwingAmount * globalHeight);

        leftThigh.addRotX(blend * -0.05f * limbSwingAmount * globalHeight);
        leftThigh.addRotZ(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree + 0.05 * globalDegree) * limbSwingAmount);
        leftThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree - 0.15) * limbSwingAmount);
        leftShin.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree - 0.6f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftFoot.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -0.4f * globalDegree) * limbSwingAmount);
        leftFoot.addRotY(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree + 1.4f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightThigh.addRotX(blend * 0.05f * limbSwingAmount * globalHeight);
        rightThigh.addRotZ(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree - 0.05 * globalDegree) * limbSwingAmount);
        rightThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree + 0.15) * limbSwingAmount);
        rightShin.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        rightAnkle.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree - 0.1f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree + 0.6f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightFoot.addRotY(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -0.4f * globalDegree) * limbSwingAmount);
        rightFoot.addRotY(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree - 1.4f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        leftShin.addRotX(blend * -(float) (Math.pow(Math.cos(limbSwing * 0.25 * speed - 0.6), 12) * 0.6f * globalHeight) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.pow(Math.cos(limbSwing * 0.25 * speed - 0.6), 12) * 0.6f * globalHeight) * limbSwingAmount);

        leftArm.addRotY(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftArm.addRotZ(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);

        rightArm.addRotY(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightArm.addRotZ(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);
    }

    private void walkRightAnim(float blend, float limbSwing, float limbSwingAmount, float speed) {
        MowzieGeoBone head = getMowzieBone("head");
        MowzieGeoBone neck = getMowzieBone("neck");
        MowzieGeoBone hips = getMowzieBone("hips");
        MowzieGeoBone stomach = getMowzieBone("stomach");
        MowzieGeoBone chest = getMowzieBone("chest");
        MowzieGeoBone leftThigh = getMowzieBone("leftThigh");
        MowzieGeoBone leftShin = getMowzieBone("leftShin");
        MowzieGeoBone leftAnkle = getMowzieBone("leftAnkle");
        MowzieGeoBone leftFoot = getMowzieBone("leftFoot");
        MowzieGeoBone leftToesBack = getMowzieBone("leftToesBack");
        MowzieGeoBone rightThigh = getMowzieBone("rightThigh");
        MowzieGeoBone rightShin = getMowzieBone("rightShin");
        MowzieGeoBone rightAnkle = getMowzieBone("rightAnkle");
        MowzieGeoBone rightFoot = getMowzieBone("rightFoot");
        MowzieGeoBone rightToesBack = getMowzieBone("rightToesBack");
        MowzieGeoBone leftArm = getMowzieBone("leftArm");
        MowzieGeoBone leftForeArm = getMowzieBone("leftForeArm");
        MowzieGeoBone leftHand = getMowzieBone("leftHand");
        MowzieGeoBone rightArm = getMowzieBone("rightArm");
        MowzieGeoBone rightForeArm = getMowzieBone("rightForeArm");
        MowzieGeoBone rightHand = getMowzieBone("rightHand");

        float globalHeight = 1.5f;
        float globalDegree = 1.5f;

        hips.addPosY(blend * (float) (Math.cos(limbSwing * speed) * 1.5f * globalHeight) * limbSwingAmount);
        hips.addRotX(blend * -0.1f * limbSwingAmount * globalHeight);
        hips.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        hips.addRotZ(blend * -0.08f * limbSwingAmount * globalHeight);
        chest.addRotY(blend * (float) (-Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.2f * globalHeight) * limbSwingAmount);
        stomach.addRotX(blend * -(float) (Math.cos(limbSwing * speed + 1.4) * 0.025 * globalHeight) * limbSwingAmount);
        stomach.addRotZ(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.4) * 0.02 * globalHeight) * limbSwingAmount);
        stomach.addRotZ(blend * (float) (Math.cos(limbSwing * speed - 0.5) * 0.02 * globalHeight) * limbSwingAmount);
        neck.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.0) * -0.1f * globalHeight) * limbSwingAmount);
        neck.addRotX(blend * -(float) (Math.cos(limbSwing * speed) * 0.175 * globalHeight) * limbSwingAmount);
        head.addRotX(blend * (float) (Math.cos(limbSwing * speed + 0.175) * 0.175 * globalHeight + 0.1 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 1.4) * 0.02 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * -(float) (Math.cos(limbSwing * speed - 0.5) * 0.02 * globalHeight) * limbSwingAmount);
        head.addRotZ(blend * 0.03f * limbSwingAmount * globalHeight);

        leftThigh.addRotX(blend * 0.05f * limbSwingAmount * globalHeight);
        leftThigh.addRotZ(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree + 0.05 * globalDegree) * limbSwingAmount);
        leftThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree - 0.15) * limbSwingAmount);
        leftShin.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        leftAnkle.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree + 0.1f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree - 0.6f * globalDegree) * limbSwingAmount);
        leftFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftFoot.addRotY(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -0.4f * globalDegree) * limbSwingAmount);
        leftFoot.addRotY(blend * -(float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree + 1.4f * globalDegree) * limbSwingAmount);
        leftToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightThigh.addRotX(blend * -0.05f * limbSwingAmount * globalHeight);
        rightThigh.addRotZ(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.55f * globalDegree - 0.05 * globalDegree) * limbSwingAmount);
        rightThigh.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 1.5) * 0.1f * globalDegree + 0.15) * limbSwingAmount);
        rightShin.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * -0.7f * globalDegree) * limbSwingAmount);
        rightAnkle.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.40) * 1.1f * globalDegree - 0.1f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -1.3f * globalDegree + 0.6f * globalDegree) * limbSwingAmount);
        rightFoot.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightFoot.addRotY(blend * (float) (Math.cos(limbSwing * speed * 0.5 + 2.5) * -0.4f * globalDegree) * limbSwingAmount);
        rightFoot.addRotY(blend * -(float) (Math.cos(limbSwing * speed * 1 - 0.2) * -0.2f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * -(float) (Math.cos(limbSwing * speed * 0.5 + 3.1) * 1.6f * globalDegree - 1.4f * globalDegree) * limbSwingAmount);
        rightToesBack.addRotX(blend * (float) (Math.cos(limbSwing * speed * 1 + 0.1) * 0.3f * globalDegree) * limbSwingAmount);

        rightShin.addRotX(blend * -(float) (Math.pow(Math.cos(limbSwing * 0.25 * speed - 0.6 + Math.PI/2.0), 12) * 0.6f * globalHeight) * limbSwingAmount);
        rightAnkle.addRotX(blend * (float) (Math.pow(Math.cos(limbSwing * 0.25 * speed - 0.6 + Math.PI/2.0), 12) * 0.6f * globalHeight) * limbSwingAmount);

        leftArm.addRotY(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftArm.addRotZ(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        leftForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);

        rightArm.addRotY(blend * (float) (Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightArm.addRotZ(blend * (float) -(Math.cos(limbSwing * speed + 0.52) * 0.0707 * globalHeight) * limbSwingAmount);
        rightForeArm.addRotX(blend * (float) (Math.cos(limbSwing * speed - 1.0) * 0.03 * globalHeight) * limbSwingAmount);
    }

}