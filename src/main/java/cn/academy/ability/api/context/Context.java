package cn.academy.ability.api.context;

import cn.academy.core.AcademyCraft;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class Context {
    public static final String
        MSG_TERMINATED = "0",
        MSG_MADEALIVE = "1",
        MSG_TICK = "2";

    public enum Status { CONSTRUCTED, ALIVE, TERMINATED }

    final ContextManager mgr = ContextManager.instance;

    final EntityPlayer player;

    Status status = Status.CONSTRUCTED;

    int networkID = -1;

    /**
     * Default ctor
     */
    public Context(EntityPlayer _player) {
        player = _player;
    }

    EntityPlayer getPlayer() {
        return player;
    }

    // Lifetime

    public final Status getStatus() {
        return status;
    }

    public final void terminate() {
        ContextManager.instance.terminate(this);
    }

    //

    //
    protected final boolean isRemote() {
        return FMLCommonHandler.instance().getEffectiveSide().isClient();
    }

    protected final boolean isLocal() {
        if (isRemote()) {
            return isLocalClient_();
        } else {
            return false;
        }
    }
    //

    // Messaging
    public double getRange() {
        return 20.0;
    }
    //

    // Scala sugar

    //

    // Misc
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
