package com.bobmowzie.mowziesmobs.server.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class TestEntity extends Entity {
    public TestEntity(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    public TestEntity(EntityType<?> p_19870_, Level p_19871_, Vec3 position) {
        super(p_19870_, p_19871_);
        this.setPos(position);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount >= 20) remove(RemovalReason.DISCARDED);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {

    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_20052_) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_20139_) {

    }
}
