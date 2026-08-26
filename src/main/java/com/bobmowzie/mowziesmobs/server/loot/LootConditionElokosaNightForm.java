package com.bobmowzie.mowziesmobs.server.loot;

import com.bobmowzie.mowziesmobs.server.entity.elokosa.EntityElokosa;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

public record LootConditionElokosaNightForm() implements LootItemCondition {
    public static final MapCodec<LootConditionElokosaNightForm> CODEC = MapCodec.unit(LootConditionElokosaNightForm::new);

    public boolean test(LootContext context) {
        if (context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof EntityElokosa elokosa) {
            return elokosa.getNightForm();
        }

        return false;
    }

    @Override
    public @NotNull MapCodec<LootConditionElokosaNightForm> codec() {
        return CODEC;
    }
}
