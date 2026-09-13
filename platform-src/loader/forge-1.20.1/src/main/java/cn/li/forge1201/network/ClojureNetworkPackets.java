package cn.li.forge1201.network;

import cn.li.mcbase.network.NetworkHandlerRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

final class ClojureNetworkPackets {
    private ClojureNetworkPackets() {
    }

    static final class C2SPacket {
        final String msgId;
        final int requestId;
        final byte[] payload;

        C2SPacket(String msgId, int requestId, byte[] payload) {
            this.msgId = msgId;
            this.requestId = requestId;
            this.payload = payload;
        }

        static void encode(C2SPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.msgId);
            buf.writeInt(pkt.requestId);
            buf.writeByteArray(pkt.payload);
        }

        static C2SPacket decode(FriendlyByteBuf buf) {
            return new C2SPacket(buf.readUtf(), buf.readInt(), buf.readByteArray());
        }

        static void handle(C2SPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ServerPlayer sender = ctx.getSender();
            ctx.enqueueWork(() -> NetworkHandlerRegistry.dispatchRequest(pkt.msgId, pkt.requestId, pkt.payload, sender));
            ctx.setPacketHandled(true);
        }
    }

    static final class S2CPacket {
        final int requestId;
        final byte[] response;

        S2CPacket(int requestId, byte[] response) {
            this.requestId = requestId;
            this.response = response;
        }

        static void encode(S2CPacket pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.requestId);
            buf.writeByteArray(pkt.response);
        }

        static S2CPacket decode(FriendlyByteBuf buf) {
            return new S2CPacket(buf.readInt(), buf.readByteArray());
        }

        static void handle(S2CPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> NetworkHandlerRegistry.dispatchResponse(pkt.requestId, pkt.response));
            ctx.setPacketHandled(true);
        }
    }
}
