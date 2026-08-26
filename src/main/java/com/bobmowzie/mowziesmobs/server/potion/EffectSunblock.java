package com.bobmowzie.mowziesmobs.server.potion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class EffectSunblock extends MowzieEffect {
    public EffectSunblock() {
        super(MobEffectCategory.BENEFICIAL, 0xFFDF42);
    }

    // PORTING NOTE (1.21.1 -> 26.1.2): MobEffect#applyEffectTick gained a leading ServerLevel parameter (confirmed
    // against real 26.1.2 MobEffect source).
    @Override
    public boolean applyEffectTick(ServerLevel serverLevel, LivingEntity entityLivingBaseIn, int amplifier) {
        super.applyEffectTick(serverLevel, entityLivingBaseIn, amplifier);
        int k = 50 >> amplifier;
        if (k > 0 && entityLivingBaseIn.tickCount % k == 0) {
            if (entityLivingBaseIn.getHealth() < entityLivingBaseIn.getMaxHealth()) {
                entityLivingBaseIn.heal(1.0F);
            }
        }

        return true;
    }
}
