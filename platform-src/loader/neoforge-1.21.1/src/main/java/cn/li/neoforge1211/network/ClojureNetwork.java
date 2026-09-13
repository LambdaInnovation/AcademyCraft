package cn.li.neoforge1211.network;

import cn.li.mcbase.network.NetworkHandlerRegistry;
import clojure.lang.IFn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

/**
 * NeoForge 1.21.1 {@code CustomPacketPayload} bridge for the Clojure GUI RPC network system.
 */
public class ClojureNetwork {
    private ClojureNetwork() {
    }

    /**
     * Register payload types on the mod event bus.
     * Must be called during mod construction (before {@link net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent}).
     */
    public static void register(IEventBus modBus) {
        ClojureNetworkChannel.register(modBus);
    }

    /**
     * Install Clojure request/response handlers.
     * Safe to call during common setup after {@link #register(IEventBus)}.
     *
     * @param reqHandler  IFn(String msgId, int requestId, byte[] payload, ServerPlayer sender)
     * @param respHandler IFn(int requestId, byte[] response)
     */
    public static void init(IFn reqHandler, IFn respHandler) {
        NetworkHandlerRegistry.install(reqHandler, respHandler);
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
