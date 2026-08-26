package com.bobmowzie.mowziesmobs.client.render.entity.player;

import com.bobmowzie.mowziesmobs.client.model.entity.ModelGeckoPlayerFirstPerson;
import com.bobmowzie.mowziesmobs.client.model.entity.ModelGeckoPlayerThirdPerson;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieAnimationController;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * PORTING NOTE (1.21.1 -> 26.1.2): see {@code GeckoRenderPlayer.java}'s class javadoc for the full architecture
 * writeup of this subsystem's redesign. This class itself only needed a handful of GeckoLib-4 -> 5 mechanical fixes:
 * <ul>
 *   <li>{@code GeoRenderer<GeckoPlayer>} (1 type arg) -> {@code GeoRenderer<GeckoPlayer, Void, GeoRenderState>}
 *       (3 type args - see PORTING_NOTES.md GeoRenderer architecture section). {@code GeckoRenderPlayer}/
 *       {@code GeckoFirstPersonRenderer} both now extend {@code GeoObjectRenderer<GeckoPlayer, Void, GeoRenderState>}
 *       (the {@code O} "related object" type isn't used here, so {@code Void}).</li>
 *   <li>{@code MowzieAnimationController}'s constructor dropped the bound-{@code animatable}/easing-value
 *       parameters entirely (confirmed by reading its real GeckoLib-4 -> 5 rewrite, see that class's own porting
 *       note) - {@code new MowzieAnimationController<>(this, name, 0, predicate, 0)} (5 args) ->
 *       {@code new MowzieAnimationController<>(name, 0, predicate)} (3 args).</li>
 *   <li>{@code AnimationState<E>} -> {@code AnimationTest<E>} (record, {@code .controller()} accessor instead of
 *       {@code .getController()}), and {@code AnimationController#transitionLength(int)} -> {@code #setTransitionTicks(int)}.
 *       {@code predicate}'s generic {@code <E extends GeoEntity>} was narrowed to a concrete {@code GeckoPlayer}
 *       parameter so {@code this::predicate} type-checks against {@code AnimationController.AnimationStateHandler<GeckoPlayer>}
 *       (a method reference target can't itself stay generic here).</li>
 *   <li>{@code AnimationController#setLastModel(...)} no longer exists (GeckoLib 5's {@code AnimationController} no
 *       longer tracks a "last model" at all - confirmed via {@code MowzieAnimationController}'s own porting note,
 *       listing it among the fields/methods that "didn't survive"). The two calls to it below have been removed;
 *       there is no replacement, nothing else in this mod reads a controller's "last model".</li>
 *   <li>{@code GeoRendererInternals#getInstanceId(T, O)} now takes 2 args (animatable + related object) instead of 1.</li>
 *   <li>{@code GeckoFirstPersonRenderer}/{@code GeckoRenderPlayer} no longer need an {@code EntityRendererProvider.Context}
 *       at all (they no longer extend any vanilla renderer base class - see their class javadocs), so the manual
 *       {@code Context} construction previously done in {@code initRenderer()} has been removed as dead code.</li>
 *   <li><b>NEW fix (found during a later verification pass, not by the original player-render agent)</b>: the old
 *       {@code @Override public double getTick(Object entity)} (a GeckoLib-4-era {@code IAnimatable}/
 *       {@code GeoAnimatable} hook for a custom per-instance tick counter, used here to feed {@link #tickTimer}) has
 *       NO override target at all in real GeckoLib 5 - grep-confirmed zero {@code getTick} methods anywhere in
 *       {@code GeoAnimatable}/{@code GeoEntity} or anywhere else in the decompiled GeckoLib 5.5.2 source - removed.
 *       <b>Real, verified, NOT-fully-resolved finding (kept {@code implements GeoEntity} rather than switching to
 *       the more semantically-correct {@code GeoAnimatable} - see below for why)</b>: {@code GeoEntity}'s default
 *       methods ({@code triggerAnim}/{@code stopTriggeredAnim}) unconditionally cast {@code this} to
 *       {@code net.minecraft.world.entity.Entity}, which {@code GeckoPlayer} (a plain wrapper object referencing a
 *       {@code Player}, not an {@code Entity} itself - see {@code GeckoRenderPlayer.java}'s own javadoc) is not and
 *       can never safely be - a real, dormant {@code ClassCastException} hazard IF either method is ever called on a
 *       {@code GeckoPlayer} (grep-confirmed: nothing in this codebase currently does, so it's inert today, not
 *       fixed). Switching to {@code implements GeoAnimatable} (the honest supertype) was tried and reverted: it
 *       compiles fine for every generic bound {@code GeckoPlayer} itself is used against ({@code GeoObjectRenderer},
 *       {@code MowzieGeoModel}, {@code MowzieAnimationController}, {@code AnimationTest} - all
 *       {@code <T extends GeoAnimatable>}), BUT {@code server/capability/AbilityData.java#codeAnimations} and
 *       {@code server/ability/Ability.java#codeAnimations}/{@code #animationPredicate} (all out of this file's scope)
 *       are declared {@code <E extends GeoEntity>}/{@code MowzieGeoModel<? extends GeoEntity>} even though their
 *       bodies don't appear to need actual {@code Entity}-ness (read: {@code Ability#animationPredicate} only calls
 *       {@code e.controller().setAnimation(...)}) - switching would break those 2 files' compilation AND this file's
 *       own call to {@code abilityData.animationPredicate(e, getPerspective())} at line ~104, a compile error inside
 *       this very file that can't be resolved without editing files outside this pass's scope. Kept
 *       {@code implements GeoEntity} to avoid that ripple; flagging the dormant-cast-hazard + the real fix (loosen
 *       {@code Ability}/{@code AbilityData}'s two bounds from {@code GeoEntity} to {@code GeoAnimatable}, then switch
 *       this class to {@code implements GeoAnimatable}) for whoever owns {@code server/ability/**}/
 *       {@code server/capability/**} next.</li>
 * </ul>
 */
public abstract class GeckoPlayer implements GeoEntity {

	protected GeoRenderer<GeckoPlayer, Void, GeoRenderState> renderer;
	protected MowzieGeoModel<GeckoPlayer> model;
	protected MowzieAnimationController<GeckoPlayer> controller;

	private int tickTimer = 0;

	private Player player;
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public static final String THIRD_PERSON_CONTROLLER_NAME = "thirdPersonAnimation";
	public static final String FIRST_PERSON_CONTROLLER_NAME = "firstPersonAnimation";

	public enum Perspective {
		FIRST_PERSON,
		THIRD_PERSON
	}

	public GeckoPlayer(Player player) {
		this.player = player;
		setup(player);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controller = new MowzieAnimationController<>(getControllerName(), 0, this::predicate);
		controllers.add(controller);
	}

	public MowzieAnimationController<GeckoPlayer> getController() {
		return controller;
	}

	public Player getPlayer() {
		return player;
	}

	public void tick() {
		tickTimer++;
	}

	private static RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");
	public PlayState predicate(AnimationTest<GeckoPlayer> e) {
		e.controller().setTransitionTicks(0);
		Player player = getPlayer();
		if (player == null) {
			return PlayState.STOP;
		}
		AbilityData abilityData = DataHandler.getData(player, DataHandler.ABILITY_DATA);
		if (abilityData == null) {
			return PlayState.STOP;
		}

		if (abilityData.getActiveAbility() != null) {
			return abilityData.animationPredicate(e, getPerspective());
		}
		else {
			e.setAnimation(IDLE_ANIMATION);
			return PlayState.CONTINUE;
		}
	}

	@Nullable
	public static GeckoPlayer getGeckoPlayer(Player player, Perspective perspective) {
		if (perspective == Perspective.FIRST_PERSON) return GeckoFirstPersonRenderer.GECKO_PLAYER_FIRST_PERSON;
        return DataHandler.getData(player, DataHandler.PLAYER_DATA).getGeckoPlayer();
    }

	public static MowzieAnimationController<GeckoPlayer> getAnimationController(Player player, Perspective perspective) {
        GeckoPlayer geckoPlayer;

		if (perspective == Perspective.FIRST_PERSON) {
			geckoPlayer = GeckoFirstPersonRenderer.GECKO_PLAYER_FIRST_PERSON;
		} else {
			geckoPlayer = DataHandler.getData(player, DataHandler.PLAYER_DATA).getGeckoPlayer();
		}

        if (geckoPlayer != null) {
            return geckoPlayer.controller;
        }

        return null;
    }

	public GeoRenderer<GeckoPlayer, Void, GeoRenderState> getPlayerRenderer() {
		return renderer;
	}

	public MowzieGeoModel<GeckoPlayer> getModel() {
		return model;
	}

	public abstract String getControllerName();

	public abstract Perspective getPerspective();

	public abstract void setup(Player player);

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	public static class GeckoPlayerFirstPerson extends GeckoPlayer {
		public GeckoPlayerFirstPerson(Player player) {
			super(player);
		}

		@Override
		public String getControllerName() {
			return FIRST_PERSON_CONTROLLER_NAME;
		}

		@Override
		public Perspective getPerspective() {
			return Perspective.FIRST_PERSON;
		}

		@Override
		public void setup(Player player) {
			ModelGeckoPlayerFirstPerson modelGeckoPlayer = new ModelGeckoPlayerFirstPerson();
			model = modelGeckoPlayer;
			GeckoFirstPersonRenderer geckoRenderer = new GeckoFirstPersonRenderer(modelGeckoPlayer);
			renderer = geckoRenderer;
			if (!geckoRenderer.getModelsToLoad().containsKey(this.getClass())) {
				geckoRenderer.getModelsToLoad().put(this.getClass(), geckoRenderer);
			}

			getAnimatableInstanceCache().getManagerForId(renderer.getInstanceId(this, null));
			GeckoFirstPersonRenderer.GECKO_PLAYER_FIRST_PERSON = this;
		}
	}

	public static class GeckoPlayerThirdPerson extends GeckoPlayer {
		public static GeckoRenderPlayer GECKO_RENDERER_THIRD_PERSON_NORMAL;
		public static ModelGeckoPlayerThirdPerson GECKO_MODEL_THIRD_PERSON_NORMAL;
		public static GeckoRenderPlayer GECKO_RENDERER_THIRD_PERSON_SLIM;
		public static ModelGeckoPlayerThirdPerson GECKO_MODEL_THIRD_PERSON_SLIM;

		protected GeoRenderer<GeckoPlayer, Void, GeoRenderState> rendererSlim;
		protected MowzieGeoModel<GeckoPlayer> modelSlim;

		public GeckoPlayerThirdPerson(Player player) {
			super(player);
		}

		@Override
		public String getControllerName() {
			return THIRD_PERSON_CONTROLLER_NAME;
		}

		@Override
		public Perspective getPerspective() {
			return Perspective.THIRD_PERSON;
		}

		@Override
		public void setup(Player player) {
			model = GECKO_MODEL_THIRD_PERSON_NORMAL;
			renderer = GECKO_RENDERER_THIRD_PERSON_NORMAL;
			modelSlim = GECKO_MODEL_THIRD_PERSON_SLIM;
			rendererSlim = GECKO_RENDERER_THIRD_PERSON_SLIM;

			getAnimatableInstanceCache().getManagerForId(renderer.getInstanceId(this, null));
		}

		public boolean isPlayerSlim() {
			return ((AbstractClientPlayer) getPlayer()).getSkin().model().name().equals("SLIM");
		}

		@Override
		public MowzieGeoModel<GeckoPlayer> getModel() {
			return isPlayerSlim() ? modelSlim : model;
		}

		@Override
		public GeoRenderer<GeckoPlayer, Void, GeoRenderState> getPlayerRenderer() {
			return isPlayerSlim() ? rendererSlim : renderer;
		}

		/**
		 * PORTING NOTE: previously built a manual {@code EntityRendererProvider.Context} to pass to
		 * {@code GeckoRenderPlayer}'s (then vanilla-{@code PlayerRenderer}-extending) constructor. Neither
		 * {@code GeckoRenderPlayer} nor {@code GeckoFirstPersonRenderer} extend a vanilla renderer class any more
		 * (see {@code GeckoRenderPlayer}'s class javadoc for why - they're plain {@code GeoObjectRenderer}s now),
		 * so no {@code Context} is needed here at all.
		 */
		public static void initRenderer() {
			GECKO_MODEL_THIRD_PERSON_NORMAL = new ModelGeckoPlayerThirdPerson();
			GECKO_MODEL_THIRD_PERSON_SLIM = new ModelGeckoPlayerThirdPerson();
			GECKO_MODEL_THIRD_PERSON_SLIM.setUseSmallArms(true);

			GeckoRenderPlayer geckoRenderer = new GeckoRenderPlayer(GECKO_MODEL_THIRD_PERSON_NORMAL);
			if (!geckoRenderer.getModelsToLoad().containsKey(GeckoPlayer.GeckoPlayerThirdPerson.class)) {
				geckoRenderer.getModelsToLoad().put(GeckoPlayer.GeckoPlayerThirdPerson.class, geckoRenderer);
			}
			GECKO_RENDERER_THIRD_PERSON_NORMAL = geckoRenderer;

			GeckoRenderPlayer geckoRendererSlim = new GeckoRenderPlayer(GECKO_MODEL_THIRD_PERSON_SLIM);
			if (!geckoRendererSlim.getModelsToLoad().containsKey(GeckoPlayer.GeckoPlayerThirdPerson.class)) {
				geckoRendererSlim.getModelsToLoad().put(GeckoPlayer.GeckoPlayerThirdPerson.class, geckoRendererSlim);
			}
			GECKO_RENDERER_THIRD_PERSON_SLIM = geckoRendererSlim;
		}
	}
}
