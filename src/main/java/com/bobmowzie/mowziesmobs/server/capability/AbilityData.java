package com.bobmowzie.mowziesmobs.server.capability;

import com.bobmowzie.mowziesmobs.client.model.tools.geckolib.MowzieGeoModel;
import com.bobmowzie.mowziesmobs.client.render.entity.player.GeckoPlayer;
import com.bobmowzie.mowziesmobs.server.ability.Ability;
import com.bobmowzie.mowziesmobs.server.ability.AbilityHandler;
import com.bobmowzie.mowziesmobs.server.ability.AbilityType;
import com.bobmowzie.mowziesmobs.server.entity.MowzieGeckoEntity;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.*;

public class AbilityData implements ValueIOSerializable {
    SortedMap<AbilityType<?, ?>, Ability<?>> abilityInstances = new TreeMap<>();
    Ability<?> activeAbility = null;
    Map<String, Tag> nbtMap = new HashMap<>();
    private boolean needsReinstancing = true;

    public void instanceAbilities(LivingEntity entity) {
        setActiveAbility(null);
        for (AbilityType<? extends LivingEntity, ?> abilityType : getAbilityTypesOnEntity(entity)) {
            Ability<?> ability = abilityType.makeInstance(entity);
            abilityInstances.put(abilityType, ability);
            if (nbtMap.containsKey(abilityType.getName())) ability.readNBT(nbtMap.get(abilityType.getName()));
        }
    }

    public void activateAbility(LivingEntity entity, AbilityType<?, ?> abilityType) {
        Ability<?> ability = abilityInstances.get(abilityType);
        if (ability != null) {
            boolean tryResult = ability.tryAbility();
            if (tryResult) ability.start();
        } else
            System.out.println("Ability<?>" + abilityType.toString() + " does not exist on mob " + entity.getClass().getSimpleName());
    }

    public void tick(LivingEntity entity) {
        if (needsReinstancing) {
            instanceAbilities(entity);
            needsReinstancing = false;
        }
        for (Ability<?> ability : abilityInstances.values()) {
            ability.tick();
        }
    }

    public AbilityType<?, ?>[] getAbilityTypesOnEntity(LivingEntity entity) {
        if (entity instanceof Player) {
            return AbilityHandler.PLAYER_ABILITIES;
        }
        if (entity instanceof MowzieGeckoEntity) {
            return ((MowzieGeckoEntity) entity).getAbilities();
        }
        return new AbilityType[0];
    }

    public Map<AbilityType<?, ?>, Ability<?>> getAbilityMap() {
        return abilityInstances;
    }

    public Ability<?> getAbilityFromType(AbilityType<?, ?> abilityType) {
        return abilityInstances.get(abilityType);
    }

    public Ability<?> getActiveAbility() {
        return activeAbility;
    }

    public void setActiveAbility(Ability<?> activeAbility) {
        if (getActiveAbility() != null && getActiveAbility().isUsing()) getActiveAbility().interrupt();
        this.activeAbility = activeAbility;
    }

    public Collection<Ability<?>> getAbilities() {
        return abilityInstances.values();
    }

    public boolean attackingPrevented() {
        return getActiveAbility() != null && getActiveAbility().preventsAttacking();
    }

    public boolean blockBreakingBuildingPrevented() {
        return getActiveAbility() != null && getActiveAbility().preventsBlockBreakingBuilding();
    }

    public boolean interactingPrevented() {
        return getActiveAbility() != null && getActiveAbility().preventsInteracting();
    }

    public boolean itemUsePrevented(ItemStack itemStack) {
        return getActiveAbility() != null && getActiveAbility().preventsItemUse(itemStack);
    }

    public <E extends GeoEntity> PlayState animationPredicate(AnimationTest<E> e, GeckoPlayer.Perspective perspective) {
        return getActiveAbility().animationPredicate(e, perspective);
    }

    public void codeAnimations(MowzieGeoModel<? extends GeoEntity> model, float partialTick) {
        getActiveAbility().codeAnimations(model, partialTick);
    }

    @Override
    public void serialize(ValueOutput output) {
        CompoundTag compound = new CompoundTag();
        for (Map.Entry<AbilityType<?, ?>, Ability<?>> abilityEntry : getAbilityMap().entrySet()) {
            CompoundTag nbt = abilityEntry.getValue().writeNBT();
            if (!nbt.isEmpty()) {
                compound.put(abilityEntry.getKey().getName(), nbt);
            }
        }
        if (!compound.isEmpty()) {
            output.store("abilities", CompoundTag.CODEC, compound);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        nbtMap.clear();
        CompoundTag compound = input.read("abilities", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        Set<String> keys = compound.keySet();
        for (String abilityName : keys) {
            nbtMap.put(abilityName, compound.get(abilityName));
        }
        needsReinstancing = true;
    }
}
