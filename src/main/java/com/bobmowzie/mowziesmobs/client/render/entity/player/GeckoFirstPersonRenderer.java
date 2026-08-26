package com.bobmowzie.mowziesmobs.client.render.entity.player;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelGeckoPlayerFirstPerson;
import com.bobmowzie.mowziesmobs.server.ability.Ability;
import com.bobmowzie.mowziesmobs.server.ability.AbilitySection;
import com.bobmowzie.mowziesmobs.server.ability.PlayerAbility;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoObjectRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

/**
 * PORTING NOTE (1.21.1 -> 26.1.2): full redesign - read {@code GeckoRenderPlayer.java}'s class javadoc first, this
 * class hits the exact same architectural wall (GeckoLib 5 no longer exposes bone rotation outside its own live
 * render pass) plus one first-person-specific problem, both explained below.
 * <p>
 * This class no longer extends {@code ItemInHandRenderer} (the old dual-inheritance target). It didn't need to:
 * {@code accesstransformer.cfg} already widens {@code ItemInHandRenderer#renderPlayerArm}/{@code #renderArmWithItem}
 * to {@code public} (see the AT file's own porting-note comment above those two lines), so a plain held reference
 * (obtained via {@code Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer()}, the same
 * shared instance vanilla itself uses) can call them directly - no subclassing required.
 * <p>
 * <b>First-person-specific problem this redesign had to solve:</b> the old {@code renderItemInFirstPerson} read
 * {@code bone.getWorldSpaceMatrix()} (the arm bone's full world transform) for the OFF-hand call without re-running
 * the model's render pass, relying on GeckoLib-4's bones holding their pose persistently from the MAIN-hand call
 * moments earlier. GeckoLib 5's bone pose (`GeoBone#frameSnapshot`) is only valid during that specific bone's own
 * live draw call and is nulled again immediately after - there is no persistent state left to read a frame later,
 * let alone from an entirely separate, earlier-in-the-same-frame render pass. Rather than accept a stale/incorrect
 * off-hand transform, this class's entry point was reshaped from "called once per hand" (matching vanilla's
 * {@code ItemInHandRenderer}/old {@code RenderHandEvent} convention) to {@link #renderHands}, called ONCE per frame,
 * which runs a SINGLE GeckoLib render pass and attaches both hands' item/arm geometry from bone-position-listener
 * callbacks fired live during that one pass (see {@code registerHandListener}) - guaranteeing both hands always use
 * this frame's actual pose. See the wiring note below for what this means for whoever hooks this up to NeoForge's
 * (now per-hand) {@code RenderHandEvent}.
 * <p>
 * <b>Known degradation:</b> the old {@code renderRecursively} override applied an entire alternate
 * translate/rotate/scale path ({@code MowzieRenderUtils.translateMirror/moveToPivotMirror/rotateMirror}) to
 * mirror the whole viewmodel rig's geometry for left-handed players. That per-bone override point no longer exists
 * at the renderer level at all in GeckoLib 5 (cube-drawing/positioning moved fully inside {@code GeoBone}/
 * {@code CuboidGeoBone} - see {@code MowzieGeoBone.java}'s own porting note) - restoring a geometry mirror would
 * need a bone-level hook, which is the model agent's territory, not this file's. This renderer still correctly
 * figures out which bone/hand maps to "main"/"off" when the player is left-handed (see {@code pendingMirror}
 * below, preserved from the old {@code mirror} field) - only the actual mesh-chirality flip is dropped. A
 * left-handed player's first-person view model will show the correct item in the correct hand, just without the
 * geometry itself being mirrored.
 * <p>
 * <b>Wiring (cross-scope, NOT done here):</b> {@code client/ClientEventHandler.onHandRender} (out of this agent's
 * scope) is stubbed pending exactly this redesign. {@code net.neoforged.neoforge.client.event.RenderHandEvent} still
 * fires once per hand and no longer exposes a {@code MultiBufferSource} (only
 * {@code getSubmitNodeCollector()}/{@code getPoseStack()}/{@code getPartialTick()}/{@code getInterpolatedPitch()}/
 * {@code getPackedLight()}) - since {@link #renderHands} handles both hands in one call (see above), the intended
 * wiring is to call it exactly once, e.g. gated on {@code event.getHand() == InteractionHand.MAIN_HAND} (and cancel
 * both hands' vanilla rendering via {@code event.setCanceled(true)} on each event when an ability is active),
 * passing {@code event.getPoseStack()}/{@code event.getSubmitNodeCollector()}/{@code event.getPackedLight()}/
 * {@code event.getInterpolatedPitch()}/{@code event.getPartialTick()} plus
 * {@code Minecraft.getInstance().gameRenderer.getGameRenderState().levelRenderState.cameraRenderState} for the
 * {@code CameraRenderState} (same accessor chain as {@code GeckoRenderPlayer}'s wiring note).
 */
public class GeckoFirstPersonRenderer extends GeoObjectRenderer<GeckoPlayer, Void, GeoRenderState> {

    public static GeckoPlayer.GeckoPlayerFirstPerson GECKO_PLAYER_FIRST_PERSON;

    private static final HashMap<Class<? extends GeckoPlayer>, GeckoFirstPersonRenderer> modelsToLoad = new HashMap<>();

    private static final DataTicket<AbstractClientPlayer> LIVE_PLAYER =
            DataTicket.create("mowziesmobs_gecko_first_person_live_player", AbstractClientPlayer.class);
    private static final DataTicket<GeckoPlayer> LIVE_GECKO_PLAYER =
            DataTicket.create("mowziesmobs_gecko_first_person_animatable", GeckoPlayer.class);

    private final ModelGeckoPlayerFirstPerson geoModel;
    private final ItemInHandRenderer itemInHandRenderer;

    // Left null (not Vec3.ZERO) until a live bone-position-listener callback populates it - SupernovaAbility
    // explicitly null-checks this before use, matching the pre-port field.
    public Vec3 particleEmitterRoot;

    // Per-call context, set immediately before performRenderPass() in renderHands() and consumed by the bone
    // position listeners registered in preRenderPass() - see class javadoc "First-person-specific problem".
    private AbstractClientPlayer pendingPlayer;
    private float pendingPitch;
    private float pendingPartialTick;
    private int pendingPackedLight;
    private boolean pendingMirror;
    private ItemStack pendingMainStack = ItemStack.EMPTY;
    private ItemStack pendingOffStack = ItemStack.EMPTY;
    private PlayerAbility.HandDisplay pendingMainHandDisplay = PlayerAbility.HandDisplay.DEFAULT;
    private PlayerAbility.HandDisplay pendingOffHandDisplay = PlayerAbility.HandDisplay.DEFAULT;
    private float pendingOffHandEquipProgress;

    public GeckoFirstPersonRenderer(ModelGeckoPlayerFirstPerson geoModel) {
        super(geoModel);
        this.geoModel = geoModel;
        this.itemInHandRenderer = Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer();
    }

    public HashMap<Class<? extends GeckoPlayer>, GeckoFirstPersonRenderer> getModelsToLoad() {
        return modelsToLoad;
    }

    public void setSmallArms() {
        this.geoModel.setUseSmallArms(true);
    }

    public ModelGeckoPlayerFirstPerson getAnimatedPlayerModel() {
        return this.geoModel;
    }

    @Override
    public void addRenderData(GeckoPlayer animatable, Void relatedObject, GeoRenderState renderState, float partialTick) {
        renderState.addGeckolibData(LIVE_PLAYER, (AbstractClientPlayer) animatable.getPlayer());
        renderState.addGeckolibData(LIVE_GECKO_PLAYER, animatable);
    }

    @Override
    public void setMolangQueryValues(GeckoPlayer animatable, Void relatedObject, GeoRenderState renderState, float partialTick) {
        // No bespoke Molang query data needed beyond what GeckoLib captures by default.
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<GeoRenderState> renderPassInfo) {
        // Suppress GeoObjectRenderer's default (0.5, 0.51, 0.5) item/block-entity-style centering - not applicable
        // to a viewmodel arm positioned by the caller-supplied PoseStack.
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<GeoRenderState> renderPassInfo, BoneSnapshots snapshots) {
        GeckoPlayer geckoPlayer = renderPassInfo.getGeckolibData(LIVE_GECKO_PLAYER);

        if (geckoPlayer == null || !geoModel.isInitialized()) return;

        // GeckoLib-5-correct replacement for the removed "setCustomAnimations" render-time hook - see
        // MowzieGeoBone.java's class javadoc and GeckoRenderPlayer.java's matching section for the full writeup.
        // ModelGeckoPlayerFirstPerson#setCustomAnimations(GeckoPlayer, long, AnimationTest<GeckoPlayer>) is still
        // declared against an AnimationTest (an extraction-time type) rather than this RenderPassInfo/BoneSnapshots
        // hook - bridged below by reconstructing an equivalent AnimationTest from data already captured on the
        // render state. This bridge is a best-effort cross-scope assumption (the model agent owns that file's exact
        // calling convention) but is internally consistent: it supplies the same (animatable, renderState, manager,
        // controller) tuple the real extraction-time AnimationTest would have held.
        GeoRenderState state = renderPassInfo.renderState();
        Long instanceId = state.getGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID);
        AnimatableManager<GeckoPlayer> manager = castManager(state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER));

        if (instanceId == null || manager == null) return;

        AnimationController<GeckoPlayer> controller = manager.getAnimationControllers().get(geckoPlayer.getControllerName());

        if (controller != null) {
            geoModel.setCustomAnimations(geckoPlayer, instanceId, new AnimationTest<>(geckoPlayer, state, manager, controller));
        }
    }

    @SuppressWarnings("unchecked")
    private static AnimatableManager<GeckoPlayer> castManager(AnimatableManager<? extends GeoAnimatable> manager) {
        return (AnimatableManager<GeckoPlayer>) manager;
    }

    @Override
    public void preRenderPass(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks) {
        registerHandListener(renderPassInfo, renderTasks, HumanoidArm.RIGHT);
        registerHandListener(renderPassInfo, renderTasks, HumanoidArm.LEFT);

        renderPassInfo.addBonePositionListener("ParticleEmitterRoot", (worldPos, modelPos, localPos) -> {
            if (worldPos != null) this.particleEmitterRoot = worldPos;
        });
    }

    /**
     * @param boneSide the rig's bone-naming side ("RightArm"/"LeftArm"). Which logical hand (main/off) that bone
     *                 represents flips when the player is left-handed - mirrors the old {@code mirror}/
     *                 {@code handside} dance in the pre-port code (see {@link #pendingMirror}).
     */
    private void registerHandListener(RenderPassInfo<GeoRenderState> renderPassInfo, SubmitNodeCollector renderTasks, HumanoidArm boneSide) {
        String boneName = (boneSide == HumanoidArm.RIGHT ? "Right" : "Left") + "Arm";
        String controllerSide = boneSide == HumanoidArm.RIGHT ? "Right" : "Left";
        int sideMult = boneSide == HumanoidArm.RIGHT ? -1 : 1;

        renderPassInfo.addBonePositionListener(boneName, (worldPos, modelPos, localPos) -> {
            if (pendingPlayer == null) return;

            HumanoidArm logicalSide = pendingMirror ? boneSide.getOpposite() : boneSide;
            boolean isMainHand = logicalSide == pendingPlayer.getMainArm();
            PlayerAbility.HandDisplay display = isMainHand ? pendingMainHandDisplay : pendingOffHandDisplay;

            if (display == PlayerAbility.HandDisplay.DONT_RENDER) return;

            ItemStack stack = isMainHand ? pendingMainStack : pendingOffStack;
            PoseStack.Pose bonePose = renderPassInfo.poseStack().last();
            PoseStack newMatrixStack = new PoseStack();
            float fixedPitchController = 1.0F - geoModel.getControllerValueInverted("FixedPitchController" + controllerSide);

            newMatrixStack.mulPose(Axis.XP.rotationDegrees(pendingPitch * fixedPitchController));
            newMatrixStack.last().normal().mul(bonePose.normal());
            newMatrixStack.last().pose().mul(bonePose.pose());
            newMatrixStack.translate(sideMult * 0.547, 0.7655, 0.625);

            if (stack.isEmpty() && !isMainHand && display == PlayerAbility.HandDisplay.FORCE_RENDER && !pendingPlayer.isInvisible()) {
                newMatrixStack.translate(0.0, -1.0 * pendingOffHandEquipProgress, 0.0);
                itemInHandRenderer.renderPlayerArm(newMatrixStack, renderTasks, pendingPackedLight, 0.0F, 0.0F, logicalSide);
            } else {
                InteractionHand hand = isMainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                itemInHandRenderer.renderArmWithItem(pendingPlayer, pendingPartialTick, pendingPitch, hand, 0.0F, stack, 0.0F, newMatrixStack, renderTasks, pendingPackedLight);
            }
        });
    }

    /**
     * Renders both first-person hands (viewmodel arm/gecko rig plus item/arm attachment) for one frame. Called ONCE
     * per frame, not once per hand - see class javadoc "First-person-specific problem" for why.
     */
    public void renderHands(AbstractClientPlayer player, GeckoPlayer geckoPlayer, float pitch, float partialTicks, PoseStack poseStack, SubmitNodeCollector renderTasks, CameraRenderState cameraState, int combinedLight) {
        if (!geoModel.isInitialized()) return;

        geoModel.setTextureFromPlayer(player);

        this.pendingPlayer = player;
        this.pendingPitch = pitch;
        this.pendingPartialTick = partialTicks;
        this.pendingPackedLight = combinedLight;
        this.pendingMirror = player.getMainArm() == HumanoidArm.LEFT;

        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        PlayerAbility.HandDisplay mainHandDisplay = PlayerAbility.HandDisplay.DEFAULT;
        PlayerAbility.HandDisplay offHandDisplay = PlayerAbility.HandDisplay.DEFAULT;
        float offHandEquipProgress = 0.0F;

        AbilityData data = DataHandler.getData(player, DataHandler.ABILITY_DATA);

        if (data != null && data.getActiveAbility() != null) {
            Ability<?> ability = data.getActiveAbility();

            if (ability instanceof PlayerAbility playerAbility) {
                if (playerAbility.heldItemMainHandOverride() != null) mainHandStack = playerAbility.heldItemMainHandOverride();
                if (playerAbility.heldItemOffHandOverride() != null) offHandStack = playerAbility.heldItemOffHandOverride();
                mainHandDisplay = playerAbility.getFirstPersonMainHandDisplay();
                offHandDisplay = playerAbility.getFirstPersonOffHandDisplay();
            }

            if (ability.getCurrentSection().sectionType == AbilitySection.AbilitySectionType.STARTUP) {
                offHandEquipProgress = Mth.clamp(1.0F - (ability.getTicksInSection() + partialTicks) / 5.0F, 0.0F, 1.0F);
            } else if (ability.getCurrentSection().sectionType == AbilitySection.AbilitySectionType.RECOVERY
                    && ability.getCurrentSection() instanceof AbilitySection.AbilitySectionDuration durationSection) {
                offHandEquipProgress = Mth.clamp((ability.getTicksInSection() + partialTicks - durationSection.duration + 5) / 5.0F, 0.0F, 1.0F);
            }
        }

        this.pendingMainStack = mainHandStack;
        this.pendingOffStack = offHandStack;
        this.pendingMainHandDisplay = mainHandDisplay;
        this.pendingOffHandDisplay = offHandDisplay;
        this.pendingOffHandEquipProgress = offHandEquipProgress;

        poseStack.pushPose();
        poseStack.translate(0.0, -2.0, -1.0);
        performRenderPass(geckoPlayer, null, poseStack, renderTasks, cameraState, combinedLight, partialTicks);
        poseStack.popPose();
    }
}
