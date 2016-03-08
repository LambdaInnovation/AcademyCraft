/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import cn.academy.ability.api.context.Context.Status;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.core.LambdaLib;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.*;
import cn.lambdalib.s11n.network.NetworkS11n;
import cn.lambdalib.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib.util.client.ClientUtils;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.mc.SideHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.lambdalib.s11n.network.NetworkMessage.*;

/**
 * Global manager of {@link Context}. Connections are handled through it.
 */
@Registrant
public enum ContextManager {
    instance;

    private static final long
            T_FIRST_KA_TOL = 2000L, // Time tolerance from creating the context in client to first KeepAlive packet
            T_KA_TOL = 1500L, // Tile tolerance of receiving keepAlive packets
            T_KA = 500L; // Time interval between sending KeepAlive packets

    //API

    /**
     * Activate the context, which establish the connection of C/S.
     */
    public void activate(Context ctx) {
        Preconditions.checkState(ctx.getStatus() == Status.CONSTRUCTED, "Can't activate same context multiple times");

        if (remote()) activate_c(ctx);
        else          activate_s(ctx, true);
    }

    /**
     * Terminate this Context. Links between server and clients are ripped apart, and the terminate message is invoked.
     */
    public void terminate(Context ctx) {
        Status stat = ctx.getStatus();
        if (remote() && (stat == Status.ALIVE || stat == Status.CONSTRUCTED)) {
            terminate_c(ctx);
        } else if (!remote() && (stat == Status.ALIVE)) {
            terminate_s(ctx);
        } else {
            throw new IllegalStateException("Can't terminate this context");
        }
    }

    /**
     * Finds a context of given type that is alive.
     */
    public <T> Optional<T> find(Class<T> type) {
        return find(type, x -> true);
    }

    /**
     * Finds a context that is created locally (by the client player) of given type.
     */
    @SideOnly(Side.CLIENT)
    public <T> Optional<T> findLocal(Class<T> type) {
        return find(type, ctx -> Minecraft.getMinecraft().thePlayer.equals(((Context)ctx).player));
    }

    /**
     * Finds a context that is of given type and satifies the given precondition.
     */
    public <T> Optional<T> find(Class<T> type, Predicate<T> pred) {
        return aliveStream().filter(ctx -> type.isInstance(ctx) && pred.test((T) ctx))
                .map(x -> (T) x)
                .findAny();
    }

    public List<Context> allAlive() {
        return aliveStream().collect(Collectors.toList());
    }

    private Stream<Context> aliveStream() {
        Stream<Context> orig = alive().values().stream().map(info -> info.ctx);
        Stream<Context> stream;
        if (remote()) {
            stream = Stream.concat(
                    clientSuspended.values().stream().map(sus -> sus.ctx),
                    orig);
        } else {
            stream = orig;
        }
        return stream;
    }

    // Internal
    private static final String
        TERMINATE_AT_CLIENT = "1",
        TERMINATE_AT_SERVER = "2",
        MAKE_ALIVE = "3",
        AFTER_MAKE_ALIVE = "4",
        ACTIVATE_AT_CLIENT = "5",
        HANDSHAKE = "6",
        KEEPALIVE = "7",
        KEEPALIVE_SERVER = "8";

    private ThreadLocal<Map<Integer, ContextInfo>> aliveLocal = ThreadLocal.withInitial(HashMap::new);

    // Those created in client but yet to handshake with server.
    // They are recon as alive in client.
    // @SideOnly(Side.CLIENT)
    private Map<Integer, ClientSuspended> clientSuspended = new HashMap<>();

    // Server global context ID incrementor
    // Runtime local
    private int increm = 0;

    // Client (temp) ID incrementor
    // Runtime local
    private int clientCreateIncrem = 0;

    private void terminate_s(Context ctx) {
        sendToAll(this, TERMINATE_AT_CLIENT, ctx);
        terminatePost(ctx);
    }

    private void terminate_c(Context ctx) {
        Preconditions.checkArgument(ctx.isLocal(), "Can't terminate context at non-local client");
        debug("terminate_c");

        if (ctx.getStatus() == Status.CONSTRUCTED) {
            clientSuspended.values().stream()
                    .filter(sus -> sus.ctx == ctx)
                    .findAny()
                    .ifPresent(sus -> {
                        sus.terminateSignal = true;
                    });
        } else {
            sendToServer(this, TERMINATE_AT_SERVER, ctx);
        }

        terminatePost(ctx);
    }

    private void terminatePost(Context ctx) {
        sendToSelf(ctx, Context.MSG_TERMINATED);
        // alive().remove(ctx.networkID);  NOTE: Suspended to next tick: prevent ConcurrentModification
        ctx.status = Status.TERMINATED;
    }

    // Creates context at server and informs all client to activate
    private void activate_s(Context ctx, boolean sendToCreator) {
        ctx.networkID = increm;
        ++increm;

        makeAlivePost(ctx);

        EntityPlayerMP[] players = getSendList(ctx, !sendToCreator);
        // Handshake
        sendToPlayers(players, this, ACTIVATE_AT_CLIENT, ctx.networkID, ctx.getClass().getName());
    }

    @SideOnly(Side.CLIENT)
    private void activate_c(Context ctx) {
        final int clientID = clientCreateIncrem;
        clientSuspended.put(clientID, new ClientSuspended(ctx, clientID));
        clientCreateIncrem++;

        sendToServer(this, MAKE_ALIVE, Minecraft.getMinecraft().thePlayer, clientID, ctx.getClass().getName());
    }

    private void makeAlivePost(Context ctx) {
        createInfo(ctx);
        ctx.status = Status.ALIVE;

        sendToSelf(ctx, Context.MSG_MADEALIVE);
    }

    private void killClient(int networkID) {
        ClientInfo info = getInfoC(networkID);
        if (info != null) {
            alive().remove(networkID);
        }
    }

    private void killServer(int networkID) {
        ServerInfo info = getInfoS(networkID);
        if (info != null) {
            alive().remove(networkID);
        }
    }

    /**
     * @return Should remove this info because no KeepAlive package received
     */
    private boolean checkKeepAlive(ContextInfo info, long time) {
        if (info.lastKeepAlive == -1) {
            if (time - info.createTime > T_FIRST_KA_TOL) {
                return true;
            }
        } else {
            if (time - info.lastKeepAlive > T_KA_TOL) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return Should remove this info
     */
    private boolean tickClient(ClientInfo info, long time) {
        if (info.ctx.player.isDead) {
            if (info.ctx.getStatus() != Status.TERMINATED) {
                terminate(info.ctx);
            }
            return true;
        }

        if (info.ctx.getStatus() == Status.TERMINATED || checkKeepAlive(info, time)) {
            return true;
        }

        if (info.ctx.isLocal() && info.lastSend == -1 || time - info.lastSend > T_KA) {
            sendToServer(this, KEEPALIVE_SERVER, info.ctx.networkID);
        }

        sendToSelf(info.ctx, Context.MSG_TICK);
        return false;
    }

    /**
     * @return Should remove this info
     */
    private boolean tickServer(ServerInfo info, long time) {
        if (info.ctx.player.isDead) {
            if (info.ctx.getStatus() != Status.TERMINATED) {
                terminate(info.ctx);
            }
            return true;
        }

        if (info.ctx.getStatus() == Status.TERMINATED || checkKeepAlive(info, time)) {
            return true;
        }

        if (info.lastSend == -1 || time - info.lastSend > T_KA) {
            sendToPlayers(info.client.toArray(new EntityPlayerMP[info.client.size()]),
                    this, KEEPALIVE, info.ctx.networkID);
        }

        sendToSelf(info.ctx, Context.MSG_TICK);
        return false;
    }

    // Messaging
    void mToSelf(Context ctx, String cn, Object[] args) {
        if (ctx.getStatus() == Status.ALIVE ||
                // It's possible that makealive hasn't yet happened. Allow some degree of uncertainty here.
                // If user sends message in local constructed stage, it's handled by suspending the call in MakeAlive stage.
           (ctx.getStatus() == Status.CONSTRUCTED && ctx.isLocal())) {
            NetworkMessage.sendToSelf(ctx, cn, args);
        } else {
            throw new IllegalStateException("Try to sendToSelf after context is terminated");
        }
    }

    void mToServer(Context ctx, String cn, Object[] args) {
        if (!_mCheck(ctx, true)) return;

        if (ctx.getStatus() == Status.ALIVE) {
            sendToServer(ctx, cn, args);
        } else if (ctx.getStatus() == Status.CONSTRUCTED) { // Suspend call
            checkSuspended(ctx).get().suspendedCalls.add(new MessageCall(cn, args));
        } else {
            throw new RuntimeException("Sending message after context is terminated");
        }
    }

    void mToLocal(Context ctx, String cn, Object[] args) {
        if (!_mCheck(ctx, false)) return;

        sendTo(ctx.getPlayer(), ctx, cn, args);
    }

    void mToExceptLocal(Context ctx, String cn, Object[] args) {
        if (!_mCheck(ctx, false)) return;

        EntityPlayer localPlayer = ctx.getPlayer();
        EntityPlayerMP[] players = getInfoS(ctx.networkID).client.stream()
                .filter(p -> p != localPlayer).toArray(EntityPlayerMP[]::new);
        sendToPlayers(players, ctx, cn, args);
    }

    void mToClient(Context ctx, String cn, Object[] args) {
        if (!_mCheck(ctx, false)) return;

        sendToPlayers(Iterables.toArray(getInfoS(ctx.networkID).client, EntityPlayerMP.class), ctx, cn, args);
    }

    private boolean _mCheck(Context ctx, boolean requiredRemote) {
        if (ctx.getStatus() != Status.ALIVE) return false;

        Preconditions.checkState(remote() == requiredRemote, "Invalid messaging side");
        if (requiredRemote) {
            Preconditions.checkState(ctx.isLocal(), "Context must be local");
        }

        return true;
    }

    private Optional<ClientSuspended> checkSuspended(Context ctx) {
        return clientSuspended.values().stream().filter(cs -> cs.ctx == ctx).findFirst();
    }

    //

    // Message handlers

    @Listener(channel=MAKE_ALIVE, side=Side.SERVER)
    private void hMakeAlive(EntityPlayerMP player, int clientID, String className) {
        Context ctx = createContext(className, player);

        activate_s(ctx, false); // Assign networkID
        getInfoS(ctx.networkID).client.add(player); // Add connection to this client

        sendTo(player, this, AFTER_MAKE_ALIVE, clientID, ctx.networkID);
    }

    @Listener(channel=AFTER_MAKE_ALIVE, side=Side.CLIENT)
    private void hAfterMadeAlive(int clientID, int networkID) {
        if (clientSuspended.containsKey(clientID)) {
            ClientSuspended susp = clientSuspended.remove(clientID);
            Context ctx = susp.ctx;

            debug("AfterMadeAlive " + ctx);
            ctx.networkID = networkID;
            makeAlivePost(ctx);

            if (susp.terminateSignal) {
                terminate_c(ctx);
            } else { // TODO: This doesn't represent actual order of sending messages. Check if it causes problems.
                for (MessageCall call : susp.suspendedCalls) {
                    sendToServer(ctx, call.channel, call.args);
                }
            }

        } // { else makeAlive not successful, shall we do something about it? }
    }

    @Listener(channel=HANDSHAKE, side=Side.SERVER)
    private void hHandshake(int networkID, EntityPlayerMP player) {
        final ServerInfo info = getInfoS(networkID);
        if (info != null) {
            debug("Handshake from client " + player);
            info.client.add(player);
        } // {else omit}
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel=ACTIVATE_AT_CLIENT, side=Side.CLIENT)
    private void hActivateAtClient(int networkID, String className) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Preconditions.checkNotNull(player, "Invalid context: player is null");

        Context ctx = createContext(className, player);
        ctx.networkID = networkID;

        makeAlivePost(ctx);

        sendToServer(this, HANDSHAKE, networkID, player);
    }

    @Listener(channel=KEEPALIVE, side=Side.CLIENT)
    private void hKeepAlive(int networkID) {
        ClientInfo info = getInfoC(networkID);
        if (info != null) {
            // debug("KeepAliveC " + networkID);
            info.lastKeepAlive = time();
        }
    }

    @Listener(channel =KEEPALIVE_SERVER, side=Side.SERVER)
    private void hKeepAliveServer(int networkID) {
        ServerInfo info = getInfoS(networkID);
        if (info != null) {
            // debug("KeepAliveS " + networkID);
            info.lastKeepAlive = time();
        }
    }

    @Listener(channel=TERMINATE_AT_CLIENT, side=Side.CLIENT)
    private void hTerminateAtClient(Context ctx) {
        debug("TerminateAtClient " + ctx);

        if (ctx != null)
            terminatePost(ctx);
    }

    @Listener(channel=TERMINATE_AT_SERVER, side=Side.SERVER)
    private void hTerminateAtServer(Context ctx) {
        debug("TerminateAtServer " + ctx);
        terminate_s(ctx);
    }
    //

    private EntityPlayerMP[] getSendList(Context ctx, boolean excludeCreator) {
        final EntityPlayer creator = ctx.getPlayer();
        final double range = ctx.getRange();
        return Arrays.stream(SideHelper.getPlayerList())
                .filter(x -> x instanceof EntityPlayerMP)
                .map(x -> (EntityPlayerMP) x)
                .filter(x -> !excludeCreator || !x.equals(creator) && creator.getDistanceSqToEntity(x) <= range)
                .toArray(EntityPlayerMP[]::new);
    }

    @SuppressWarnings("unchecked")
    private <T extends Context> T createContext(String className, EntityPlayer player) {
        try {
            return (T) Class.forName(className).getConstructor(EntityPlayer.class).newInstance(player);
        } catch(Exception e) {
            throw new RuntimeException("Failed to create context with type " + className, e);
        }
    }

    private void createInfo(Context ctx) {
        Preconditions.checkState(!alive().containsKey(ctx.networkID), "ERR: Context info alrdy exists");

        if (remote()) {
            alive().put(ctx.networkID, new ClientInfo(ctx));
        } else {
            alive().put(ctx.networkID, new ServerInfo(ctx));
        }
    }

    private ContextInfo getInfo(int networkID) {
        return alive().get(networkID);
    }

    private ClientInfo getInfoC(int networkID) {
        return (ClientInfo) getInfo(networkID);
    }

    private ServerInfo getInfoS(int networkID) {
        return (ServerInfo) getInfo(networkID);
    }

    private long time() { return GameTimer.getAbsTime(); }

    private boolean remote() {
        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

    private Map<Integer, ContextInfo> alive() {
        return aliveLocal.get();
    }

    private void debug(Object msg) {
        log().info("[CM]" + msg);
    }

    private Logger log() {
        return AcademyCraft.log;
    }

    private class ContextInfo {

        final long createTime;
        final Context ctx;

        long lastSend = -1;
        long lastKeepAlive = -1;

        ContextInfo(Context _ctx) {
            ctx = _ctx;
            createTime = GameTimer.getAbsTime();
        }
    }

    private class MessageCall {
        final String channel;
        final Object[] args;

        public MessageCall(String msg, Object[] _args) {
            channel = msg;
            args = _args;
        }
    }

    private class ClientSuspended {
        final Context ctx;
        final List<MessageCall> suspendedCalls = new ArrayList<>();
        final int cid;

        boolean terminateSignal = false; // A small hack to handle client terminate at CONSTRUCTED stage.

        public ClientSuspended(Context _ctx, int _cid) {
            ctx = _ctx;
            cid = _cid;
        }
    }

    private class ClientInfo extends ContextInfo {

        ClientInfo(Context _ctx) {
            super(_ctx);
        }

    }

    private class ServerInfo extends ContextInfo {

        final List<EntityPlayerMP> client = new ArrayList<>();

        ServerInfo(Context _ctx) {
            super(_ctx);
        }

    }

    static {
        // For serialization
        NetworkS11n.addDirect(Context.class, new NetS11nAdaptor<Context>() {
            @Override
            public void write(ByteBuf buf, Context obj) {
                buf.writeInt(obj.networkID);
            }
            @Override
            public Context read(ByteBuf buf) {
                int idx = buf.readInt();
                ContextInfo info = instance.alive().get(idx);
                if (info == null) {
                    throw new ContextException("No such context");
                } else {
                    return info.ctx;
                }
            }
        });

        // For internal network messaging
        NetworkS11n.addDirect(ContextManager.class, new NetS11nAdaptor<ContextManager>() {
            @Override
            public void write(ByteBuf buf, ContextManager obj) {}
            @Override
            public ContextManager read(ByteBuf buf) {
                return ContextManager.instance;
            }
        });

        FMLCommonHandler.instance().bus().register(new Events());
    }

    public static class Events {

        private final ContextManager m = ContextManager.instance;

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onClientTick(ClientTickEvent evt) {
            if (evt.phase != Phase.START || !ClientUtils.isInWorld())
                return;

            final long time = m.time();

            Map<Integer, ContextInfo> alive = m.alive();
            List<Integer> toRemove = new ArrayList<>();
            for (ContextInfo inf : alive.values()) {
                boolean shouldRemove = m.tickClient((ClientInfo) inf, time);
                if (shouldRemove) {
                    toRemove.add(inf.ctx.networkID);
                }
            }

            toRemove.forEach(m::killClient);
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent evt) {
            if (evt.phase != Phase.START)
                return;

            final long time = m.time();

            Map<Integer, ContextInfo> alive = m.alive();
            List<Integer> toRemove = new ArrayList<>();
            for (ContextInfo inf : alive.values()) {
                boolean shouldRemove = m.tickServer((ServerInfo) inf, time);
                if (shouldRemove) {
                    toRemove.add(inf.ctx.networkID);
                }
            }

            toRemove.forEach(m::killServer);
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onClientDisconnect(ClientDisconnectionFromServerEvent evt) {
            instance.aliveLocal.get().clear();
            instance.clientSuspended.clear();
        }

    }

}
