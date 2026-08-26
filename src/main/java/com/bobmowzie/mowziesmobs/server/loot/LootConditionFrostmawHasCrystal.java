package com.bobmowzie.mowziesmobs.server.loot;

import com.bobmowzie.mowziesmobs.server.entity.frostmaw.EntityFrostmaw;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

public record LootConditionFrostmawHasCrystal() implements LootItemCondition {
    public static final MapCodec<LootConditionFrostmawHasCrystal> CODEC = MapCodec.unit(LootConditionFrostmawHasCrystal::new);

    public boolean test(LootContext context) {
        if (context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof EntityFrostmaw frostmaw) {
            return frostmaw.getHasCrystal();
        }

        return false;
    }

    @Override
    public @NotNull MapCodec<LootConditionFrostmawHasCrystal> codec() {
        return CODEC;
    }
}
