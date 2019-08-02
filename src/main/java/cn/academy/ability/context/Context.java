package cn.academy.ability.context;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.AcademyCraft;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.IMessageDelegate;
import cn.lambdalib2.util.Debug;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@link Context} represents an environment that is bound to a specific player. When a context is activated
 *  via {@link ContextManager#activate(Context)}, a connection of same context is established in server and clients
 *  near the context, making one able to channel messages inside same context through many different sides.
 * <p>
 *     The logic is built upon LambdaLib's NetworkMessage, so the serialization logic is almost the same, except for
 *  Context handles player connections and remote-side creation for you.
 * </p>
 *
 * @see cn.lambdalib2.s11n.network.NetworkMessage
 * @see ContextManager
 * @author WeAthFolD
 */
public class Context<TSkill extends Skill> implements IMessageDelegate {

    // Turn this on if you want to debug context message detail
    public static final boolean DEBUG_MSG = false;

    public static final String
        MSG_TERMINATED = "i_term",
        MSG_MADEALIVE = "i_alive",
        MSG_TICK = "i_tick";

    // Key messages for single key context.
    public static final String MSG_KEYDOWN = "keydown";
    public static final String MSG_KEYTICK = "keytick";
    public static final String MSG_KEYUP = "keyup";
    public static final String MSG_KEYABORT = "keyabort";

    public enum Status { CONSTRUCTED, ALIVE, TERMINATED }

    private final ContextManager mgr = ContextManager.instance;

    @SideOnly(Side.CLIENT)
    List<ClientContext> clientContexts;

    public final EntityPlayer player;
    public final TSkill skill;
    public final AbilityContext ctx;

    Status status = Status.CONSTRUCTED;

    int serverID;

    /**
     * Default ctor, must be kept for reflection creation
     */
    @SuppressWarnings("sideonly")
    public Context(EntityPlayer _player, TSkill _skill) {
        player = _player;
        skill = _skill;

        ctx = AbilityContext.of(_player, _skill);

        if (isRemote()) {
            constructClientContexts();
        }
    }

    @SideOnly(Side.CLIENT)
    private void constructClientContexts() {
        clientContexts = new ArrayList<>();
        for (Function<Context, ClientContext> supplier : ClientContext.clientTypes.get(getClass())) {
            clientContexts.add(supplier.apply(this));
        }
    }

    @SideOnly(Side.CLIENT)
    public Context(TSkill _skill) {
        this(Minecraft.getMinecraft().player, _skill);
    }

    EntityPlayer getPlayer() {
        return player;
    }

    // Lifetime

    public Status getStatus() {
        return status;
    }

    /**
     * @see ContextManager#terminate(Context)
     */
    public void terminate() {
        ContextManager.instance.terminate(this);
    }

    //

    //
    public final boolean isRemote() {
        // use world.isRemote
        return player.world.isRemote;
//        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

    @SuppressWarnings("sideonly")
    public final boolean isLocal() {
        if (isRemote()) {
            return isLocalClient_();
        } else {
            return false;
        }
    }
    //

    // Messaging
    public double getRange() {
        return 50.0;
    }

    public void sendToServer(String channel, Object ...args) {
        messageDebug("ToServer: " + channel);
        mgr.mToServer(this, channel, args);
    }

    public void sendToClient(String channel, Object ...args) {
        messageDebug("ToClient: " + channel);
        mgr.mToClient(this, channel, args);
    }

    public void sendToLocal(String channel, Object ...args) {
        messageDebug("ToLocal: " + channel);
        mgr.mToLocal(this, channel, args);
    }

    public void sendToExceptLocal(String channel, Object ...args) {
        messageDebug("ToExceptLocal: " + channel);
        mgr.mToExceptLocal(this, channel, args);
    }

    public void sendToSelf(String channel, Object ...args) {
        messageDebug("ToSelf: " + channel);
        mgr.mToSelf(this, channel, args);
    }

    private void messageDebug(String s) {
        if (AcademyCraft.DEBUG_MODE && DEBUG_MSG) {
            Debug.log("[Context]" + (isRemote() ? "[C] " : "[S] " ) +getClass().getSimpleName() + ": " + s);
        }
    }

    //

    // Sugar

    @SideOnly(Side.CLIENT)
    protected ClientRuntime clientRuntime() {
        return ClientRuntime.instance();
    }

    protected World world() {
        return player.world;
    }

    protected void debug(Object message) {
        AcademyCraft.log.info("[CTX]" + message);
    }
    //

    // Ugly hacks
    @SideOnly(Side.CLIENT)
    private boolean isLocalClient_() {
        return Minecraft.getMinecraft().player.equals(player);
    }

    @Override
    @SuppressWarnings("sideonly")
    public final void onMessage(String channel, Object... args) {
        messageDebug("Recv: " + channel);
        if (isRemote()) {
            for (ClientContext cctx : clientContexts) {
                NetworkMessage.sendToSelf(cctx, channel, args);
            }
        }
    }


}