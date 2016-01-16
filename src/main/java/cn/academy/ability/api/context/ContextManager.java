package cn.academy.ability.api.context;

import cn.academy.ability.api.context.Context.Status;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.SideHelper;
import cn.lambdalib.networkcall.TargetPointHelper;
import cn.lambdalib.s11n.network.NetworkMessage.NetworkListener;
import cn.lambdalib.s11n.network.NetworkS11n;
import cn.lambdalib.s11n.network.NetworkS11n.NetS11nAdaptor;
import cn.lambdalib.util.helper.GameTimer;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static cn.lambdalib.s11n.network.NetworkMessage.*;

@Registrant
public enum ContextManager {
    instance;

    static final long
            T_FIRST_KA_TOL = 2000L, // Time tolerance from creating the context in client to first KeepAlive packet
            T_KA_TOL = 1500L, // Tile tolerance of receiving keepAlive packets
            T_KA = 500L; // Time interval between sending KeepAlive packets

    //API
    public void activate(Context ctx) {
        Preconditions.checkState(ctx.getStatus() == Status.CONSTRUCTED, "Can't activate same context multiple times");

        if (remote()) activate_c(ctx);
        else          activate_s(ctx, true);
    }

    public void terminate(Context ctx) {
        if (ctx.getStatus() == Status.ALIVE) {
            if(remote()) terminate_c(ctx);
            else         terminate_s(ctx);
        } // else { emit? }
    }

    public <T extends Context> Optional<T> find(Class<T> type) {
        // TODO
        return Optional.empty();
    }

    public <T extends Context> Collection<T> findAll(Class<T> type) {
        // TODO
        return Collections.emptyList();
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

    @SideOnly(Side.CLIENT)
    private Map<Integer, Context> waitToMakeAlive = new HashMap<>();

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

        sendToServer(this, TERMINATE_AT_SERVER, ctx);
        terminatePost(ctx);
    }

    private void terminatePost(Context ctx) {
        sendToSelf(ctx, Context.MSG_TERMINATED);
        alive().remove(ctx.networkID);
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
        waitToMakeAlive.put(clientID, ctx);
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
            sendToSelf(info.ctx, Context.MSG_TERMINATED);
        }
    }

    private void killServer(int networkID) {
        ServerInfo info = getInfoS(networkID);
        if (info != null) {
            terminate_s(info.ctx);
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
        if (info.ctx.isLocal() && info.lastSend == -1 || time - info.lastSend > T_KA) {
            sendToServer(this, KEEPALIVE_SERVER, info.ctx.networkID);
        }

        if (checkKeepAlive(info, time)) {
            return true;
        }

        sendToSelf(info.ctx, Context.MSG_TICK);
        return false;
    }

    /**
     * @return Should remove this info
     */
    private boolean tickServer(ServerInfo info, long time) {
        if (info.lastSend == -1 || time - info.lastSend > T_KA) {
            sendToPlayers(info.client.toArray(new EntityPlayerMP[info.client.size()]),
                    this, KEEPALIVE, info.ctx.networkID);
        }

        if (checkKeepAlive(info, time)) {
            return true;
        }

        sendToSelf(info.ctx, Context.MSG_TICK);
        return false;
    }

    // Message handlers
    // TODO null check

    @NetworkListener(value=MAKE_ALIVE, side=Side.SERVER)
    private void hMakeAlive(EntityPlayerMP player, int clientID, String className) {
        Context ctx = createContext(className, player);

        activate_s(ctx, false); // Assign networkID
        getInfoS(ctx.networkID).client.add(player); // Add connection to this client

        sendTo(player, this, AFTER_MAKE_ALIVE, clientID, ctx.networkID);
    }

    @NetworkListener(value=AFTER_MAKE_ALIVE, side=Side.CLIENT)
    private void hAfterMadeAlive(int clientID, int networkID) {
        if (waitToMakeAlive.containsKey(clientID)) {
            Context ctx = waitToMakeAlive.remove(clientID);

            debug("AfterMadeAlive " + ctx);
            ctx.networkID = networkID;
            makeAlivePost(ctx);

        } // { else makeAlive not successful, shall we do something about it? }
    }

    @NetworkListener(value=HANDSHAKE, side=Side.SERVER)
    private void hHandshake(int networkID, EntityPlayerMP player) {
        final ServerInfo info = getInfoS(networkID);
        if (info != null) {
            debug("Handshake from client " + player);
            info.client.add(player);
        } // {else omit}
    }

    @SideOnly(Side.CLIENT)
    @NetworkListener(value=ACTIVATE_AT_CLIENT, side=Side.CLIENT)
    private void hActivateAtClient(int networkID, String className) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        Preconditions.checkNotNull(player, "Invalid context: player is null");

        Context ctx = createContext(className, player);
        ctx.networkID = networkID;

        makeAlivePost(ctx);

        sendToServer(this, HANDSHAKE, networkID, player);
    }

    @NetworkListener(value=KEEPALIVE, side=Side.CLIENT)
    private void hKeepAlive(int networkID) {
        ClientInfo info = getInfoC(networkID);
        if (info != null) {
            debug("KeepAliveC " + networkID);
            info.lastKeepAlive = time();
        }
    }

    @NetworkListener(value=KEEPALIVE_SERVER, side=Side.SERVER)
    private void hKeepAliveServer(int networkID) {
        ServerInfo info = getInfoS(networkID);
        if (info != null) {
            debug("KeepAliveS " + networkID);
            info.lastKeepAlive = time();
        }
    }

    @NetworkListener(value=TERMINATE_AT_CLIENT, side=Side.CLIENT)
    private void hTerminateAtClient(Context ctx) {
        debug("TerminateAtClient " + ctx);

        if (ctx != null)
            terminatePost(ctx);
    }

    @NetworkListener(value=TERMINATE_AT_SERVER, side=Side.SERVER)
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
                return info == null ? null : info.ctx;
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

        @SubscribeEvent
        public void onClientTick(ClientTickEvent evt) {
            if (evt.phase != Phase.START)
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

    }

}
