package com.bobmowzie.mowziesmobs.server.entity.frostmaw;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Created by BobMowzie on 7/20/2017.
 */
public class EntityFrozenController extends Entity {
    public EntityFrozenController(EntityType<? extends EntityFrozenController> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // NOTE 26.1.2 port: see EntityMagicEffect#hurtServer for context on this required override.
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount >= 70 && !isVehicle()) discard() ;
//        List<Entity> passengers = getPassengers();
//        for (Entity passenger : passengers) {
//            if (passenger instanceof LivingEntity) {
//                LivingEntity livingEntity = (LivingEntity)passenger;
//                if (!livingEntity.isPotionActive(PotionHandler.FROZEN)) discard() ;
//            }
//        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canRiderInteract() {
        return false;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity entity) {
        return this.position();
    }

    @Override
    public boolean canBeRiddenUnderFluidType(net.neoforged.neoforge.fluids.FluidType type, Entity rider) {
        return true;
    }

    @Override
    public void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger))
        {
            if (passenger instanceof Player) passenger.setPos(this.getX(), this.getY(), this.getZ());
            else passenger.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        }
    }
}
