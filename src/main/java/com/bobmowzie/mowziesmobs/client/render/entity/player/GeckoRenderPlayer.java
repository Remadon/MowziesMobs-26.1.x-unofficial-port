package com.bobmowzie.mowziesmobs.client.render.entity.player;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelGeckoPlayerThirdPerson;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoPlayerItemInHandLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.SolarFlareLayer;
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

    public GeckoRenderPlayer(ModelGeckoPlayerThirdPerson geoModel) {
        super(geoModel);
        this.geoModel = geoModel;
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
