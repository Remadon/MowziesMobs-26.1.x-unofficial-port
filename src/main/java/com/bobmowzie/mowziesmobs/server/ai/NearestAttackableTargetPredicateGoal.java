package com.bobmowzie.mowziesmobs.server.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;


public class NearestAttackableTargetPredicateGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    // PORTING NOTE (1.21.1 -> 26.1.2): NearestAttackableTargetGoal#targetConditions is now `protected final` (it
    // used to be assignable after construction), so it can no longer be wholesale-replaced with a fully custom
    // TargetingConditions object post-super(...) - confirmed against the real vanilla source. It also can't be
    // built via a constructor argument since every NearestAttackableTargetGoal constructor hardcodes
    // `TargetingConditions.forCombat().range(getFollowDistance())` internally and only accepts a plain Selector,
    // discarding any custom forCombat()/forNonCombat() choice or custom .range(...) value a caller might set (this
    // mod's callers use both: EntityUmvuthanaCraneToPlayer/EntityUmvuthanaCrane pass forNonCombat() with custom
    // ranges). To preserve the exact custom TargetingConditions callers pass in, this class now keeps its own
    // field and overrides findTarget() (protected, still overridable) to replicate
    // NearestAttackableTargetGoal#findTarget()'s logic verbatim but against `this.predicate` instead of the
    // superclass's own (unrelated, unused-by-us) targetConditions.
    private final TargetingConditions predicate;

    public NearestAttackableTargetPredicateGoal(Mob goalOwnerIn, Class targetClassIn, int targetChanceIn, boolean checkSight, boolean nearbyOnlyIn, TargetingConditions predicate) {
        super(goalOwnerIn, targetClassIn, targetChanceIn, checkSight, nearbyOnlyIn, null);
        this.predicate = predicate;
    }

    @Override
    protected void findTarget() {
        ServerLevel level = getServerLevel(this.mob);
        if (this.targetType != Player.class && this.targetType != ServerPlayer.class) {
            this.target = level.getNearestEntity(
                    this.mob.level().getEntitiesOfClass(this.targetType, this.getTargetSearchArea(this.getFollowDistance()), entity -> true),
                    this.predicate,
                    this.mob,
                    this.mob.getX(),
                    this.mob.getEyeY(),
                    this.mob.getZ()
            );
        } else {
            this.target = level.getNearestPlayer(this.predicate, this.mob, this.mob.getX(), this.mob.getEyeY(), this.mob.getZ());
        }
    }
}
