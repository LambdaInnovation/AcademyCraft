package cn.li.forge1201.network;

import cn.li.mcmod.ModId;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

final class ClojureNetworkChannel {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel channel;

    private ClojureNetworkChannel() {
    }

    static void initialize() {
        if (channel != null) {
            return;
        }

        channel = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModId.ID, "gui_rpc"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        channel.registerMessage(0,
            ClojureNetworkPackets.C2SPacket.class,
            ClojureNetworkPackets.C2SPacket::encode,
            ClojureNetworkPackets.C2SPacket::decode,
            ClojureNetworkPackets.C2SPacket::handle);

        channel.registerMessage(1,
            ClojureNetworkPackets.S2CPacket.class,
            ClojureNetworkPackets.S2CPacket::encode,
            ClojureNetworkPackets.S2CPacket::decode,
            ClojureNetworkPackets.S2CPacket::handle);
    }

    static void sendToServer(String msgId, int requestId, byte[] payload) {
        requireInitialized().sendToServer(new ClojureNetworkPackets.C2SPacket(msgId, requestId, payload));
    }

    static void sendToClient(ServerPlayer player, int requestId, byte[] response) {
        requireInitialized().send(PacketDistributor.PLAYER.with(() -> player), new ClojureNetworkPackets.S2CPacket(requestId, response));
    }

    static void broadcastGuiBlockStateToTrackingChunk(ServerLevel level, BlockPos pos, byte[] payload) {
        requireInitialized().send(
            PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
            new ClojureNetworkPackets.S2CPacket(-1, payload));
    }

    private static SimpleChannel requireInitialized() {
        if (channel == null) {
            throw new IllegalStateException("ClojureNetwork not initialized");
        }
        return channel;
    }
}
