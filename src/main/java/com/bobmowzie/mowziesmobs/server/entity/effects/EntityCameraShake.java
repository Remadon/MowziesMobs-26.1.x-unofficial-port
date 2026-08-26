package com.bobmowzie.mowziesmobs.server.entity.effects;

import com.bobmowzie.mowziesmobs.server.entity.EntityHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class EntityCameraShake extends Entity {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(EntityCameraShake.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MAGNITUDE = SynchedEntityData.defineId(EntityCameraShake.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(EntityCameraShake.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE_DURATION = SynchedEntityData.defineId(EntityCameraShake.class, EntityDataSerializers.INT);

    public EntityCameraShake(EntityType<?> type, Level world) {
        super(type, world);
    }

    public EntityCameraShake(Level world, Vec3 position, float radius, float magnitude, int duration, int fadeDuration) {
        super(EntityHandler.CAMERA_SHAKE.get(), world);
        setRadius(radius);
        setMagnitude(magnitude);
        setDuration(duration);
        setFadeDuration(fadeDuration);
        setPos(position.x(), position.y(), position.z());
    }

    public float getShakeAmount(Player player, float delta) {
        float ticksDelta = tickCount + delta;
        float timeFrac = 1.0f - (ticksDelta - getDuration()) / (getFadeDuration() + 1.0f);
        float baseAmount = ticksDelta < getDuration() ? getMagnitude() : timeFrac * timeFrac * getMagnitude();
        Vec3 playerPos = player.getEyePosition(delta);
        float distFrac = (float) (1.0f - Mth.clamp(position().distanceTo(playerPos) / getRadius(), 0, 1));
        return baseAmount * distFrac * distFrac;
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > getDuration() + getFadeDuration()) discard() ;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // NOTE 26.1.2 port: see EntityMagicEffect#hurtServer for context on this required override.
        return false;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 10.0f);
        builder.define(MAGNITUDE, 1.0f);
        builder.define(DURATION, 0);
        builder.define(FADE_DURATION, 5);
    }

    public float getRadius() {
        return getEntityData().get(RADIUS);
    }

    public void setRadius(float radius) {
        getEntityData().set(RADIUS, radius);
    }

    public float getMagnitude() {
        return getEntityData().get(MAGNITUDE);
    }

    public void setMagnitude(float magnitude) {
        getEntityData().set(MAGNITUDE, magnitude);
    }

    public int getDuration() {
        return getEntityData().get(DURATION);
    }

    public void setDuration(int duration) {
        getEntityData().set(DURATION, duration);
    }

    public int getFadeDuration() {
        return getEntityData().get(FADE_DURATION);
    }

    public void setFadeDuration(int fadeDuration) {
        getEntityData().set(FADE_DURATION, fadeDuration);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setRadius(input.getFloatOr("radius", 10.0f));
        setMagnitude(input.getFloatOr("magnitude", 1.0f));
        setDuration(input.getIntOr("duration", 0));
        setFadeDuration(input.getIntOr("fade_duration", 5));
        tickCount = input.getIntOr("ticks_existed", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putFloat("radius", getRadius());
        output.putFloat("magnitude", getMagnitude());
        output.putInt("duration", getDuration());
        output.putInt("fade_duration", getFadeDuration());
        output.putInt("ticks_existed", tickCount);
    }

    public static void cameraShake(Level world, Vec3 position, float radius, float magnitude, int duration, int fadeDuration) {
        if (!world.isClientSide()) {
            EntityCameraShake cameraShake = new EntityCameraShake(world, position, radius, magnitude, duration, fadeDuration);
            world.addFreshEntity(cameraShake);
        }
    }
}
