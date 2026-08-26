package com.bobmowzie.mowziesmobs.server.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public record LootConditionMoonPhase(Integer value) implements LootItemCondition {
    public static final MapCodec<LootConditionMoonPhase> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                            Codec.INT.fieldOf("value").forGetter(LootConditionMoonPhase::value)
                    )
                    .apply(instance, LootConditionMoonPhase::new)
    );

    @Override
    public @NotNull MapCodec<LootConditionMoonPhase> codec() {
        return CODEC;
    }

    // PORTING NOTE (1.21.1 -> 26.1.2): ServerLevel#getMoonPhase() no longer exists - moon phase moved into the new
    // EnvironmentAttributeSystem (confirmed against real 26.1.2 ServerLevel source, e.g. getMoonBrightness(pos)
    // now reads it via environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index()). That API
    // is positional (takes a BlockPos) even though moon phase is effectively level-wide in vanilla, so a position
    // is needed here purely to satisfy the API - using the loot context's ORIGIN parameter when present.
    public boolean test(LootContext context) {
        ServerLevel serverlevel = context.getLevel();
        Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
        BlockPos pos = origin != null ? BlockPos.containing(origin) : BlockPos.ZERO;
        int i = serverlevel.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index();
        return this.value == i;
    }

    public static LootConditionMoonPhase.Builder time(Integer moonPhase) {
        return new LootConditionMoonPhase.Builder(moonPhase);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final Integer value;

        public Builder(Integer moonPhase) {
            this.value = moonPhase;
        }

        public LootConditionMoonPhase build() {
            return new LootConditionMoonPhase(this.value);
        }
    }
}
