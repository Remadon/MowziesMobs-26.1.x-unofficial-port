package com.bobmowzie.mowziesmobs.server.entity.effects.geomancy;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class EntityPillarPiece extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> PILLAR = SynchedEntityData.defineId(EntityPillarPiece.class, com.bobmowzie.mowziesmobs.server.entity.EntityHandler.OPTIONAL_UUID.get());
    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(EntityPillarPiece.class, EntityDataSerializers.INT);

    private EntityPillar pillar;

    public EntityPillarPiece(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityPillarPiece(EntityType<?> type, Level level, EntityPillar pillar, Vec3 position) {
        super(type, level);
        this.pillar = pillar;
        setTier(pillar.getTier());
        this.snapTo(position.x, position.y, position.z);
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof EntityPillar || entity instanceof EntityPillarPiece) return false;
        return super.canCollideWith(entity);
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            if (pillar == null) {
                pillar = getPillar();
                if (pillar == null) {
                    remove(RemovalReason.DISCARDED);
                    return;
                }
            }
            if (pillar.isRemoved()) {
                remove(RemovalReason.DISCARDED);
                return;
            }
            if (pillar.isFalling() && pillar.getHeight() + pillar.getY() < getY() + 1) {
                remove(RemovalReason.DISCARDED);
                return;
            }
            setTier(pillar.getTier());
        }

        super.tick();
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        float f = EntityPillar.SIZE_MAP.get(getTier()) / 2.0F;
        return new AABB(position.x - (double)f, position.y, position.z - (double)f, position.x + (double)f, position.y + 1, position.z + (double)f);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(PILLAR, Optional.empty());
        builder.define(TIER, 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // NOTE 26.1.2 port: see EntityMagicEffect#hurtServer for context on this required override.
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("pillar", UUIDUtil.CODEC).ifPresent(this::setPillarUUID);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (pillar != null) output.store("pillar", UUIDUtil.CODEC, pillar.getUUID());
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.BLOCK;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    public Optional<UUID> getPillarUUID() {
        return getEntityData().get(PILLAR);
    }

    public void setPillarUUID(UUID uuid) {
        getEntityData().set(PILLAR, Optional.of(uuid));
    }

    public EntityPillar getPillar() {
        Optional<UUID> uuid = getPillarUUID();
        if (uuid.isPresent() && !level().isClientSide()) {
            return (EntityPillar) ((ServerLevel) level()).getEntity(uuid.get());
        }
        return null;
    }

    public EntityGeomancyBase.GeomancyTier getTier() {
        return EntityGeomancyBase.GeomancyTier.values()[entityData.get(TIER)];
    }

    public void setTier(EntityGeomancyBase.GeomancyTier size) {
        entityData.set(TIER, size.ordinal());
    }
}
