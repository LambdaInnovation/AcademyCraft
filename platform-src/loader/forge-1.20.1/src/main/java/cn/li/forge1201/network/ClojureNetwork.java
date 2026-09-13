package cn.li.forge1201.network;

import cn.li.mcbase.network.NetworkHandlerRegistry;
import clojure.lang.IFn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge 1.20.1 SimpleChannel bridge for Clojure GUI RPC network system.
 */
public class ClojureNetwork {
    private ClojureNetwork() {
    }

    /**
     * Register the SimpleChannel and both packet types.
     * Must be called during mod common setup (before any packet is sent).
     *
     * @param reqHandler  IFn(String msgId, int requestId, byte[] payload, ServerPlayer sender)
     * @param respHandler IFn(int requestId, byte[] response)
     */
    public static void init(IFn reqHandler, IFn respHandler) {
        NetworkHandlerRegistry.install(reqHandler, respHandler);
        ClojureNetworkChannel.initialize();
    }

    public static void sendToServer(String msgId, int requestId, byte[] payload) {
        ClojureNetworkChannel.sendToServer(msgId, requestId, payload);
    }

    public static void sendToClient(ServerPlayer player, int requestId, byte[] response) {
        ClojureNetworkChannel.sendToClient(player, requestId, response);
    }

    public static void broadcastGuiBlockStateToTrackingChunk(ServerLevel level, BlockPos pos, byte[] payload) {
        ClojureNetworkChannel.broadcastGuiBlockStateToTrackingChunk(level, pos, payload);
    }
}
