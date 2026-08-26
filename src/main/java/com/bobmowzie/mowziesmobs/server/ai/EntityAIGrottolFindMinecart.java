package com.bobmowzie.mowziesmobs.server.ai;

import com.bobmowzie.mowziesmobs.server.entity.grottol.EntityGrottol;
import com.google.common.base.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.minecart.Minecart;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class EntityAIGrottolFindMinecart extends Goal {
    private final EntityGrottol grottol;

    private final Comparator<Entity> sorter;

    private final Predicate<Minecart> predicate;

    private Minecart minecart;

    private int time;

    public EntityAIGrottolFindMinecart(EntityGrottol grottol) {
        this.grottol = grottol;
        this.sorter = Comparator.comparing(grottol::distanceToSqr);
        this.predicate = minecart -> minecart != null && minecart.isAlive() && !minecart.isVehicle() && EntityGrottol.isBlockRail(minecart.level().getBlockState(minecart.blockPosition()).getBlock());
        setFlags(EnumSet.of(Flag.LOOK, Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (grottol.fleeTime <= 1) return false;
        List<Minecart> minecarts = grottol.level().getEntitiesOfClass(Minecart.class, grottol.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), predicate);
        minecarts.sort(sorter);
        if (minecarts.isEmpty()) {
            return false;
        }
        minecart = minecarts.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return predicate.test(minecart) && time < 1200 && !grottol.isInMinecart();
    }

    @Override
    public void start() {
        time = 0;
        grottol.getNavigation().moveTo(minecart, 0.5D);
    }

    @Override
    public void stop() {
        grottol.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (grottol.distanceToSqr(minecart) > 1.45D * 1.45D) {
            grottol.getLookControl().setLookAt(minecart, 10.0F, grottol.getMaxHeadXRot());
            if (++time % 40 == 0) {
                grottol.getNavigation().moveTo(minecart, 0.5D);
            }
        } else {
            // PORTING NOTE (1.21.1 -> 26.1.2): Entity#startRiding(Entity, boolean force) 2-arg overload is gone -
            // only startRiding(Entity) (force=false) and startRiding(Entity, boolean force, boolean
            // sendEventAndTriggers) remain (confirmed against real 26.1.2 Entity source). Using the 3-arg form with
            // sendEventAndTriggers=true (vanilla's own default for the force=true case) to preserve force=true.
            grottol.startRiding(minecart, true, true);
            if (minecart.getHurtTime() == 0) {
                minecart.setHurtDir(-minecart.getHurtDir());
                minecart.setHurtTime(10);
                minecart.setDamage(50.0F);
            }
        }
    }
}
