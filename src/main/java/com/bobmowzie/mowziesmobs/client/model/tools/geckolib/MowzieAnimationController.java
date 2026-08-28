package com.bobmowzie.mowziesmobs.client.model.tools.geckolib;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.cache.animation.Animation;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.ClientUtil;
import org.jetbrains.annotations.TestOnly;

/**
 * PORTING NOTE (GeckoLib 4 -> 5): {@code AnimationController}'s internals were completely rewritten between these
 * versions - it no longer stores a bound {@code animatable}, {@code tickOffset}, {@code shouldResetTick},
 * {@code isJustStarting}, {@code animationQueue}/{@code currentAnimation}, {@code animationSpeedModifier}, a
 * {@code State} enum, or a {@code lastModel} field, and there is no more overridable {@code adjustTick(double)}
 * method - none of that survived. It's now built around an {@code AnimationTimeline}/{@code timelineTime} model
 * instead (see {@code com.geckolib.animation.AnimationController} real source, fetched from the GeckoLib GitHub
 * repo's {@code 26.1} branch for 5.5.2 during this port, since the previously-provided decompiled sources jar was
 * unavailable when this file was written).
 * <p>
 * This class is reimplemented against that new model. Its purpose (per the original code comment, preserved below)
 * is a workaround so that an animation started via server-driven behavior/ability logic while an entity is
 * off-screen (and therefore not receiving per-frame {@code AnimationStateHandler} calls) begins "already in
 * progress" the moment the entity starts rendering, rather than restarting from time zero. The new equivalent is to
 * force-initialize the controller's timeline immediately (via the now-{@code protected} {@code initializeNewAnimation})
 * instead of waiting for the next natural {@code checkControllerState} pass.
 * <p>
 * UNVERIFIED: this could not be compiled/run to confirm exact timing fidelity against the old behavior (see porting
 * report). All call sites of the old {@code playAnimation}/constructor are outside this agent's scope
 * (server/ability/**, server/entity/**, client/render/entity/player/** per a repo-wide grep) and will need to be
 * updated to the new signatures below.
 */
public class MowzieAnimationController<T extends GeoAnimatable> extends AnimationController<T> {

    public MowzieAnimationController(String name, int transitionTicks, AnimationController.AnimationStateHandler<T> animationHandler) {
        super(name, transitionTicks, animationHandler);
    }

    // Normally, mobs won't start playing an animation until they are rendering on screen, even if the behavior starts off-screen
    // This combination of playAnimation and forcing timeline initialization fixes this issue

    /**
     * Immediately starts the given animation on this controller, bypassing the normal one-frame delay before a
     * newly-set animation's {@link com.geckolib.animation.state.AnimationTimeline} is established.
     *
     * @param animatable  The animatable this controller belongs to
     * @param renderState The current {@link GeoRenderState} for the upcoming render pass
     * @param manager     This animatable's {@link AnimatableManager}
     * @param geoModel    This animatable's {@link GeoModel}
     * @param animation   The animation to start playing
     */
    public void playAnimation(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, GeoModel<T> geoModel, RawAnimation animation) {
        reset();
        setAnimation(animation);
        // FOLLOW-UP FIX (confirmed live via debug logging: a freshly-started animation's timeline was observed
        // jumping straight to its own end - e.g. timelineTime landing on a 1.64s clip's exact 1.64 total length -
        // on the very first real tick after this force-init, meaning the whole clip played for zero visible
        // frames): AnimationController#checkControllerState advances the timeline each call by
        // (currentAge - lastAnimatableAge), clamped to the clip's length - and lastAnimatableAge is a controller
        // field that's only ever refreshed *inside* checkControllerState itself, once per call. This player's
        // third-person controller only receives checkControllerState calls while GeckoRenderPlayer is actively
        // driving the pose, which (see ClientEventHandler's RenderPlayerEvent.Pre gating) is only while an ability
        // is active - vanilla rendering (and therefore no controller ticking at all) resumes the rest of the time.
        // So lastAnimatableAge sits frozen at whatever tick the previous ability last rendered a frame at, however
        // long ago that was; the next time any ability starts and this force-init runs, the immediately-following
        // real checkControllerState call computes (now - that stale, possibly many-seconds-old value) as its
        // delta - almost always larger than the whole clip - and the timeline clamp instantly jumps to the end.
        // Resyncing lastAnimatableAge to "now" here means that first real call instead measures only the true
        // elapsed time since this force-init (at most a few milliseconds), which is what lets the clip actually
        // play through its keyframes instead of skipping straight to its final pose.
        this.lastAnimatableAge = ClientUtil.getCurrentTick();
        initializeNewAnimation(animatable, renderState, geoModel, getAnimationSpeed(), getTransitionTicks());
    }

    /**
     * Dev-tooling helper (see {@code @TestOnly}): if the currently-playing animation's baked definition has since
     * changed (e.g. via an in-game animation reload), restart it from the new definition.
     */
    @TestOnly
    public void checkAndReloadAnims(T animatable, GeoRenderState renderState, AnimatableManager<T> manager, GeoModel<T> geoModel) {
        if (getCurrentAnimationPoint() != null && getCurrentAnimationPoint().animation() != null) {
            Animation currentAnimation = getCurrentAnimationPoint().animation();
            Animation reloadedAnimation = geoModel.getBakedAnimation(animatable, currentAnimation.name());

            if (reloadedAnimation != null && !reloadedAnimation.equals(currentAnimation)) {
                RawAnimation rawAnimation = getCurrentRawAnimation();

                reset();

                if (rawAnimation != null) {
                    setAnimation(rawAnimation);
                    initializeNewAnimation(animatable, renderState, geoModel, getAnimationSpeed(), getTransitionTicks());
                }
            }
        }
    }
}
