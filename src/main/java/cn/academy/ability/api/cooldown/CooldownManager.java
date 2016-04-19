package cn.academy.ability.api.cooldown;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.context.ClientRuntime;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

import static cn.lambdalib.template.client.render.block.RenderEmptyBlock.id;

/**
 * Manages cooldown cwd for all players on server.
 * @author EAirPeter, WeAthFolD
 */
@Registrant
@NetworkS11nType
public class CooldownManager {

    private static final String MSG_SYNCSET = "syncset", MSG_SETSVR = "setsvr";

    @RegEventHandler
    public static final CooldownManager INSTANCE = new CooldownManager();

    private static final Object staticDelegate = NetworkMessage.staticCaller(CooldownManager.class);

    private CooldownWorldData cwd = null;

    private CooldownManager() {}

    public static boolean isInCooldown(EntityPlayer player, Controllable ctrl, int idSub) {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(), getCtrlId(ctrl, idSub));
    }

    public static boolean isInCooldown(EntityPlayer player, Controllable ctrl) {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(), getCtrlId(ctrl));
    }

    public static void clearCooldown(EntityPlayer player) {
        INSTANCE.cwd.clearCd(player.getCommandSenderName());
    }

    public static void setCooldown(EntityPlayer player, Controllable ctrl, int idSub, int cd) {
        setCooldownRaw(player, getCtrlId(ctrl, idSub), cd);
    }

    public static void setCooldown(EntityPlayer player, Controllable ctrl, int cd) {
        setCooldownRaw(player, getCtrlId(ctrl), cd);
    }

    private static void setCooldownRaw(EntityPlayer player, int id, int cd) {
        if (player.worldObj.isRemote) {
            cIntSetCd(id, cd);
            NetworkMessage.sendToServer(staticDelegate, MSG_SETSVR, player, id, cd);
        } else {
            INSTANCE.cwd.setCd(player.getCommandSenderName(), id, cd);
        }
    }

    public void onServerStarting() {
        Preconditions.checkState(cwd == null);
        cwd = CooldownWorldData.load(FMLCommonHandler.instance().
            getMinecraftServerInstance().getEntityWorld()
        );
        Preconditions.checkNotNull(cwd);
    }

    public void onServerStopping() {
        Preconditions.checkState(cwd != null);
        cwd = null;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.START)
            cwd.tick();
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        clearCooldown(event.player);
    }

    static void cNetSetCd(EntityPlayer player, int id, int cd) {
        NetworkMessage.sendTo(player, staticDelegate, MSG_SYNCSET, id, cd);
    }

    @Listener(channel=MSG_SYNCSET, side=Side.CLIENT)
    @SideOnly(Side.CLIENT)
    private static void cIntSetCd(int id, int cd) {
        ClientRuntime.instance().setCooldownRawFromServer(id, cd);
    }

    @Listener(channel=MSG_SETSVR, side=Side.SERVER)
    private static void sIntSetCd(EntityPlayer player, int id, int cd) {
        setCooldownRaw(player, id, cd);
    }

    public static int getCtrlId(int idCtrl, int idSub) {
        return (idCtrl << 8) | (idSub & 0xff);
    }

    public static int getCtrlId(Controllable ctrl, int idSub) {
        return getCtrlId(ctrl.getControlID(), idSub);
    }

    public static int getCtrlId(int idCtrl) {
        return getCtrlId(idCtrl, 0);
    }

    public static int getCtrlId(Controllable ctrl) {
        return getCtrlId(ctrl, 0);
    }

}
