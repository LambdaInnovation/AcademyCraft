package cn.academy.ability.context;

import cn.academy.AcademyCraft;
import cn.academy.ability.context.Context.Status;
import cn.academy.analytic.events.AnalyticSkillEvent;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.academy.event.ability.OverloadEvent;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.NetworkS11n.ContextException;
import cn.lambdalib2.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.stream.Stream;

/**
 * Global manager of {@link Context}. Connections are handled through it.
 */
public enum ContextManager {
    instance;

    private static final boolean DEBUG_LOG = false;

    private static final double
            T_KA_TOL = 1.5, // Tile tolerance of receiving keepAlive packets
            T_KA = 0.5; // Time interval between sending KeepAlive packets

    private static final String
        M_BEGIN_LINK="l",
        M_ESTABLISH_LINK="ld",
        M_MAKEALIVE="m",
        M_TERM_ATLOCAL="tl",
        M_TERM_ATSERVER="ts",
        M_KEEPALIVE="ka";

    //API

    /**
     * Activate the context, which establish the connection of C/S.
     */
    @SideOnly(Side.CLIENT)
    public void activate(Context ctx) {
        Preconditions.checkState(ctx.status == Status.CONSTRUCTED, "Can't activate one context multiple times");
        Preconditions.checkState(ctx.isLocal(), "Can only activate context at local.");

        LocalManager.instance.activate(ctx);
    }

    /**
     * Terminate this Context. Link between server and clients is ripped apart, and the terminate message is sent.
     */
    public void terminate(Context ctx) {
        if (!ctx.isRemote()) {
            ServerManager.instance.terminate(ctx);
        } else if (ctx.isLocal()) {
            LocalManager.instance.terminate(ctx);
        } else throw wrongSide();
    }

    /**
     * Finds a context of given type that is alive.
     */
    @SuppressWarnings("sideonly")
    public <T> Optional<T> find(Class<T> type) {
        if (SideUtils.isClient()) {
            Optional<T> test1 = findLocal(type);
            if (test1.isPresent()) return test1;
            return findIn(ClientManager.instance.alive.stream().map(d -> d.ctx), type);
        } else {
            return findIn(ServerManager.instance.alive.stream().map(d -> d.ctx), type);
        }
    }

    /**
     * Finds a context that is created locally (by the client player) of given type.
     */
    @SideOnly(Side.CLIENT)
    public <T> Optional<T> findLocal(Class<T> type) {
        return findIn(LocalManager.instance.alive.stream().map(d -> d.ctx), type);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> findIn(Stream<Context> stream, Class<T> type) {
        return (Optional) stream.filter(type::isInstance)
                .findAny();
    }

    void mToSelf(Context ctx, String channel, Object ...args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToSelf(ctx, channel, args);
        } else if (ctx.isLocal()) {
            LocalManager.instance.mToSelf(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToServer(Context ctx, String channel, Object ...args) {
        if (!checkStatus(ctx)) return;
        if (ctx.isLocal()) {
            LocalManager.instance.mToServer(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToLocal(Context ctx, String channel, Object ...args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToLocal(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToClient(Context ctx, String channel, Object ...args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToClient(ctx, channel, args);
        } else throw wrongSide();
    }

    void mToExceptLocal(Context ctx, String channel, Object ...args) {
        if (!checkStatus(ctx)) return;
        if (!ctx.isRemote()) {
            ServerManager.instance.mToExceptLocal(ctx, channel, args);
        } else throw wrongSide();
    }

    private boolean checkStatus(Context ctx) { return ctx.getStatus() != Status.TERMINATED; }

    private static IllegalStateException wrongSide() {
        return new IllegalStateException("Wrong context side!");
    }

    private static IllegalStateException notFound() {
        return new IllegalStateException("Illegal state: alive context not found in data!");
    }

    private static Object writeContextType(Class<? extends Context> type) {
        return type.getName();
    }

    private static void log(Object msg) {
        if (AcademyCraft.DEBUG_MODE && DEBUG_LOG)
             AcademyCraft.log.info("CM: " + msg);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Context> readContextType(Object in) {
        try  {
            return (Class) Class.forName((String) in);
        } catch (ClassNotFoundException ex) {
            throw Throwables.propagate(ex);
        }
    }

    @NetworkS11nType
    public enum LocalManager {
        instance;

        {
            FMLCommonHandler.instance().bus().register(this);
        }

        Map<Integer, ContextData> suspended = new HashMap<>();
        List<ContextData> alive = new LinkedList<>();

        int nextClientID;

        @SideOnly(Side.CLIENT)
        void activate(Context ctx) {
            ContextData data = new ContextData();
            data.ctx = ctx;

            suspended.put(nextClientID, data);
            NetworkMessage.sendToServer(ServerManager.instance, M_BEGIN_LINK,
                    writeContextType(ctx.getClass()), player(), nextClientID);

            nextClientID += 1;

            log("[LOC] BeginLink");
        }

        void terminate(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                    data.disposed = true;
                    return;
                }

            for (ContextData data : suspended.values()) if (data.ctx == ctx) {
                data.disposed = true;
                return;
            }

            throw new IllegalStateException("Not found");
        }

        void mToSelf(Context ctx, String channel, Object[] args) {
            if (ctx.status == Status.CONSTRUCTED || ctx.status == Status.ALIVE) {
                NetworkMessage.sendToSelf(ctx, channel, args);
            } else {
                // Ignore terminated
            }
        }

        void mToServer(Context ctx, String channel, Object[] args) {
            if (ctx.status == Status.ALIVE) {
                for (ContextData data : alive) {
                    if (data.ctx == ctx) {
                        NetworkMessage.sendToServer(ctx, channel, args);
                        return;
                    }
                }
                throw notFound();
            } else if (ctx.status == Status.CONSTRUCTED) {
                for (ContextData data : suspended.values()) {
                    if (data.ctx == ctx) {
                        Call call = new Call();
                        call.msg = channel;
                        call.args = args;
                        data.calls.add(call);
                        return;
                    }
                }
                throw notFound();
            } else {
                // Ignore terminated
            }
        }

        @SideOnly(Side.CLIENT)
        private EntityPlayer player() {
            return Minecraft.getMinecraft().player;
        }

        @Listener(channel=M_ESTABLISH_LINK, side=Side.CLIENT)
        private void hEstablishLink(int clientID, int serverID) {
            ContextData data = suspended.remove(clientID);
            if (data != null) {
                data.ctx.status = Status.ALIVE;
                data.serverID = serverID;
                data.ctx.serverID = serverID;

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);
                for (Call call : data.calls) {
                    mToServer(data.ctx, call.msg, call.args);
                }
                data.calls = null;

                log("[LOC] EstablishLink");
            }
        }

        @Listener(channel=M_TERM_ATSERVER, side=Side.CLIENT)
        private void hTerminate(int serverID) {
            Optional.ofNullable(findOrNull(serverID)).ifPresent(x -> x.disposed = true);

            log("[LOC] Terminate At Server");
        }

        @Listener(channel=M_KEEPALIVE, side=Side.CLIENT)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null) {
                data.lastKeepAlive = time();
            }

            log("[LOC] KeepAlive");
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        private class ContextData {
            Context ctx;
            int serverID;

            double lastKeepAlive = time();
            double lastSentKeepAlive = time() - 0.5;
            boolean disposed;

            List<Call> calls = new ArrayList<>();
        }

        private class Call {
            String msg;
            Object[] args;
        }

        private double time() {
            return GameTimer.getTime();
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void __onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END && ClientUtils.isPlayerPlaying()) {
                double time = time();

                for (ContextData data : alive) {
                    if (time - data.lastKeepAlive > T_KA_TOL) {
                        log("[LOC] Timeout");
                        data.disposed = true;
                    } else {
                        if (time - data.lastSentKeepAlive > T_KA) {
                            NetworkMessage.sendToServer(ServerManager.instance, M_KEEPALIVE, data.serverID);

                            data.lastSentKeepAlive = time;
                        }

                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                    }
                }

                Iterator<ContextData> itr = alive.iterator();
                while (itr.hasNext()) {
                    ContextData data = itr.next();
                    if (data.disposed) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
                        NetworkMessage.sendToServer(ServerManager.instance, M_TERM_ATLOCAL, data.serverID);
                        itr.remove();

                        log("[LOC] Dispose");
                    }
                }
            }
        }

        @SubscribeEvent
        public void __onDisconnect(ClientDisconnectionFromServerEvent evt) {
            for(ContextData data:alive){
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
            }
            alive.clear();
            suspended.clear();
        }
    }

    @NetworkS11nType
    public enum ServerManager {
        instance;

        {
            FMLCommonHandler.instance().bus().register(this);
            MinecraftForge.EVENT_BUS.register(this);
        }

        List<ContextData> alive = new LinkedList<>();

        int nextServerID;

        void terminate(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                data.disposed = true;
            }
        }

        void mToSelf(Context ctx, String channel, Object[] args) {
            NetworkMessage.sendToSelf(ctx, channel, args);
        }

        void mToLocal(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendTo(data.ctx.player, ctx, channel, args);
        }

        void mToClient(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendTo(data.ctx.player, ctx, channel, args);
            NetworkMessage.sendToPlayers(data.targets, ctx, channel, args);
        }

        void mToExceptLocal(Context ctx, String channel, Object[] args) {
            ContextData data = find(ctx);
            NetworkMessage.sendToPlayers(data.targets, ctx, channel, args);
        }

        private ContextData find(Context ctx) {
            for (ContextData data : alive) if (data.ctx == ctx) {
                return data;
            }
            throw new IllegalStateException("ContextData not present");
        }

        @Listener(channel=M_BEGIN_LINK, side=Side.SERVER)
        @SuppressWarnings("unchecked")
        private void hBeginLink(Object typein, EntityPlayerMP player, int clientID) {
            try {
                Class<? extends Context> type = readContextType(typein);
                Context ctx = type.getConstructor(EntityPlayer.class).newInstance(player);
                ContextData data = new ContextData();
                data.ctx = ctx;

                Set<EntityPlayerMP> players = new HashSet<>((List) WorldUtils.getEntities(player, 25, EntitySelectors.player()));
                players.remove(player);

                data.targets = players.toArray(new EntityPlayerMP[players.size()]);

                data.ctx.status = Status.ALIVE;
                data.serverID = nextServerID;
                data.ctx.serverID = nextServerID;

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);

                NetworkMessage.sendTo(player, LocalManager.instance, M_ESTABLISH_LINK, clientID, nextServerID);
                NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_MAKEALIVE,
                        writeContextType(ctx.getClass()), player, nextServerID);
                MinecraftForge.EVENT_BUS.post(new AnalyticSkillEvent(data.ctx.player,data.ctx.skill));
                nextServerID += 1;

                log("[SVR] BeginLink");
            } catch (Exception ex) {
                Throwables.propagate(ex);
            }
        }

        @Listener(channel=M_TERM_ATLOCAL, side=Side.SERVER)
        private void hTerminate(int serverID) {
            Optional.ofNullable(findOrNull(serverID)).ifPresent(x -> x.disposed = true);
        }

        @Listener(channel=M_KEEPALIVE, side=Side.SERVER)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null) {
                data.lastKeepAlive = time();

                log("[SVR] KeepAlive");
            }
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        @SubscribeEvent
        public void __onServerTick(ServerTickEvent evt) {
            if (evt.phase == Phase.END) {
                double time = time();

                for (ContextData data : alive) {
                    if (data.disposed || time - data.lastKeepAlive > T_KA_TOL) {
                        data.disposed = true;
                    } else {
                        if (time - data.lastSentKeepAlive > T_KA) { // Send KeepAlive packets
                            NetworkMessage.sendTo(data.ctx.player, LocalManager.instance, M_KEEPALIVE, data.serverID);
                            NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_KEEPALIVE, data.serverID);

                            data.lastSentKeepAlive = time;
                            log("[SVR] SendKeepAlive");
                        }

                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                    }
                }

                Iterator<ContextData> itr = alive.iterator();
                while (itr.hasNext()) {
                    ContextData data = itr.next();
                    if (data.disposed || data.ctx.player.isDead) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);

                        NetworkMessage.sendTo(data.ctx.player, LocalManager.instance, M_TERM_ATSERVER, data.serverID);
                        NetworkMessage.sendToPlayers(data.targets, ClientManager.instance, M_TERM_ATSERVER, data.serverID);

                        itr.remove();

                        log("[SVR] Dispose");
                    }
                }

            }
        }

        @SubscribeEvent
        public void __onOverload(OverloadEvent evt) {
            disposePlayer(evt.player);
        }

        @SubscribeEvent
        public void __onCategoryChange(CategoryChangeEvent evt) {
            if (!evt.player.world.isRemote) {
                disposePlayer(evt.player);
            }
        }

        private void disposePlayer(EntityPlayer p) {
            for (ContextData d : alive) if (d.ctx.player.equals(p)) {
                d.disposed = true;
            }
        }

        private class ContextData {
            Context ctx;
            EntityPlayerMP[] targets;
            int serverID;
            boolean disposed = false;

            double lastKeepAlive = time();
            double lastSentKeepAlive = time() - 0.5;
        }

        private double time() {
            return GameTimer.getTime();
        }
    }

    @NetworkS11nType
    public enum ClientManager {
        instance;

        {
            FMLCommonHandler.instance().bus().register(this);
        }

        List<ContextData> alive = new LinkedList<>();

        @Listener(channel=M_MAKEALIVE, side=Side.CLIENT)
        private void hMakeAlive(Object typein, EntityPlayer player, int serverID) {
            try {
                Class<? extends Context> type = readContextType(typein);
                Context ctx = type.getConstructor(EntityPlayer.class).newInstance(player);
                ContextData data = new ContextData();

                data.ctx = ctx;
                data.serverID = serverID;
                data.ctx.serverID = serverID;
                data.ctx.status = Status.ALIVE;
                data.lastKeepAlive = time();

                alive.add(data);
                NetworkMessage.sendToSelf(data.ctx, Context.MSG_MADEALIVE);

                log("[CLI] MakeAlive");
            } catch (Exception ex) {
                Throwables.propagate(ex);
            }
        }

        @Listener(channel=M_KEEPALIVE, side=Side.CLIENT)
        private void hKeepAlive(int serverID) {
            ContextData data = findOrNull(serverID);
            if (data != null) {
                data.lastKeepAlive = time();

                log("[CLI] KeepAlive");
            }
        }

        @Listener(channel=M_TERM_ATSERVER, side=Side.CLIENT)
        private void hTerminate(int serverID) {
            Optional.ofNullable(findOrNull(serverID)).ifPresent(x -> x.disposed = true);
            log("[CLI] TerminateAtServer");
        }

        private ContextData findOrNull(int serverID) {
            for (ContextData data : alive) {
                if (data.serverID == serverID) return data;
            }
            return null;
        }

        private class ContextData {
            Context ctx;
            int serverID;
            double lastKeepAlive = time();
            boolean disposed = false;
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public void __onClientTick(ClientTickEvent evt) {
            if (evt.phase == Phase.END && ClientUtils.isPlayerPlaying()) {
                double time = time();

                for (ContextData data : alive) {
                    if (data.disposed || time - data.lastKeepAlive > T_KA_TOL) {
                        log("[CLI] Timeout!!");
                        data.disposed = true;
                    } else {
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TICK);
                    }
                }

                Iterator<ContextData> iter = alive.iterator();
                while (iter.hasNext()) {
                    ContextData data = iter.next();
                    if (data.disposed) {
                        data.ctx.status = Status.TERMINATED;
                        NetworkMessage.sendToSelf(data.ctx, Context.MSG_TERMINATED);
                        iter.remove();
                        log("[CLI] Dispose");
                    }
                }
            }
        }

        @SubscribeEvent
        public void __onDisconnect(ClientDisconnectionFromServerEvent evt) {
            alive.clear();
        }

        private double time() {
            return GameTimer.getTime();
        }
    }

    static {
        NetworkS11n.addDirect(Context.class, new NetS11nAdaptor<Context>() {
            @Override
            public void write(ByteBuf buf, Context obj) {
                Preconditions.checkState(obj.serverID != -1);
                buf.writeInt(obj.serverID);
            }
            @Override
            public Context read(ByteBuf buf) throws ContextException {
                int serverID = buf.readInt();
                if (!SideUtils.isClient()) {
                    ServerManager.ContextData data = ServerManager.instance.findOrNull(serverID);
                    if (data != null) return data.ctx;
                    else throw new ContextException("Can't find server context");
                } else {
                    LocalManager.ContextData data0 = LocalManager.instance.findOrNull(serverID);
                    if (data0 != null) return data0.ctx;
                    else {
                        ClientManager.ContextData data1 = ClientManager.instance.findOrNull(serverID);
                        if (data1 != null) return data1.ctx;
                        else throw new ContextException("Can't find client context");
                    }
                }
            }
        });
    }

}