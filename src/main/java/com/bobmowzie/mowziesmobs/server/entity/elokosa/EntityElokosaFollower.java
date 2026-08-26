package com.bobmowzie.mowziesmobs.server.entity.elokosa;

import com.bobmowzie.mowziesmobs.server.item.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class EntityElokosaFollower<L extends LivingEntity> extends EntityElokosa {
    protected static final Optional<UUID> ABSENT_LEADER = Optional.empty();

    private static final EntityDataAccessor<Optional<UUID>> LEADER = SynchedEntityData.defineId(EntityElokosaFollower.class, com.bobmowzie.mowziesmobs.server.entity.EntityHandler.OPTIONAL_UUID.get());

    private final Class<L> leaderClass;

    public int index;

    protected L leader;

    public boolean shouldSetDead;

    public EntityElokosaFollower(EntityType<? extends EntityElokosaFollower> type, Level world, Class<L> leaderClass) {
        this(type, world, leaderClass, null);
    }

    public EntityElokosaFollower(EntityType<? extends EntityElokosaFollower> type, Level world, Class<L> leaderClass, L leader) {
        super(type, world);
        this.leaderClass = leaderClass;
        if (leader != null) {
            setLeaderUUID(leader.getUUID());
        }
        shouldSetDead = false;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(7, new MoveTowardsRestrictionGoal(this, 1.0));
    }

    @Override
    protected void defineSynchedData(@NotNull SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LEADER, ABSENT_LEADER);
    }

    public Optional<UUID> getLeaderUUID() {
        return getEntityData().get(LEADER);
    }

    public void setLeaderUUID(UUID uuid) {
        setLeaderUUID(Optional.of(uuid));
    }

    public void setLeaderUUID(Optional<UUID> uuid) {
        getEntityData().set(LEADER, uuid);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ItemHandler.ELOKOSA_SPAWN_EGG.get());
    }

    @Override
    public void tick() {
        super.tick();
        if (leader == null && getLeaderUUID().isPresent()) {
            leader = getLeader();
            if (leader != null) {
                addAsPackMember();
            }
        }
        if (shouldSetDead) discard();

        if (leader != null) {
            setHomeTo(leader.getOnPos(), 16);
        }
        else {
            clearHome();
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (leader != null) {
            removeAsPackMember();
        }
    }

    public void setShouldSetDead() {
        shouldSetDead = true;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (leader != null) {
            removeAsPackMember();
        }
        super.remove(reason);
    }

    public L getLeader() {
        Optional<UUID> uuid = getLeaderUUID();
        if (uuid.isPresent()) {
            List<L> potentialLeaders = level().getEntitiesOfClass(leaderClass, getBoundingBox().inflate(32, 32, 32));
            for (L entity : potentialLeaders) {
                if (uuid.get().equals(entity.getUUID())) {
                    return entity;
                }
            }
        }
        return null;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return leader != null;
    }

    protected abstract int getPackSize();

    protected abstract void addAsPackMember();

    protected abstract void removeAsPackMember();

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        Optional<UUID> leader = getLeaderUUID();
        if (leader.isPresent()) {
            output.putString("leaderUUID", leader.get().toString());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String uuid = input.getStringOr("leaderUUID", "");
        if (uuid.isEmpty()) {
            setLeaderUUID(ABSENT_LEADER);
        } else {
            setLeaderUUID(UUID.fromString(uuid));
        }
    }

    @Override
    public boolean hasHome() {
        return getLeader() != null;
    }

    @Override
    public @NotNull BlockPos getHomePosition() {
        if (getLeader() != null) {
            return getLeader().getOnPos();
        }
        return super.getHomePosition();
    }
}
