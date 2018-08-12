package cn.academy.ability.context;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.AcademyCraft;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.IMessageDelegate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
 * @see cn.lambdalib.s11n.network.NetworkMessage
 * @see ContextManager
 * @author WeAthFolD
 */
public class Context implements IMessageDelegate {
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

    List<ClientContext> clientContexts = new ArrayList<>();

    public final EntityPlayer player;
    public final Skill skill;
    public final AbilityContext ctx;

    Status status = Status.CONSTRUCTED;

    int serverID;

    /**
     * Default ctor, must be kept for reflection creation
     */
    public Context(EntityPlayer _player, Skill _skill) {
        player = _player;
        skill = _skill;

        ctx = AbilityContext.of(_player, _skill);

        if (isRemote()) {
            constructClientContexts();
        }
    }

    @SideOnly(Side.CLIENT)
    private void constructClientContexts() {
        for (Function<Context, ClientContext> supplier : clientTypes.get(getClass())) {
            clientContexts.add(supplier.apply(this));
        }
    }

    @SideOnly(Side.CLIENT)
    public Context(Skill _skill) {
        this(Minecraft.getMinecraft().thePlayer, _skill);
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
        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

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
        mgr.mToServer(this, channel, args);
    }

    public void sendToClient(String channel, Object ...args) {
        mgr.mToClient(this, channel, args);
    }

    public void sendToLocal(String channel, Object ...args) {
        mgr.mToLocal(this, channel, args);
    }

    public void sendToExceptLocal(String channel, Object ...args) {
        mgr.mToExceptLocal(this, channel, args);
    }

    public void sendToSelf(String channel, Object ...args) { mgr.mToSelf(this, channel, args); }

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
        return Minecraft.getMinecraft().thePlayer.equals(player);
    }

    @Override
    public final void onMessage(String channel, Object... args) {
        if (isRemote()) {
            for (ClientContext cctx : clientContexts) {
                NetworkMessage.sendToSelf(cctx, channel, args);
            }
        }
    }

    // RegClientContext support
    static final Multimap<Class<? extends Context>, Function<Context, ClientContext>>
            clientTypes = HashMultimap.create();


}