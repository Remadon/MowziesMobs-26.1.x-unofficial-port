package com.bobmowzie.mowziesmobs.server.entity.effects;

import com.bobmowzie.mowziesmobs.server.entity.ILinkedEntity;
import com.bobmowzie.mowziesmobs.server.message.MessageLinkEntities;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by BobMowzie on 9/2/2018.
 */
public abstract class EntityMagicEffect extends Entity implements ILinkedEntity {
    private LivingEntity cachedCaster;
    protected boolean hasSyncedCaster = false;
    private static final EntityDataAccessor<Optional<UUID>> CASTER = SynchedEntityData.defineId(EntityMagicEffect.class, com.bobmowzie.mowziesmobs.server.entity.EntityHandler.OPTIONAL_UUID.get());

    public EntityMagicEffect(EntityType<? extends EntityMagicEffect> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityMagicEffect(EntityType<? extends EntityMagicEffect> type, Level world, LivingEntity caster) {
        super(type, world);
        if (!world.isClientSide() && caster != null) {
            this.setCasterID(caster.getUUID());
        }
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        builder.define(CASTER, Optional.empty());
    }

    public Optional<UUID> getCasterID() {
        return getEntityData().get(CASTER);
    }

    public void setCasterID(UUID id) {
        getEntityData().set(CASTER, Optional.of(id));
    }

    public LivingEntity getCaster() {
        if (this.cachedCaster != null && !this.cachedCaster.isRemoved()) {
            return this.cachedCaster;
        } else if (this.getCasterID().isPresent() && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.getCasterID().get());
            if (entity instanceof LivingEntity) {
                cachedCaster = (LivingEntity) entity;
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, MessageLinkEntities.fromEntity(this, cachedCaster));
            }
            return this.cachedCaster;
        } else {
            return null;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // NOTE 26.1.2 port: Entity#hurt(DamageSource, float) was final/no-op-by-default pre-port; the new abstract
        // Entity#hurtServer(ServerLevel, DamageSource, float) has no default implementation, so every concrete leaf
        // class must provide one. All EntityMagicEffect subclasses were relying on the old inert default (magic
        // effect entities can't be "hurt" via the normal damage pipeline), so that default is reproduced here once
        // for the whole hierarchy rather than duplicated in every leaf subclass.
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void push(Entity entityIn) {
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
//        if (!level().isClientSide() && getCasterID().isPresent() && cachedCaster == null) {
//            Entity casterEntity = ((ServerLevel)this.level()).getEntity(getCasterID().get());
//            if (casterEntity instanceof LivingEntity) {
//                cachedCaster = (LivingEntity) casterEntity;
//                MowziesMobs.NETWORK.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this), new MessageLinkEntities(this, cachedCaster));
//            }
//            hasSyncedCaster = true;
//        }
    }

    @Override
    public void link(Entity entity) {
        if (entity instanceof LivingEntity) {
            cachedCaster = (LivingEntity) entity;
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("caster", UUIDUtil.CODEC).ifPresent(this::setCasterID);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (getCasterID().isPresent()) {
            output.store("caster", UUIDUtil.CODEC, getCasterID().get());
        }
    }

    public List<Entity> getEntitiesNearby(double radius) {
        return getEntitiesNearby(Entity.class, radius);
    }

    public <T extends Entity> List<T> getEntitiesNearby(Class<T> entityClass, double r) {
        return level().getEntitiesOfClass(entityClass, getBoundingBox().inflate(r, r, r), e -> e != this && distanceTo(e) <= r + e.getBbWidth() / 2f);
    }

    public <T extends Entity> List<T> getEntitiesNearbyCube(Class<T> entityClass, double r) {
        return level().getEntitiesOfClass(entityClass, getBoundingBox().inflate(r, r, r), e -> e != this);
    }

    public boolean raytraceCheckEntity(Entity entity) {
        Vec3 from = this.position();
        int numChecks = 3;
        for (int i = 0; i < numChecks; i++) {
            float increment = entity.getBbHeight() / (numChecks + 1);
            Vec3 to = entity.position().add(0, increment * (i + 1), 0);
            BlockHitResult result = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (result.getType() != HitResult.Type.BLOCK) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(@NotNull ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, entity, cachedCaster == null ? 0 : cachedCaster.getId());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Entity entity = this.level().getEntity(packet.getData());
        if (entity instanceof LivingEntity) {
            cachedCaster = (LivingEntity) entity;
        }
    }
}
