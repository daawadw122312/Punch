package dev.punchboost;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Server-side opt-out. Any server (Fabric mod, Paper/Spigot plugin, proxy) can send an
 * empty plugin message on the channel "punchboost:disable" and the mod turns itself off
 * until the player disconnects.
 */
public final class ServerOptOut {
    private static volatile boolean disabledByServer = false;

    private ServerOptOut() {
    }

    public static boolean isDisabledByServer() {
        return disabledByServer;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(DisablePayload.ID, DisablePayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(DisablePayload.ID, (payload, context) -> {
            disabledByServer = true;
            BoostController.cancel(context.client());
            PunchBoostClient.LOGGER.info("Server sent punchboost:disable; Punch Boost is off for this session");
            if (context.player() != null) {
                context.player().sendMessage(
                        Text.translatable("punchboost.msg.server_disabled").formatted(Formatting.RED), false);
            }
        });

        // Reset when a new connection is set up and when leaving.
        ClientPlayConnectionEvents.INIT.register((handler, client) -> disabledByServer = false);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            disabledByServer = false;
            BoostController.cancel(client);
        });
    }

    public record DisablePayload() implements CustomPayload {
        public static final DisablePayload INSTANCE = new DisablePayload();
        public static final CustomPayload.Id<DisablePayload> ID =
                new CustomPayload.Id<>(Identifier.of(PunchBoostClient.MOD_ID, "disable"));
        public static final PacketCodec<RegistryByteBuf, DisablePayload> CODEC = PacketCodec.unit(INSTANCE);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
