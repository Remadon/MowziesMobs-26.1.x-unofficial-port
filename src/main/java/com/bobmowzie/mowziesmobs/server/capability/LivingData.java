package com.bobmowzie.mowziesmobs.server.capability;

import com.google.common.collect.Maps;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import java.util.List;
import java.util.Map;

public class LivingData implements ValueIOSerializable {
    float lastDamage = 0;
    boolean hasSunblock;
    private final Map<Holder<MobEffect>, MobEffectInstance> eclipsedEffects = Maps.newHashMap();

    public void setLastDamage(float damage) {
        lastDamage = damage;
    }

    public float getLastDamage() {
        return lastDamage;
    }

    public void setHasSunblock(boolean hasSunblock) {
        this.hasSunblock = hasSunblock;
    }

    public boolean getHasSunblock() {
        return hasSunblock;
    }

    public void tick(LivingEntity entity) {
//            if (!hasSunblock && entity.isPotionActive(EffectHandler.SUNBLOCK)) hasSunblock = true;
    }

    public void eclipseEffect(MobEffectInstance mobEffectInstance) {
        eclipsedEffects.put(mobEffectInstance.getEffect(), mobEffectInstance);
    }

    public void unEclipseEffects(LivingEntity entity) {
        if (!this.eclipsedEffects.isEmpty()) {
            for (MobEffectInstance mobeffectinstance : this.eclipsedEffects.values()) {
                entity.addEffect(mobeffectinstance);
            }
            this.eclipsedEffects.clear();
        }
    }

    @Override
    public void serialize(ValueOutput output) {
        if (!this.eclipsedEffects.isEmpty()) {
            output.store("eclipsed_effects", MobEffectInstance.CODEC.listOf(), List.copyOf(this.eclipsedEffects.values()));
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        List<MobEffectInstance> effects = input.read("eclipsed_effects", MobEffectInstance.CODEC.listOf()).orElse(List.of());

        for (MobEffectInstance mobeffectinstance : effects) {
            this.eclipsedEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
        }
    }
}
