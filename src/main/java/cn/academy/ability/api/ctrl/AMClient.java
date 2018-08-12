package cn.academy.ability.api.ctrl;

import cn.academy.AcademyCraft;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.auxgui.OpenAuxGuiEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.*;

/**
 * @author EAirPeter
 */
public class AMClient implements IActionManager {

    AMClient() {
        if (FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT)) {
            FMLCommonHandler.instance().bus().register(this);
            MinecraftForge.EVENT_BUS.register(this);
            map = new HashMap<UUID, SyncAction>();
            set = new HashSet<UUID>();
        }
    }
    
    private Map<UUID, SyncAction> map = null;
    //Optimized: abortPlayer
    private Set<UUID> set = null;

    private static final Object AMDelegate = NetworkMessage.staticCaller(ActionManager.class);
    
    @Override
    @SideOnly(Side.CLIENT)
    public void startAction(SyncAction action) {
        //System.out.println("AMC#INT_START");
        action.player = Minecraft.getMinecraft().thePlayer;
        map.put(action.uuid, action);
        NBTTagCompound tag = action.getNBTStart();

        NetworkMessage.sendToServer(AMDelegate, ActionManager.M_START_SVR, Minecraft.getMinecraft().thePlayer, action.getClass().getName(), tag);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void endAction(SyncAction action) {
        NetworkMessage.sendToServer(AMDelegate, ActionManager.M_END_SVR, Minecraft.getMinecraft().thePlayer, action.uuid.toString());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void abortAction(SyncAction action) {
        NetworkMessage.sendToServer(AMDelegate, ActionManager.M_ABORT_SVR, Minecraft.getMinecraft().thePlayer, action.uuid.toString());
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public SyncAction findAction(EntityPlayer player, Class clazz) {
        for (SyncAction action : map.values())
            if (clazz.isInstance(action)) {
                if (player == null && action.player == null)
                    return action;
                if (player != null && action.player != null && player.equals(action.player))
                    return action;
            }
        return null;
    }
    
    @SideOnly(Side.CLIENT)
    void startFromServer(EntityPlayer player, String className, NBTTagCompound tag) {
        //System.out.println("AMC#NET_START");
        SyncAction action = map.get(SyncAction.getUUIDFromNBT(tag));
        if (action == null)
            try {
                action = (SyncAction) Class.forName(className).newInstance();
                action.player = player;
                action.setNBTStart(tag);
                map.put(action.uuid, action);
            }
            catch (Throwable e) {
                AcademyCraft.log.error("Failed to create an action", e);
            }
        else
            action.setNBTStart(tag);
        if (player != null && player.equals(Minecraft.getMinecraft().thePlayer))
            set.add(action.uuid);
        action.start();
    }

    @SideOnly(Side.CLIENT)
    void updateFromServer(UUID uuid, NBTTagCompound tag) {
        //System.out.println("AMC#NET_UPDATE");
        SyncAction action = map.get(uuid);
        if (action != null)
            action.setNBTUpdate(tag);
    }
    
    @SideOnly(Side.CLIENT)
    void endFromServer(UUID uuid, NBTTagCompound tag) {
        //System.out.println("AMC#NET_END");
        SyncAction action = map.get(uuid);
        if (action != null)
            action.end(tag);
    }
    
    @SideOnly(Side.CLIENT)
    void abortFromServer(UUID uuid, NBTTagCompound tag) {
        //System.out.println("AMC#NET_ABORT");
        SyncAction action = map.get(uuid);
        if (action != null)
            action.abort(tag);
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientTick(ClientTickEvent event) {
        if (Minecraft.getMinecraft().isGamePaused() || event.phase.equals(Phase.START))
            return;
        for (Iterator<SyncAction> i = map.values().iterator(); i.hasNext(); ) {
            SyncAction action = i.next();
            switch (action.getState()) {
            case CREATED:
                break;
            case STARTED:
                action.onTick();
                break;
            case ENDED:
            case ABORTED:
                i.remove();
                set.remove(action.uuid);
                break;
            default:
                break;
            }
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onClientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
        for (Iterator<SyncAction> i = map.values().iterator(); i.hasNext(); ) {
            SyncAction action = i.next();
            action.abort();
            i.remove();
        }
        set.clear();
    }
    
    @SideOnly(Side.CLIENT)
    private void abortPlayer() {
        //System.out.println("AMC#PRI_APLAYER");
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && !set.isEmpty()) {
            NetworkMessage.sendToServer(AMDelegate, ActionManager.M_ABORT_PLAYER_SVR, player);
        }
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null)
            abortPlayer();
    }
    
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onOpenAuxGui(OpenAuxGuiEvent event) {
        if (event.gui != null && event.gui.isForeground())
            abortPlayer();
    }

}