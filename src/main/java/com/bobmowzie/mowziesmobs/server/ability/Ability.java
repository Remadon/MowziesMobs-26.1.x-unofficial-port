package com.bobmowzie.mowziesmobs.server.ability;

import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieAnimationController;
import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoPlayer;
import com.bobmowzie.mowziesmobs.server.ability.AbilitySection.AbilitySectionDuration;
import com.bobmowzie.mowziesmobs.server.ability.AbilitySection.AbilitySectionInstant;
import com.bobmowzie.mowziesmobs.server.capability.AbilityData;
import com.bobmowzie.mowziesmobs.server.capability.DataHandler;
import com.bobmowzie.mowziesmobs.server.entity.MowzieGeckoEntity;
import com.bobmowzie.mowziesmobs.server.potion.EffectHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.RawAnimation;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.util.RenderUtil;

import java.util.List;
import java.util.Random;

public class Ability<T extends LivingEntity> {
    private final AbilitySection[] sectionTrack;
    protected int cooldownMax;
    private final AbilityType<T, ? extends Ability> abilityType;
    private final T user;
    private final AbilityData abilityData;

    private int ticksInUse;
    private int ticksInSection;
    private int currentSectionIndex;
    private boolean isUsing;
    private int cooldownTimer;

    protected Random rand;

    protected RawAnimation activeAnimation;

    public Ability(AbilityType<T, ? extends Ability> abilityType, T user, AbilitySection[] sectionTrack, int cooldownMax) {
        this.abilityType = abilityType;
        this.user = user;
        this.abilityData = DataHandler.getData(user, DataHandler.ABILITY_DATA);
        this.sectionTrack = sectionTrack;
        this.cooldownMax = cooldownMax;
        this.rand = new Random();
    }

    public Ability(AbilityType<T, ? extends Ability> abilityType, T user, AbilitySection[] sectionTrack) {
        this(abilityType, user, sectionTrack, 0);
    }

    public void start() {
        if (!runsInBackground()) abilityData.setActiveAbility(this);
        ticksInUse = 0;
        ticksInSection = 0;
        currentSectionIndex = 0;
        isUsing = true;
        beginSection(getSectionTrack()[0]);
    }

    // PORTING NOTE (GeckoLib 4 -> 5): MowzieAnimationController#playAnimation's signature grew a GeoRenderState,
    // AnimatableManager<T> and GeoModel<T> parameter (see that class's own javadoc) - this call site (ability
    // start/tick logic, NOT inside an active render pass) has none of those readily available. Analysis of the real
    // GeckoLib 5.5.2 AnimationController#initializeNewAnimation source: playAnimation() always calls reset() first,
    // which forces triggeredAnimTime = -1, so the only branch that would actually dereference renderState (the
    // "triggered animation replay" branch, triggeredAnimTime >= 0) never executes on this call path - a throwaway
    // GeoRenderState.Impl is safe here. The AnimatableManager<T> argument is likewise unused inside playAnimation's
    // own body (only passed through, never read), but still constructed correctly via the entity's own
    // AnimatableInstanceCache for correctness/robustness against a future GeckoLib update. GeoModel<T> IS
    // load-bearing (used to resolve baked bone animation curves), so it's fetched from the entity's actual live
    // GeckoLib renderer via RenderUtil.getGeckoLibEntityRenderer(EntityType) (the same lookup GeckoLib's own
    // GeoRenderProvider/RenderUtil helpers use elsewhere) rather than guessed at. UNVERIFIED at runtime (could not
    // compile/run the game during this port) - flagged per the model/render agent's cross-scope request in
    // MowzieAnimationController's javadoc.
    public void playAnimation(RawAnimation animation) {
        if (getUser() instanceof MowzieGeckoEntity && getUser().level().isClientSide()) {
            MowzieGeckoEntity entity = (MowzieGeckoEntity) getUser();
            activeAnimation = animation;
            MowzieAnimationController<MowzieGeckoEntity> controller = entity.getController();
            if (controller != null) {
                GeoEntityRenderer<?, ?> renderer = RenderUtil.getGeckoLibEntityRenderer(entity.getType());
                if (renderer != null) {
                    @SuppressWarnings("unchecked")
                    GeoModel<MowzieGeckoEntity> geoModel = (GeoModel<MowzieGeckoEntity>) renderer.getGeoModel();
                    AnimatableManager<MowzieGeckoEntity> manager = entity.getAnimatableInstanceCache().getManagerForId(entity.getId());
                    controller.playAnimation(entity, new GeoRenderState.Impl(), manager, geoModel, animation);
                }
            }
        }
    }

    public void tick() {
        if (isUsing()) {
            if (getUser().isEffectiveAi() && !canContinueUsing()) AbilityHandler.INSTANCE.sendInterruptAbilityMessage(getUser(), this.abilityType);

            tickUsing();

            ticksInUse++;
            ticksInSection++;
            AbilitySection section = getCurrentSection();
            if (section instanceof AbilitySectionInstant) {
                nextSection();
            } else if (section instanceof AbilitySectionDuration) {
                AbilitySectionDuration sectionDuration = (AbilitySectionDuration) section;
                if (ticksInSection > sectionDuration.duration) nextSection();
            }
        }
        else {
            tickNotUsing();
            if (getCooldownTimer() > 0) cooldownTimer--;
        }
    }

    public void tickUsing() {

    }

    public void tickNotUsing() {

    }

    public void end() {
        ticksInUse = 0;
        ticksInSection = 0;
        isUsing = false;
        cooldownTimer = getMaxCooldown();
        currentSectionIndex = 0;
        if (!runsInBackground()) abilityData.setActiveAbility(null);
    }

    public void interrupt() {
        end();
    }

    public void complete() {
        end();
    }

    /**
     * Server-only check to see if the user can use this ability. Checked before packet is sent.
     * @return Whether or not the ability can be used
     */
    public boolean canUse() {
        if (getUser().hasEffect(EffectHandler.FROZEN)) return false;
        boolean toReturn = (!isUsing() || canCancelSelf()) && cooldownTimer == 0;
        if (!runsInBackground()) toReturn = toReturn && (abilityData.getActiveAbility() == null || canCancelActiveAbility() || abilityData.getActiveAbility().canBeCanceledByAbility(this));
        return toReturn;
    }

    /**
     * Both sides check and behavior when user tries to use this ability. Ability<?>only starts if this returns true.
     * Called after packet is received.
     * @return Whether or not the ability try succeeded
     */
    public boolean tryAbility() {
        return true;
    }

    public boolean canCancelActiveAbility() {
        return false;
    }

    public Ability<?> getActiveAbility() {
        AbilityData data = getAbilityCapability();
        if (data == null) return null;
        return getAbilityCapability().getActiveAbility();
    }

    public boolean canCancelSelf() {
        return false;
    }

    public boolean canBeCanceledByAbility(Ability<?> ability) {
        return false;
    }

    protected boolean canContinueUsing() {
        return !getUser().hasEffect(EffectHandler.FROZEN);
    }

    public boolean isUsing() {
        return isUsing;
    }

    public T getUser() {
        return user;
    }

    public Level getLevel() {
        return user.level();
    }

    public int getTicksInUse() {
        return ticksInUse;
    }

    public int getTicksInSection() {
        return ticksInSection;
    }

    public int getCooldownTimer() {
        return cooldownTimer;
    }

    public void nextSection() {
        jumpToSection(currentSectionIndex + 1);
    }

    public void jumpToSection(int sectionIndex) {
        endSection(getCurrentSection());
        currentSectionIndex = sectionIndex;
        ticksInSection = 0;
        if (currentSectionIndex >= getSectionTrack().length) {
            complete();
        }
        else {
            beginSection(getCurrentSection());
        }
    }

    protected void endSection(AbilitySection section) {

    }

    protected void beginSection(AbilitySection section) {

    }

    public AbilitySection getCurrentSection() {
        if (currentSectionIndex >= getSectionTrack().length) return null;
        return getSectionTrack()[currentSectionIndex];
    }

    public int getCurrentSectionIndex() {
        return currentSectionIndex;
    }

    public boolean damageInterrupts() {
        return false;
    }

    public int damageInterruptThreshold() {
        return 3;
    }

    // PORTING NOTE (1.21.1 -> 26.1.2): LivingDamageEvent.Post#getNewDamage() no longer exists - the closest
    // equivalent on the rewritten event (which now exposes originalDamage/inflictedDamage/healthDamage/
    // blockedDamage/shieldDamage separately) is getHealthDamage(), the actual damage that landed on the entity's
    // health after all reductions - matches the old "final applied damage" semantics of getNewDamage().
    public void onTakeDamage(LivingDamageEvent.Post event) {
        if (isUsing() && event.getHealthDamage() >= damageInterruptThreshold() && damageInterrupts()) AbilityHandler.INSTANCE.sendInterruptAbilityMessage(getUser(), getAbilityType());
    }

    /**
     * Non-background abilities require no other non-background abilities running to run.
     * Only one non-background ability can run at once.
     * Background abilities can all run simultaneously
     * @return
     */
    public boolean runsInBackground() {
        return false;
    }

    /**
     * Unused for background abilities
     * @return
     */
    public boolean preventsAttacking() {
        return true;
    }

    /**
     * Unused for background abilities
     * @return
     */
    public boolean preventsBlockBreakingBuilding() {
        return true;
    }

    /**
     * Unused for background abilities
     * @return
     */
    public boolean preventsInteracting() {
        return true;
    }

    /**
     * Unused for background abilities
     * @return
     */
    public boolean preventsItemUse(ItemStack stack) {
        return true;
    }

    public AbilitySection[] getSectionTrack() {
        return sectionTrack;
    }

    public int getMaxCooldown() {
        return cooldownMax;
    }

    public AbilityData getAbilityCapability() {
        return abilityData;
    }

    public <E extends GeoEntity> PlayState animationPredicate(AnimationTest<E> e, GeckoPlayer.Perspective perspective) {
        if (activeAnimation == null || activeAnimation.getAnimationStages().isEmpty())
            return PlayState.STOP;
        e.controller().setAnimation(activeAnimation);
        return PlayState.CONTINUE;
    }

    public void codeAnimations(MowzieGeoModel<? extends GeoEntity> model, float partialTick) {

    }

    public boolean isAnimating() {
        return isUsing();
    }

    public AbilityType<T, ? extends Ability> getAbilityType() {
        return abilityType;
    }

    public List<LivingEntity> getEntityLivingBaseNearby(LivingEntity player, double distanceX, double distanceY, double distanceZ, double radius) {
        return getEntitiesNearby(player, LivingEntity.class, distanceX, distanceY, distanceZ, radius);
    }

    public <T extends Entity> List<T> getEntitiesNearby(LivingEntity player, Class<T> entityClass, double r) {
        return player.level().getEntitiesOfClass(entityClass, player.getBoundingBox().inflate(r, r, r), e -> e != player && player.distanceTo(e) <= r);
    }

    public <T extends Entity> List<T> getEntitiesNearby(LivingEntity player, Class<T> entityClass, double dX, double dY, double dZ, double r) {
        return player.level().getEntitiesOfClass(entityClass, player.getBoundingBox().inflate(dX, dY, dZ), e -> e != player && player.distanceTo(e) <= r);
    }

    public CompoundTag writeNBT() {
        CompoundTag compound = new CompoundTag();
        if (isUsing()) {
            compound.putInt("ticks_in_use", ticksInUse);
            compound.putInt("ticks_in_section", ticksInSection);
            compound.putInt("current_section", currentSectionIndex);
        }
        else if (cooldownTimer > 0) {
            compound.putInt("cooldown_timer", cooldownTimer);
        }
        return compound;
    }

    public void readNBT(Tag nbt) {
        CompoundTag compound = (CompoundTag) nbt;
        isUsing = compound.contains("ticks_in_use");
        if (isUsing) {
            ticksInUse = compound.getIntOr("ticks_in_use", 0);
            ticksInSection = compound.getIntOr("ticks_in_section", 0);
            currentSectionIndex = compound.getIntOr("current_section", 0);
        }
        else {
            cooldownTimer = compound.getIntOr("cooldown_timer", 0);
        }
    }

    // Client events
    public void onRenderTick(RenderFrameEvent event) {

    }
}
