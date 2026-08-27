package com.bobmowzie.mowziesmobs.client.render.entity;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelUmvuthana;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoBone;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.GeckoSunblockLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.UmvuthanaArmorLayer;
import com.bobmowzie.mowziesmobs.client.render.entity.layer.UmvuthanaSunLayer;
import com.bobmowzie.mowziesmobs.server.entity.MowzieGeckoEntity;
import com.bobmowzie.mowziesmobs.server.entity.umvuthana.EntityUmvuthana;
import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * PORTING NOTE: `renderUpdates` -> `registerBonePositionListeners`, same pattern as RenderElokosa.java (read the
 * class javadoc there for the general rationale). The `entity.updateRattleSound(mask.getRotZ())` call is a
 * partial exception: GeckoLib 5 only offers a listener mechanism for a bone's rendered *position*
 * (`RenderPassInfo#addBonePositionListener`), not its rotation - there is no equivalent live "rotation listener".
 * The direct `MowzieGeoBone#getRotZ()` read is kept as-is (same as the pre-existing `getControllerValue(...)` calls
 * elsewhere in this port, e.g. RenderElokosa's shadow radius), since that value already came from a similarly
 * loosely-timed bone-snapshot read even in the old architecture - but note it may now read a slightly staler value
 * than before (potentially the previous frame's, or a freshly-defaulted snapshot) since it's evaluated at
 * `preRenderPass` time rather than immediately after the model render call. If the rattle sound timing feels off at
 * runtime, this is the place to revisit.
 * <p>
 * FOLLOW-UP FIX: {@link ModelUmvuthana#setCustomAnimations} (claw/talon visibility, crest visibility, root/mask
 * scale compensation, head look-at tracking, and all the procedural walk/run bone math) was never actually being
 * invoked - GeckoLib 4's automatic per-frame "setCustomAnimations" hook was removed and replaced with
 * {@code GeoRenderer#adjustModelBonesForRender}, which this class never overrode (see GeckoRenderPlayer.java for
 * the reference pattern this follows). With neither {@code leftIndexTalon}/{@code rightIndexTalon} (raptor claws,
 * only meant to show for the FURY mask) nor {@code leftIndexClaw}/{@code rightIndexClaw} (the normal claw, meant to
 * show for every other mask) ever explicitly hidden, the geo model's own default bind-pose visibility applied
 * instead - both bone sets rendering simultaneously, stacked on top of each other, which is exactly the "one claw
 * is very large and long" bug reported on non-FURY-masked Umvuthana. {@link #adjustModelBonesForRender} below
 * bridges GeckoLib's new render-time hook back into the model's still-{@code AnimationTest}-shaped method, the same
 * way {@code GeckoFirstPersonRenderer#adjustModelBonesForRender} does for the player model. The specific
 * {@code AnimationController} passed doesn't matter - {@code ModelUmvuthana#setCustomAnimations} never reads it -
 * so the first one registered on this entity's {@code AnimatableManager} is used.
 * <p>
 * NOTE: the same missing wiring affects {@code RenderBluff}, {@code RenderUmvuthi}, {@code RenderElokosa}, and
 * {@code RenderSculptor} (their models all declare an equivalent {@code setCustomAnimations} that's similarly never
 * called) - flagged for a follow-up pass, not fixed here since this session's reports were Umvuthana-specific.
 */
public class RenderUmvuthana extends MowzieGeoEntityRenderer<EntityUmvuthana, LivingEntityRenderState> {
    public RenderUmvuthana(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ModelUmvuthana());
        this.withRenderLayer(new FrozenRenderHandler.GeckoLayerFrozen<>(this, renderManager));
        this.withRenderLayer(new GeckoSunblockLayer<>(this, renderManager));
        this.withRenderLayer(new UmvuthanaArmorLayer(this, renderManager, "maskTwitcher"));
        this.withRenderLayer(new UmvuthanaArmorLayer(this, renderManager, "maskHand"));
        this.withRenderLayer(new UmvuthanaSunLayer(this, renderManager));
        this.shadowRadius = 0.6f;
    }

    @Override
    protected void registerBonePositionListeners(RenderPassInfo<LivingEntityRenderState> renderPassInfo, EntityUmvuthana entity) {
        renderPassInfo.addBonePositionListener("head", (worldPos, modelPos, localPos) -> {
            if (worldPos != null && entity.headPos != null && entity.headPos.length > 0) {
                entity.headPos[0] = worldPos;
            }
        });

        if (!Minecraft.getInstance().isPaused()) {
            MowzieGeoBone mask = getMowzieGeoModel().getMowzieBone("maskTwitcher");
            entity.updateRattleSound(mask.getRotZ());
        }
    }

    @Override
    public boolean shouldRender(EntityUmvuthana entity, Frustum p_114492_, double p_114493_, double p_114494_, double p_114495_) {
        boolean result = super.shouldRender(entity, p_114492_, p_114493_, p_114494_, p_114495_);
        if (!result) entity.headPos[0] = entity.position().add(0, entity.getEyeHeight(), 0);
        return result;
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> renderPassInfo, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(renderPassInfo, snapshots);

        MowzieGeckoEntity liveEntity = renderPassInfo.getGeckolibData(LIVE_ANIMATABLE);
        if (!(liveEntity instanceof EntityUmvuthana entity) || !getMowzieGeoModel().isInitialized()) return;

        LivingEntityRenderState state = renderPassInfo.renderState();
        Long instanceId = state.getGeckolibData(DataTickets.ANIMATABLE_INSTANCE_ID);
        AnimatableManager<EntityUmvuthana> manager = castManager(state.getGeckolibData(DataTickets.ANIMATABLE_MANAGER));
        if (instanceId == null || manager == null || manager.getAnimationControllers().isEmpty()) return;

        AnimationController<EntityUmvuthana> controller = manager.getAnimationControllers().values().iterator().next();
        AnimationTest<EntityUmvuthana> animationTest = new AnimationTest<>(entity, state, manager, controller);

        // GeckoLib registers two bone updaters per render pass, in this fixed order: renderer::applyAnimationControllers
        // (bakes the file-based keyframe animations - walk_neutral, mask_twitch, etc. - into each bone's frameSnapshot)
        // THEN renderer::adjustModelBonesForRender (this method). A blanket resetAllBonesToInitialSnapshot() here would
        // wipe out that keyframe pose before setCustomAnimations's own additive math runs on top of it - that's what
        // broke the walk/idle animations and made the arms disappear on the previous attempt at this fix. "root"
        // needs resetting unconditionally: it's a bone setCustomAnimations scales every frame
        // (root.multiplyScale(0.83/0.93, ...)) with no keyframe track of its own, in ANY clip, to refresh it, so
        // without resetting it specifically, that multiply compounds every frame and shrinks the whole model toward
        // zero within a fraction of a second (the very first attempt's bug).
        //
        // FOLLOW-UP FIX (massive rotation on the upper body/arms while walking passively; body launching into the
        // sky and vanishing while walking aggressively): the walkForward/walkBackward/run bone rotations ARE gated
        // behind a movement "blend" that's 0 while idle - true, and idle is fine - but the previous comment's
        // conclusion that they therefore "don't have the same unbounded-compounding problem" turned out to be
        // wrong specifically while *moving*. Checked directly against umvuthana.animation.json (bone-identical to
        // the original 1.21.1 mod's own file, confirmed earlier - this is the original's authored data, not
        // corrupted): walk_neutral has NO keyframe track at all for chest/leftArm/rightArm, and walk_aggressive has
        // none for hips/leftArm and almost the entire leg chain (leftThigh/leftShin/leftAnkle/leftFoot/
        // rightThigh/rightShin/rightAnkle/rightFoot). idle_neutral and idle_aggressive cover all of these. So the
        // instant the entity starts moving and the active clip switches to walk_neutral/walk_aggressive, those
        // specific bones stop getting a fresh per-frame keyframe bake - same exact "root" problem, just clip- and
        // bone-specific instead of universal. hips is a *position* add (addPosY), so its version of this compounds
        // into launching the whole body skyward every frame - the "vanish into the sky" symptom, and much worse for
        // the aggressive clip since it leaves nearly the whole leg chain unprotected too, not just hips. Fixed by
        // resetting exactly the bones known to be uncovered for whichever of the two moving clips is currently
        // active (never both sets at once, and never touching bones idle_neutral/idle_aggressive/the relevant
        // moving clip actually DOES cover, so their real keyframe-driven baseline stance is left alone). leftToesBack/
        // rightToesBack are uncovered in all four looping clips, so those two reset unconditionally, same as root.
        MowzieGeoBone rootBone = getMowzieGeoModel().getMowzieBone("root");
        if (rootBone != null) getMowzieGeoModel().resetBoneToSnapshot(rootBone);
        resetBones("leftToesBack", "rightToesBack");
        if (animationTest.isMoving()) {
            if (entity.isAggressive()) {
                resetBones("hips", "leftThigh", "leftShin", "leftAnkle", "leftFoot", "rightThigh", "rightShin", "rightAnkle", "rightFoot", "leftArm");
            } else {
                resetBones("chest", "leftArm", "rightArm");
            }
        }

        // setCustomAnimations is a plain (non-override) method declared directly on ModelUmvuthana, not part of the
        // MowzieGeoModel<T> supertype getMowzieGeoModel() is statically typed as - needs the concrete cast.
        ((ModelUmvuthana) getMowzieGeoModel()).setCustomAnimations(entity, instanceId, animationTest);
    }

    private void resetBones(String... names) {
        for (String name : names) {
            MowzieGeoBone bone = getMowzieGeoModel().getMowzieBone(name);
            if (bone != null) getMowzieGeoModel().resetBoneToSnapshot(bone);
        }
    }

    @SuppressWarnings("unchecked")
    private static AnimatableManager<EntityUmvuthana> castManager(AnimatableManager<? extends GeoAnimatable> manager) {
        return (AnimatableManager<EntityUmvuthana>) manager;
    }
}
