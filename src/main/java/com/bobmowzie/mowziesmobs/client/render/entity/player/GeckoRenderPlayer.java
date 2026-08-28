package com.bobmowzie.mowziesmobs.client.render.entity.player;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelGeckoPlayerThirdPerson;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoPlayerArmorLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoPlayerItemInHandLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.SolarFlareLayer;
import com.bobmowzie.mowziesmobs.server.ability.Ability;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.WroughtAxeSlamAbility;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.WroughtAxeSwingAbility;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.heliomancy.SolarBeamAbility;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.heliomancy.SolarFlareAbility;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.heliomancy.SunstrikeAbility;
import com.bobmowzie.mowziesmobs.server.ability.abilities.player.heliomancy.SupernovaAbility;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;

/**
 * PORTING NOTE (1.21.1 -> 26.1.2): full redesign, not a mechanical port. Read this class's javadoc before touching
 * it again - the old approach (dual-inherit {@code PlayerRenderer}/{@code GeoRenderer<GeckoPlayer>}, drive vanilla
 * {@code PlayerModel}'s swappable layer parts from GeckoLib bone world matrices) is categorically impossible against
 * 26.1.2 + GeckoLib 5.5.2 for reasons enumerated below. This is a genuinely different architecture that preserves
 * the core observable behaviour (the player's own body visibly performs GeckoLib animations during abilities, in
 * both perspectives) while dropping one piece (vanilla decorative-layer bone-matching) that is provably not
 * achievable with the current public API - see "What's dropped" at the bottom.
 * <p>
 * <b>Why the old approach is dead:</b>
 * <ul>
 *   <li>{@code PlayerRenderer} was replaced by {@code AvatarRenderer<AvatarlikeEntity extends Avatar &
 *       ClientAvatarEntity>}, which now extends {@code LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState,
 *       PlayerModel>} (not generic in the model any more) - the old "extend PlayerRenderer, replace this.model with
 *       a matrix-mode ModelPlayerAnimated, iterate this.layers manually" technique has no equivalent shape.</li>
 *   <li>{@code PlayerModel}'s clothing-layer parts ({@code jacket}/{@code leftSleeve}/{@code rightSleeve}/
 *       {@code leftPants}/{@code rightPants}) are now {@code public final ModelPart} - can't be swapped for a
 *       {@code ModelPartMatrix} stand-in any more (see {@code ModelPlayerAnimated.java}).</li>
 *   <li>Even if they could be swapped: GeckoLib 5 does not expose a bone's full world transform (rotation/matrix)
 *       outside of its own live render traversal any more. Confirmed by reading the real GeckoLib 5.5.2 source
 *       (decompiled): {@code GeoBone#frameSnapshot} (where a bone's animated pose lives) is created fresh and NULLED
 *       again in a {@code finally} block around each {@code RenderPassInfo#renderPosed(Runnable)} call - i.e. it is
 *       valid ONLY for the duration of that one bone's own draw call. The one supported way to observe a bone's pose
 *       from outside is {@code RenderPassInfo#addBonePositionListener}, and its callback is strictly
 *       position-only (world/model/local {@code Vec3}, no rotation) by its own declared interface
 *       ({@code BonePositionListener.accept(Vec3, Vec3, Vec3)}). There is no supported way left to extract a bone's
 *       rotation for use by a second, unrelated renderer (like vanilla's {@code AvatarRenderer}/{@code PlayerModel}).</li>
 *   <li>NeoForge's render-cancellation hooks changed too: {@code RenderLivingEvent.Pre}/{@code RenderPlayerEvent.Pre}
 *       (confirmed by inspecting the real NeoForge 26.1.2.95 classes) dropped {@code getEntity()} entirely - they
 *       only expose an already-captured, back-reference-free {@code AvatarRenderState} snapshot. The old
 *       "cancel + call our own renderer with the live entity" technique used from {@code ClientEventHandler} no
 *       longer has a live entity to call it with from that hook alone.</li>
 * </ul>
 * <b>The new design:</b> this class no longer extends any vanilla renderer at all. It's a small, standalone
 * {@code GeoObjectRenderer<GeckoPlayer, Void, GeoRenderState>} (GeckoLib 5's own sanctioned base class for a
 * {@code GeoAnimatable} that isn't an {@code Entity}/{@code Item}/block entity - see {@code GeoObjectRenderer}'s
 * real source; {@code GeckoPlayer} is a plain wrapper object referencing a {@code Player}, not an {@code Entity}
 * itself, so it was never eligible for {@code GeoEntityRenderer<T extends Entity & GeoAnimatable, R>} either).
 * {@link #render} is a self-contained entry point (world-position-relative {@code PoseStack} in, GeckoLib pass out)
 * meant to be invoked in place of vanilla's own player body rendering while a GeckoLib ability animation should be
 * driving the pose - see "Wiring (cross-scope, not done here)" below.
 * <p>
 * <b>Bone-driven procedural pose ({@link #adjustModelBonesForRender}):</b> this is the GeckoLib-5-correct
 * replacement for the removed GeckoLib-4 "setCustomAnimations" hook - {@code RenderPassInfo.create(...)} registers
 * {@code renderer::adjustModelBonesForRender} as a {@code BoneUpdater} that runs immediately after the base
 * keyframe animation controller has populated this frame's bone snapshots but before anything renders them
 * (confirmed by reading {@code RenderPassInfo.create}: {@code addBoneUpdater(renderer::applyAnimationControllers)}
 * is added first, {@code addBoneUpdater(renderer::adjustModelBonesForRender)} second - later updaters run later, so
 * this one's procedural {@code addRotX}/{@code addPos} edits land on top of the keyframe pose, not before it).
 * {@code MowzieGeoBone.java}'s own class javadoc explicitly calls this out as "out of this agent's scope... see the
 * porting report" - this file is that follow-up. {@code ModelGeckoBiped#setRotationAngles(...)} (already fully
 * ported by the model agent, unchanged signature) is invoked from here exactly as the old
 * {@code GeckoRenderPlayer#actuallyRender} invoked it.
 * <p>
 * <b>Attachment points (held item / particle root) - position only:</b> {@link #registerAttachmentListener} uses
 * {@code RenderPassInfo#addBonePositionListener}, but reads the FULL bone matrix (not just the listener's Vec3
 * params) by capturing {@code renderPassInfo.poseStack().last().pose()} synchronously from inside the listener
 * callback - confirmed safe by reading {@code RenderUtil.prepMatrixForBoneAndUpdateListeners}: the listener fires
 * while {@code poseStack} is pushed to that exact bone's full world transform (translate+rotate+scale), so reading
 * the live PoseStack from the callback (rather than relying on the listener's derived-position-only parameters)
 * recovers the same fidelity the old {@code renderRecursively}-based matrix capture had. <b>Known ordering caveat:</b>
 * {@code SubmitNodeCollector} submissions (and therefore these listener callbacks) are collected now and flushed
 * later in the frame, not executed synchronously inside {@link #render}. {@link #betweenHandsPos}/
 * {@link #particleEmitterRoot} are read by unrelated code (ability classes) at arbitrary later times, so they may
 * lag by up to one render frame versus the old synchronous read. This is imperceptible for a particle spawn point
 * and is the one timing simplification made here - flagging per the porting rules rather than leaving it silent.
 * <p>
 * <b>What's dropped (flagged, not silent):</b> vanilla's decorative player render layers (equipped armor via
 * {@code HumanoidArmorLayer}, cape, elytra/wings, arrows stuck in the body, bee stinger, spin-attack effect,
 * parrot-on-shoulder) are NOT rendered while this renderer is driving the pose for an active ability. Those layers
 * all read their geometry's position/rotation off vanilla's shared {@code PlayerModel} {@code ModelPart} tree, and
 * as established above there is no supported way any more to slave that tree's rotation to the GeckoLib pose from
 * outside GeckoLib's own live render pass. Rendering them un-synced would show them floating in the vanilla
 * arm-swing pose while the visible body performs the ability animation, which is worse than not rendering them.
 * The GeckoLib rig itself already includes its own clothing-layer geometry (the {@code BodyLayer}/{@code *Layer}
 * bones baked into {@code animated_player.geo.json}, driven by {@code ModelGeckoPlayerThirdPerson}), so the
 * skin/clothing silhouette itself is NOT degraded - only externally-attached vanilla decorations are skipped, and
 * only for the duration of an active ability (vanilla rendering resumes normally the instant no ability is active -
 * see the {@code ClientEventHandler} wiring note below).
 * <p>
 * <b>Wiring (cross-scope, NOT done here):</b> {@code client/ClientEventHandler.java} (out of this agent's scope)
 * already has {@code onHandRender}/{@code renderLivingEvent} stubs with FIXME headers anticipating exactly this
 * redesign. The intended wiring, for whoever owns that file: in a handler for
 * {@code net.neoforged.neoforge.client.event.RenderPlayerEvent.Pre}, resolve the live entity from
 * {@code event.getRenderState().id} via {@code Minecraft.getInstance().level.getEntity(id)} (RenderPlayerEvent.Pre
 * does not expose the live entity directly any more - see above), look up
 * {@code DataHandler.getData(player, DataHandler.PLAYER_DATA).getGeckoPlayer()}, and if an ability is active,
 * {@code event.setCanceled(true)} and call {@link #render} with {@code event.getPoseStack()}/
 * {@code event.getSubmitNodeCollector()}/{@code event.getPartialTick()} plus
 * {@code Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState} (confirmed
 * public accessor chain) for the {@code CameraRenderState}. Not performed here per the porting task's file-scope
 * constraint - flagged clearly rather than reaching outside this agent's 4 assigned files.
 */
public class GeckoRenderPlayer extends GeoObjectRenderer<GeckoPlayer, Void, GeoRenderState> {

    private static final HashMap<Class<? extends GeckoPlayer>, GeckoRenderPlayer> modelsToLoad = new HashMap<>();

    private static final DataTicket<AbstractClientPlayer> LIVE_PLAYER =
            DataTicket.create("mowziesmobs_gecko_render_player_live_player", AbstractClientPlayer.class);

    private final ModelGeckoPlayerThirdPerson geoModel;

    // Left null (not Vec3.ZERO) until the first successful render pass populates them - out-of-scope consumers
    // (SupernovaAbility, EntitySolarBeam) explicitly null-check these before use, matching the pre-port fields.
    public Vec3 betweenHandsPos;
    public Vec3 particleEmitterRoot;

    private final Matrix4f leftHeldItemPose = new Matrix4f();
    private final Matrix4f rightHeldItemPose = new Matrix4f();
    private final Matrix4f particleEmitterRootPose = new Matrix4f();

    private final GeckoPlayerItemInHandLayer itemInHandLayer = new GeckoPlayerItemInHandLayer(this);
    private final SolarFlareLayer solarFlareLayer = new SolarFlareLayer(this);
    private final GeckoPlayerArmorLayer armorLayer;

    public GeckoRenderPlayer(ModelGeckoPlayerThirdPerson geoModel, EntityRendererProvider.Context context) {
        super(geoModel);
        this.geoModel = geoModel;
        this.armorLayer = new GeckoPlayerArmorLayer(context.getModelSet(), context.getEquipmentAssets());
    }

    public ModelGeckoPlayerThirdPerson getGeckoModel() {
        return geoModel;
    }

    /** Back-compat alias - some out-of-scope {@code client/render/entity/layer/**} files still call this name. */
    public ModelGeckoPlayerThirdPerson getAnimatedPlayerModel() {
        return geoModel;
    }

    public HashMap<Class<? extends GeckoPlayer>, GeckoRenderPlayer> getModelsToLoad() {
        return modelsToLoad;
    }

    @Override
    public void addRenderData(GeckoPlayer animatable, Void relatedObject, GeoRenderState renderState, float partialTick) {
        renderState.addGeckolibData(LIVE_PLAYER, (AbstractClientPlayer) animatable.getPlayer());
    }

    @Override
    public void setMolangQueryValues(GeckoPlayer animatable, Void relatedObject, GeoRenderState renderState, float partialTick) {
        // No bespoke Molang query data needed for the player rig beyond what GeckoLib captures by default.
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        // GeoObjectRenderer's default here translates (0.5, 0.51, 0.5) for centered item/block-entity-style
        // rendering, which does not apply to a player entity rendered at the caller-supplied world-relative
        // origin - suppress it.
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        AbstractClientPlayer player = renderPassInfo.getGeckolibData(LIVE_PLAYER);

        if (player == null || !geoModel.isInitialized()) return;

        applyModelVisibility(player);

        float partialTick = renderPassInfo.renderState().getPartialTick();
        boolean shouldSit = player.isPassenger() && player.getVehicle() != null && player.getVehicle().shouldRiderSit();
        float limbSwing = 0.0F;
        float limbSwingAmount = 0.0F;

        if (!shouldSit && player.isAlive()) {
            limbSwingAmount = player.walkAnimation.speed(partialTick);
            limbSwing = player.walkAnimation.position(partialTick);
            if (player.isBaby()) limbSwing *= 3.0F;
            if (limbSwingAmount > 1.0F) limbSwingAmount = 1.0F;
        }

        float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        float headRot = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float netHeadYaw = headRot - bodyRot;
        float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
        float ageInTicks = player.tickCount + partialTick;

        HumanoidModel.ArmPose leftPose = AvatarRenderer.getArmPose(player, HumanoidArm.LEFT);
        HumanoidModel.ArmPose rightPose = AvatarRenderer.getArmPose(player, HumanoidArm.RIGHT);
        geoModel.leftArmPose = leftPose;
        geoModel.rightArmPose = rightPose;
        geoModel.isSneak = player.isCrouching();

        // FOLLOW-UP FIX (axe swing/slam: body scatters into the far distance, screenshot-confirmed): same root
        // cause as the Umvuthana bone-reset bug fixed earlier - ModelGeckoBiped#setRotationAngles unconditionally
        // does addRotX/addRotY on "Head" every frame (the procedural head-look-at-aim-direction layer), but
        // animated_player.animation.json's axe_swing_start_right/left and axe_swing_vertical clips (the ones
        // WroughtAxeSwingAbility/WroughtAxeSlamAbility play) have no keyframe track for "Head" at all - so nothing
        // ever refreshes its snapshot while those clips are active, and the additive head-look offset compounds
        // every single render frame (not just every tick) for the ability's whole duration, same class of bug as
        // Umvuthana's walk clips. Resetting Head before the additive math runs fixes it, mirroring
        // RenderUmvuthana#adjustModelBonesForRender's per-clip bone resets. NOTE: idle, ice_breath_*, sunstrike,
        // tunneling_*, hit_boulder, backstab_*, and solar_flare also have no "Head" track and likely have this same
        // latent bug - not reset here since only the axe was reported broken; if any of those abilities are
        // reported with the same "player scatters" symptom, add them to this check.
        //
        // FOLLOW-UP FIX 2 (one arm flails, the other extends to the side and slowly spins): same class of bug
        // again, but for the "controller" bones ModelGeckoBiped reads as molang-style parameters rather than the
        // mesh bones themselves. ArmPitchController/LegWalkController/ArmBreathController are ALSO missing from
        // the axe clips' bone lists (only ArmSwingController/CrouchController/SwimController are covered), so
        // armLookAmount/legWalkAmount/armBreathAmount are left holding whatever stale value they last had from
        // before the ability started (e.g. idle), instead of reading 0 for a clip that wants full manual control
        // of the arm/leg pose. armLookAmount in particular gets multiplied by netHeadYaw/headPitch and added onto
        // LeftClavicle/RightClavicle on top of the swing clip's own keyframed rotation - since those change as the
        // camera moves, a stale non-zero armLookAmount reads as an arm slowly rotating/tracking the camera
        // independently of the swing itself, matching the reported symptom. RightClavicle/LeftClavicle/RightArm/
        // LeftArm themselves are NOT reset here (unlike Head) since the axe clips DO keyframe those directly -
        // resetting the mesh bones would discard the intended swing pose; resetting only the controller bones
        // zeroes out just the stale additive contribution while leaving the keyframed pose intact.
        // FOLLOW-UP FIX 3 (slam only: whole player renders far away, screenshot-confirmed): a slam is only ever
        // triggered by shift-right-clicking (see ItemWroughtAxe#use's verticalAttack check), and holding shift
        // means isSneak is true for the entire ability - which turns on ModelGeckoBiped#setRotationAngles's
        // isSneak block, an UNCONDITIONAL addPos/addPosY on Body/Waist/LeftArm/RightArm every frame (crouch
        // posture offset). axe_swing_vertical (slam) has a "rotation" track but no "position" track for any of
        // those four bones, so - same bug family yet again - nothing ever refreshes their position snapshot while
        // it plays, and the crouch offset compounds every render frame for the whole slam. axe_swing_start_right/
        // left (swing) have the identical gap for Body/LeftArm/RightArm (Waist happens to have its own position
        // track there, so it's excluded below to avoid discarding that), but swinging can't normally trigger
        // isSneak=true in the first place (it's the non-shift branch) - reset unconditionally anyway since a
        // position-only reset is harmless even when isSneak never fires, and it costs nothing to also cover the
        // ability being renamed/re-triggered from an unexpected state. Only position is touched (not the whole
        // bone via resetBoneToSnapshot, unlike Head above) since Body/LeftArm/RightArm's ROTATION genuinely is
        // keyframed by the swing/slam clips and must survive.
        // FOLLOW-UP FIX (Sun's Blessing abilities: Sunstrike/Solar Flare made the whole player disappear, Solar
        // Beam's arms flailed, Supernova flipped the player up then made them disappear): the exact bug class
        // documented above and anticipated in this method's own earlier comment ("idle, ice_breath_*, sunstrike,
        // tunneling_*, hit_boulder, backstab_*, and solar_flare also have no Head track and likely have this same
        // latent bug"), now confirmed live for these 4 abilities and fixed the same way - reset each bone/channel
        // that a given clip does NOT keyframe, so ModelGeckoBiped's unconditional additive math (head-look,
        // arm-pitch/leg-walk/arm-breath controllers, and - critically for Supernova, which can only be triggered
        // while sneaking - the isSneak block's addRotX/addPos on Body/Waist) has nothing stale to compound onto.
        // Checked each clip's actual bone/channel coverage directly against animated_player.animation.json rather
        // than guessing, since resetting a bone a clip DOES keyframe would discard that clip's own intended pose -
        // exactly the mistake the original axe fix's own comments warn against for RightArm/LeftArm/Waist there.
        // Per-clip coverage gaps found (bones/channels present are safe to leave alone):
        //   sunstrike (0.92s):          Body/LeftArm/RightArm have rotation only (no position); Head, Waist,
        //                               LegWalkController, ArmBreathController have no track at all.
        //   solar_beam_charge (5.36s):  LegWalkController, ArmBreathController have no track at all. Everything
        //                               else (Head, ArmPitchController, Body's rotation, Waist/LeftArm/RightArm's
        //                               rotation+position) is fully covered.
        //   solar_flare (1.4s):         Body has rotation only (no position); Head, ArmPitchController, Waist,
        //                               LegWalkController, ArmBreathController have no track at all.
        //   supernova (5.76s):          Body and Waist have no track at all (rotation OR position); ArmPitchController,
        //                               ArmBreathController have no track at all.
        AbilityData abilityData = DataHandler.getData(player, DataHandler.ABILITY_DATA);
        Ability<?> activeAbility = abilityData.getActiveAbility();
        boolean isAxeSwingOrSlam = activeAbility instanceof WroughtAxeSwingAbility || activeAbility instanceof WroughtAxeSlamAbility;
        boolean isSunstrike = activeAbility instanceof SunstrikeAbility;
        boolean isSolarBeam = activeAbility instanceof SolarBeamAbility;
        boolean isSolarFlare = activeAbility instanceof SolarFlareAbility;
        boolean isSupernova = activeAbility instanceof SupernovaAbility;

        if (isAxeSwingOrSlam || isSunstrike || isSolarFlare) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("Head"));
        }
        if (isAxeSwingOrSlam || isSolarFlare || isSupernova) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("ArmPitchController"));
        }
        if (isAxeSwingOrSlam || isSunstrike || isSolarBeam || isSolarFlare) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("LegWalkController"));
        }
        if (isAxeSwingOrSlam || isSunstrike || isSolarBeam || isSolarFlare || isSupernova) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("ArmBreathController"));
        }
        if (isAxeSwingOrSlam || isSunstrike || isSolarBeam || isSolarFlare) {
            geoModel.getMowzieBone("Body").setPos(0, 0, 0);
        }
        if (isSupernova) {
            // supernova's clip has no "Body" track at all (rotation included, unlike the other abilities above) -
            // and since Supernova can only be triggered while sneaking, isSneak's addRotX on Body would otherwise
            // compound unchecked for the whole ~5.76s duration too, not just its addPos.
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("Body"));
        }
        if (isAxeSwingOrSlam || isSunstrike) {
            geoModel.getMowzieBone("LeftArm").setPos(0, 0, 0);
            geoModel.getMowzieBone("RightArm").setPos(0, 0, 0);
        }
        if (activeAbility instanceof WroughtAxeSlamAbility || isSunstrike || isSolarFlare || isSupernova) {
            geoModel.getMowzieBone("Waist").setPos(0, 0, 0);
        }
        // FOLLOW-UP FIX (Sunstrike/Solar Beam/Solar Flare: reported arm "flailing"/"waving wildly" persisted even
        // after the resets above): the arms themselves DO have correct, non-stale keyframed rotation in all three
        // clips (confirmed against animated_player.animation.json) - the missing piece was their PARENT bones,
        // "LeftClavicle"/"RightClavicle", which are absent from all three clips' bone lists. setRotationAngles's
        // head-look layer unconditionally does addRotX/addRotY on both clavicles based on the live camera's
        // netHeadYaw/headPitch (see the top of this method), and since nothing ever refreshes their snapshot while
        // these clips play, that camera-tracking rotation compounds every render frame on the bone the whole arm
        // is parented to - so even though the arm's OWN local rotation is correct, the compounding clavicle
        // rotation swings the entire arm (clavicle + arm together) around unpredictably as the camera moves,
        // which is exactly what "flailing"/"waving around" describes. axe and supernova's clips both keyframe
        // both clavicles directly and are unaffected.
        if (isSunstrike || isSolarBeam || isSolarFlare) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("LeftClavicle"));
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("RightClavicle"));
        }
        // FOLLOW-UP FIX (Supernova: head tilts to a full straight-up or straight-down extreme, screenshot-confirmed
        // - visible as the neck bending backward hard while the supernova charges): same bug family as the Waist/
        // Body position drift already handled above for Supernova, but on "Neck" specifically - it has no track at
        // all in supernova's clip, and since Supernova can only be triggered while sneaking, isSneak's
        // Neck.addRotX(0.5F * sneakController) runs unconditionally every frame with nothing to reset it, so the
        // tilt compounds for the whole ~5.76s duration. solar_flare's clip also has no "Neck" track and can
        // likewise only be triggered while sneaking (shift+left-click), so it gets the same fix pre-emptively
        // rather than waiting for a separate bug report - sunstrike/solar_beam_charge/axe all keyframe Neck
        // directly and are unaffected.
        if (isSolarFlare || isSupernova) {
            geoModel.resetBoneToSnapshot(geoModel.getMowzieBone("Neck"));
        }

        geoModel.setRotationAngles(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick);
    }

    private void applyModelVisibility(AbstractClientPlayer player) {
        if (player.isSpectator()) {
            geoModel.setVisible(false);
            geoModel.bipedHead().setHidden(false);
            geoModel.bipedHeadwear().setHidden(false);
        } else {
            geoModel.setVisible(true);
            geoModel.bipedHeadwear().setHidden(!player.isModelPartShown(PlayerModelPart.HAT));
            geoModel.bipedBodywear().setHidden(!player.isModelPartShown(PlayerModelPart.JACKET));
            geoModel.bipedLeftLegwear().setHidden(!player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG));
            geoModel.bipedRightLegwear().setHidden(!player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG));
            geoModel.bipedLeftArmwear().setHidden(!player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
            geoModel.bipedRightArmwear().setHidden(!player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
        }
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        registerAttachmentListener(renderPassInfo, "LeftHeldItem", leftHeldItemPose);
        registerAttachmentListener(renderPassInfo, "RightHeldItem", rightHeldItemPose);
        registerAttachmentListener(renderPassInfo, "ParticleEmitterRoot", particleEmitterRootPose);

        AbstractClientPlayer player = renderPassInfo.getGeckolibData(LIVE_PLAYER);
        if (player != null) {
            itemInHandLayer.registerListeners(renderPassInfo, renderTasks, player);
            solarFlareLayer.registerListener(renderPassInfo, renderTasks, player);
            // FOLLOW-UP FIX (armor invisible during abilities, documented as intentionally dropped above): render
            // equipped vanilla armor per-bone via the same bone-position-listener technique itemInHandLayer already
            // uses - see GeckoPlayerArmorLayer's own javadoc for why this sidesteps the "no live bone world
            // transform outside GeckoLib's own render pass" wall instead of fighting it.
            armorLayer.registerListeners(renderPassInfo, renderTasks, player);
        }
    }

    private void registerAttachmentListener(RenderPassInfo<GeoRenderState> renderPassInfo, String boneName, Matrix4f target) {
        renderPassInfo.addBonePositionListener(boneName, (worldPos, modelPos, localPos) ->
                target.set(renderPassInfo.poseStack().last().pose()));
    }

    /**
     * Renders this player's third-person body via GeckoLib in place of vanilla's own body rendering for this frame.
     * See class javadoc for the intended (cross-scope) call site.
     *
     * @param player       the live player entity being rendered
     * @param geckoPlayer  this player's {@link GeckoPlayer} wrapper (holds the animatable instance cache/controller)
     * @param poseStack    already translated to this entity's interpolated world-relative render position
     * @param renderTasks  the frame's {@link SubmitNodeCollector}
     * @param cameraState  the frame's {@link CameraRenderState}
     * @param packedLight  packed light coordinates for this entity
     * @param partialTick  render partial tick
     */
    public void render(AbstractClientPlayer player, GeckoPlayer geckoPlayer, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState, int packedLight, float partialTick) {
        if (!geoModel.isInitialized()) return;

        geoModel.setTextureFromPlayer(player);

        poseStack.pushPose();
        applyBodyPose(player, poseStack, partialTick);
        performRenderPass(geckoPlayer, null, poseStack, renderTasks, cameraState, packedLight, partialTick);
        poseStack.popPose();

        updateAttachmentPoints();
    }

    /**
     * Simplified port of {@code AvatarRenderer#scale}/{@code #setupRotations}: fixed vanilla player scale + body-yaw
     * facing. KNOWN SIMPLIFICATION: the pre-port {@code GeckoRenderPlayer#setupRotations}/
     * {@code applyRotationsLivingRenderer} additionally special-cased elytra fall-flying, swimming, sleeping and
     * death-flop rotations - not reproduced here. Abilities are not expected to trigger during those states; if a
     * third-person ability animation ever looks wrong while the player is also flying/swimming/sleeping/dying,
     * start here.
     */
    private void applyBodyPose(AbstractClientPlayer player, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
        float bodyRot = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
    }

    private void updateAttachmentPoints() {
        Vec3 left = transformOrigin(leftHeldItemPose);
        Vec3 right = transformOrigin(rightHeldItemPose);
        betweenHandsPos = right.add(left.subtract(right).scale(0.5));
        particleEmitterRoot = transformOrigin(particleEmitterRootPose);
    }

    private static Vec3 transformOrigin(Matrix4f pose) {
        Vector4f origin = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F).mul(pose);
        return new Vec3(origin.x(), origin.y(), origin.z());
    }
}
