package cn.li.neoforge262.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge 26.2 payload registration and send helpers for the Clojure GUI RPC channel.
 *
 * C2S moved to {@link ClientPacketDistributor#sendToServer}; S2C stays on
 * {@link PacketDistributor}.
 */
final class ClojureNetworkChannel {
    private static final String PROTOCOL_VERSION = "1";
    private static volatile boolean registered;

    private ClojureNetworkChannel() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(ClojureNetworkChannel::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
            ClojureNetworkPackets.C2SPacket.TYPE,
            ClojureNetworkPackets.C2SPacket.STREAM_CODEC,
            ClojureNetworkPackets.C2SPacket::handle);
        registrar.playToClient(
            ClojureNetworkPackets.S2CPacket.TYPE,
            ClojureNetworkPackets.S2CPacket.STREAM_CODEC,
            ClojureNetworkPackets.S2CPacket::handle);
        registered = true;
    }

    static void sendToServer(String msgId, int requestId, byte[] payload) {
        requireRegistered();
        ClientPacketDistributor.sendToServer(new ClojureNetworkPackets.C2SPacket(msgId, requestId, payload));
    }

    static void sendToClient(ServerPlayer player, int requestId, byte[] response) {
        requireRegistered();
        PacketDistributor.sendToPlayer(player, new ClojureNetworkPackets.S2CPacket(requestId, response));
    }

    static void broadcastGuiBlockStateToTrackingChunk(ServerLevel level, BlockPos pos, byte[] payload) {
        requireRegistered();
        PacketDistributor.sendToPlayersTrackingChunk(
            level,
            ChunkPos.containing(pos),
            new ClojureNetworkPackets.S2CPacket(-1, payload));
    }

    private static void requireRegistered() {
        if (!registered) {
            throw new IllegalStateException(
                "ClojureNetwork payloads not registered (RegisterPayloadHandlersEvent missing)");
        }
    }
}
