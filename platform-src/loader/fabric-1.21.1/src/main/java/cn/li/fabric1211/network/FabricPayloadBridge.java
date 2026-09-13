package cn.li.fabric1211.network;

import clojure.lang.IFn;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/** Typed-payload bridge for Fabric API 0.116+ (MC 1.21.1). */
public final class FabricPayloadBridge {
    private static StreamCodec<RegistryFriendlyByteBuf, BytesPayload> codec(CustomPacketPayload.Type<BytesPayload> type) {
        return StreamCodec.of((buf, payload) -> buf.writeByteArray(payload.bytes()),
                buf -> new BytesPayload(type, buf.readByteArray()));
    }

    /**
     * Built once, after the neutral config facade is installed.  This must not
     * be a class initializer: Fabric can load this bridge while Clojure is
     * AOT-initializing platform namespaces, before that facade has a mod id.
     */
    private static final class ChannelTypes {
        private final String modId;
        private final CustomPacketPayload.Type<BytesPayload> c2sType;
        private final CustomPacketPayload.Type<BytesPayload> s2cType;
        private final CustomPacketPayload.Type<BytesPayload> runtimeType;
        private final StreamCodec<RegistryFriendlyByteBuf, BytesPayload> c2sCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, BytesPayload> s2cCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, BytesPayload> runtimeCodec;

        private ChannelTypes(String modId) {
            this.modId = modId;
            c2sType = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(modId, "clj_rpc_c2s"));
            s2cType = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(modId, "clj_rpc_s2c"));
            runtimeType = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(modId, "runtime_sync_v2"));
            c2sCodec = codec(c2sType);
            s2cCodec = codec(s2cType);
            runtimeCodec = codec(runtimeType);
        }
    }

    private static volatile ChannelTypes channelTypes;

    private static volatile boolean serverInstalled;
    private static volatile boolean clientInstalled;

    private FabricPayloadBridge() {}

    private static ChannelTypes configure(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalStateException("Fabric network channels require an installed config mod id");
        }
        ChannelTypes configured = channelTypes;
        if (configured == null) {
            configured = new ChannelTypes(modId);
            channelTypes = configured;
        } else if (!configured.modId.equals(modId)) {
            throw new IllegalStateException("Fabric network channels were already configured for " + configured.modId);
        }
        return configured;
    }

    private static ChannelTypes requireConfigured() {
        ChannelTypes configured = channelTypes;
        if (configured == null) {
            throw new IllegalStateException("Fabric network channels were used before installation");
        }
        return configured;
    }

    public static final class BytesPayload implements CustomPacketPayload {
        private final CustomPacketPayload.Type<BytesPayload> payloadType;
        private final byte[] payloadBytes;

        public BytesPayload(CustomPacketPayload.Type<BytesPayload> type, byte[] bytes) {
            this.payloadType = type;
            this.payloadBytes = Arrays.copyOf(bytes, bytes.length);
        }

        public byte[] bytes() { return Arrays.copyOf(payloadBytes, payloadBytes.length); }
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return payloadType; }
    }

    private static BytesPayload payload(CustomPacketPayload.Type<BytesPayload> type, byte[] bytes) {
        return new BytesPayload(type, bytes);
    }

    public static synchronized void installServer(String modId, IFn requestHandler) {
        if (serverInstalled) return;
        ChannelTypes configured = configure(modId);
        PayloadTypeRegistry.playC2S().register(configured.c2sType, configured.c2sCodec);
        ServerPlayNetworking.registerGlobalReceiver(configured.c2sType, (payload, context) ->
                requestHandler.invoke(payload.bytes(), context.player()));
        serverInstalled = true;
    }

    public static synchronized void installClient(String modId, IFn responseHandler, IFn runtimeHandler) {
        if (clientInstalled) return;
        ChannelTypes configured = configure(modId);
        PayloadTypeRegistry.playS2C().register(configured.s2cType, configured.s2cCodec);
        PayloadTypeRegistry.playS2C().register(configured.runtimeType, configured.runtimeCodec);
        ClientPlayNetworking.registerGlobalReceiver(configured.s2cType, (payload, context) ->
                responseHandler.invoke(payload.bytes(), context.client()));
        ClientPlayNetworking.registerGlobalReceiver(configured.runtimeType, (payload, context) ->
                runtimeHandler.invoke(payload.bytes(), context.client()));
        clientInstalled = true;
    }

    public static void sendToClient(ServerPlayer player, String channel, byte[] bytes) {
        ChannelTypes configured = requireConfigured();
        ServerPlayNetworking.send(player, payload(channel.equals("runtime") ? configured.runtimeType : configured.s2cType, bytes));
    }

    public static void sendToServer(String channel, byte[] bytes) {
        ClientPlayNetworking.send(payload(requireConfigured().c2sType, bytes));
    }
}
