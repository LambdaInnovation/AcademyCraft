package cn.li.neoforge1211.network;

import cn.li.mcbase.network.NetworkHandlerRegistry;
import cn.li.mcmod.ModId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge 1.21.1 {@link CustomPacketPayload} types for the Clojure GUI RPC channel.
 */
final class ClojureNetworkPackets {
    private ClojureNetworkPackets() {
    }

    record C2SPacket(String msgId, int requestId, byte[] payload) implements CustomPacketPayload {
        static final Type<C2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModId.ID, "gui_rpc_c2s"));

        static final StreamCodec<RegistryFriendlyByteBuf, C2SPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeUtf(pkt.msgId());
                buf.writeVarInt(pkt.requestId());
                buf.writeByteArray(pkt.payload());
            },
            buf -> new C2SPacket(buf.readUtf(), buf.readVarInt(), buf.readByteArray())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(C2SPacket pkt, IPayloadContext context) {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer sender) {
                    NetworkHandlerRegistry.dispatchRequest(pkt.msgId(), pkt.requestId(), pkt.payload(), sender);
                }
            });
        }
    }

    record S2CPacket(int requestId, byte[] response) implements CustomPacketPayload {
        static final Type<S2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ModId.ID, "gui_rpc_s2c"));

        static final StreamCodec<RegistryFriendlyByteBuf, S2CPacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.requestId());
                buf.writeByteArray(pkt.response());
            },
            buf -> new S2CPacket(buf.readVarInt(), buf.readByteArray())
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        static void handle(S2CPacket pkt, IPayloadContext context) {
            context.enqueueWork(() ->
                NetworkHandlerRegistry.dispatchResponse(pkt.requestId(), pkt.response()));
        }
    }
}
