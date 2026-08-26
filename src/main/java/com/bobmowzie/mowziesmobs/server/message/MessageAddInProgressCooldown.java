package com.bobmowzie.mowziesmobs.server.message;

import com.bobmowzie.mowziesmobs.MMCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record MessageAddInProgressCooldown(Item item, Integer startTime, Integer endTime) implements CustomPacketPayload {
    public static final Type<MessageAddInProgressCooldown> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MMCommon.MODID, "message_add_in_progress_cooldown"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MessageAddInProgressCooldown> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM),
            MessageAddInProgressCooldown::item,
            ByteBufCodecs.VAR_INT,
            MessageAddInProgressCooldown::startTime,
            ByteBufCodecs.VAR_INT,
            MessageAddInProgressCooldown::endTime,
            MessageAddInProgressCooldown::new
    );

    public static void handleClient(final MessageAddInProgressCooldown packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = MMCommon.PROXY.getLocalPlayer();
            if (player != null) {
                // PORTING NOTE (1.21.1 -> 26.1.2): ItemCooldowns#cooldowns/CooldownInstance are now private
                // (see PlayerData.java's setPawCooldownsForNBT/loadPawCooldownsFromNBT for the fuller writeup) -
                // rebuilt on the public addCooldown(Identifier, int ticksRemaining) API. This mirrors the same
                // "resets the overlay swipe animation but preserves the correct ticks-remaining" behavior nuance.
                player.getCooldowns().addCooldown(BuiltInRegistries.ITEM.getKey(packet.item()), packet.endTime() - packet.startTime());
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
