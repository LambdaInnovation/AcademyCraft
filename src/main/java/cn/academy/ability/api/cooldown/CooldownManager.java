package cn.academy.ability.api.cooldown;

import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.context.ClientRuntime;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEventHandler;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Manages cooldown cwd for all players on server.
 * @author EAirPeter
 */
@Registrant
public class CooldownManager {

    @RegEventHandler
    public static final CooldownManager INSTANCE = new CooldownManager();

    private CooldownWorldData cwd = null;

    private CooldownManager() {
    }

    public static void setCooldown(EntityPlayer player, int idCtrl, int idSub,
        int cd)
    {
        INSTANCE.cwd.setCd(player.getCommandSenderName(),
            getCtrlId(idCtrl, idSub), cd);
    }

    public static boolean isInCooldown(EntityPlayer player, int idCtrl,
        int idSub)
    {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(),
            getCtrlId(idCtrl, idSub));
    }

    public static void clearCooldown(EntityPlayer player) {
        INSTANCE.cwd.clearCd(player.getCommandSenderName());
    }

    public static void setCooldown(EntityPlayer player, Controllable ctrl,
        int idSub, int cd)
    {
        INSTANCE.cwd.setCd(player.getCommandSenderName(),
            getCtrlId(ctrl, idSub), cd);
    }

    public static boolean isInCooldown(EntityPlayer player, Controllable ctrl,
        int idSub)
    {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(),
            getCtrlId(ctrl, idSub));
    }

    public static void setCooldown(EntityPlayer player, int idCtrl, int cd) {
        INSTANCE.cwd.setCd(player.getCommandSenderName(),
            getCtrlId(idCtrl), cd);
    }

    public static boolean isInCooldown(EntityPlayer player, int idCtrl) {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(),
            getCtrlId(idCtrl));
    }

    public static void setCooldown(EntityPlayer player, Controllable ctrl,
        int cd)
    {
        INSTANCE.cwd.setCd(player.getCommandSenderName(), getCtrlId(ctrl), cd);
    }

    public static boolean isInCooldown(EntityPlayer player, Controllable ctrl) {
        return INSTANCE.cwd.isInCd(player.getCommandSenderName(),
            getCtrlId(ctrl));
    }

    public void onServerStarting() {
        System.out.println("ONSERVERSTARTING");
        Preconditions.checkState(cwd == null);
        cwd = CooldownWorldData.load(FMLCommonHandler.instance().
            getMinecraftServerInstance().getEntityWorld()
        );
        Preconditions.checkNotNull(cwd);
    }

    public void onServerStopping() {
        System.out.println("ONSERVERSTOPPING");
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

    @RegNetworkCall(side = Side.CLIENT)
    public static void cNetSetCd(@Instance EntityPlayer player, @Data Integer id,
        @Data Integer cd)
    {
        cIntSetCd(id, cd);
    }

    @SideOnly(Side.CLIENT)
    private static void cIntSetCd(int id, int cd) {
        ClientRuntime.instance().setCooldownRawFromServer(id, cd);
    }

    public static int getCtrlId(int idCtrl, int idSub) {
        return (idCtrl << 8) | (idSub & 0xff);
    }

    public static int getCtrlId(Controllable ctrl, int idSub) {
        return (ctrl.getControlID() << 8) | (idSub & 0xff);
    }

    public static int getCtrlId(int idCtrl) {
        return (idCtrl << 8) | 0xff;
    }

    public static int getCtrlId(Controllable ctrl) {
        return (ctrl.getControlID() << 8) | 0xff;
    }

}
