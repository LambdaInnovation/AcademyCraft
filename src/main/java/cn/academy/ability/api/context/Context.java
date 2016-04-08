/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.api.context;

import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.AcademyCraft;
import cn.lambdalib.s11n.network.NetworkMessage;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

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
public class Context {
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

    public final EntityPlayer player;

    Status status = Status.CONSTRUCTED;

    int serverID;

    /**
     * Default ctor, must be kept for reflection creation
     */
    public Context(EntityPlayer _player) {
        player = _player;
    }

    @SideOnly(Side.CLIENT)
    public Context() {
        this(Minecraft.getMinecraft().thePlayer);
    }

    EntityPlayer getPlayer() {
        return player;
    }

    // Lifetime

    public final Status getStatus() {
        return status;
    }

    /**
     * @see ContextManager#terminate(Context)
     */
    public final void terminate() {
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

    private AbilityData cachedAData;
    private CPData cachedCPData;

    protected AbilityData aData() {
        if (cachedAData == null) {
            cachedAData = AbilityData.get(player);
        }
        return cachedAData;
    }

    protected CPData cpData() {
        if (cachedCPData == null) {
            cachedCPData = CPData.get(player);
        }
        return cachedCPData;
    }

    protected World world() {
        return player.worldObj;
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
}
