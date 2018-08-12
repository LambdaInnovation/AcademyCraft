package cn.academy.ability.api.ctrl;

import cn.academy.AcademyCraft;
import cn.lambdalib2.s11n.network.NetworkMessage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.*;

/**
 * @author EAirPeter
 */
public class AMServer implements IActionManager {

    private static final Object AMDelegate = NetworkMessage.staticCaller(ActionManager.class);

    AMServer() {
        FMLCommonHandler.instance().bus().register(this);
        map.put(dummy, new HashMap<UUID, SyncAction>());
    }
    
    private Map<UUID, Map<UUID, SyncAction>> map = new HashMap<UUID, Map<UUID, SyncAction>>();
    private List<UUID> off = new LinkedList<UUID>();
    
    private static final UUID dummy = UUID.randomUUID();
    
    @Override
    public void startAction(SyncAction action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void endAction(SyncAction action) {
        //System.out.println("AMS#INT_END");
        SyncAction _action = map.get(playerUUID(action)).get(action.uuid);

        NBTTagCompound tag = _action == null ? SyncAction.TAG_EMPTY : _action.end();
        NetworkMessage.sendToAll(AMDelegate, ActionManager.M_END_CLIENT,
                (_action == null ? action : _action).uuid.toString(), tag);
    }

    @Override
    public void abortAction(SyncAction action) {
        //System.out.println("AMS#INT_ABORT");
        SyncAction _action = map.get(playerUUID(action)).get(action.uuid);
        if (_action == null)
            sendAbortAtClient(action.uuid.toString(), SyncAction.TAG_EMPTY);
        else
            sendAbortAtClient(_action.uuid.toString(), _action.abort());
    }
    
    @Override
    public SyncAction findAction(EntityPlayer player, Class clazz) {
        Map<UUID, SyncAction> _map = map.get(playerUUID(player));
        if (_map == null)
            return null;
        for (SyncAction action : _map.values())
            if (clazz.isInstance(action))
                return action;
        return null;
    }
    
    boolean startFromClient(EntityPlayer player, String className, NBTTagCompound tag) {
        //System.out.println("AMS#NET_START");
        SyncAction action = null;
        try {
            action = (SyncAction) Class.forName(className).newInstance();
            action.player = player;
            action.setNBTStart(tag);
            Map<UUID, SyncAction> sub = map.get(playerUUID(action));
            if (!sub.containsKey(action.uuid)) {
                NetworkMessage.sendTo(player, AMDelegate, ActionManager.M_START_CLIENT, player, className, action.getNBTStart());
                sub.put(action.uuid, action);
                action.start();
            }
            else
                AcademyCraft.log.warn("An action with UUID(" + action.uuid + ") has already existed!");
            return true;
        }
        catch (Throwable e) {
            AcademyCraft.log.error("Failed to create an action", e);
            return false;
        }
    }

    void endFromClient(EntityPlayer player, UUID uuid) {
        //System.out.println("AMS#NET_END");
        SyncAction action = map.get(player.getUniqueID()).get(uuid);
        if (action == null)
            sendAbortAtClient(uuid.toString(), SyncAction.TAG_EMPTY);
        else
            endAction(action);
    }

    void abortFromClient(EntityPlayer player, UUID uuid) {
        //System.out.println("AMS#NET_ABORT");
        SyncAction action = map.get(player.getUniqueID()).get(uuid);
        if (action == null)
            sendAbortAtClient(uuid.toString(), SyncAction.TAG_EMPTY);
        else
            abortAction(action);
    }
    
    void abortPlayer(EntityPlayer player) {
        //System.out.println("AMS#NET_APLAYER");
        Map<UUID, SyncAction> _map = map.get(player.getUniqueID());
        if (_map != null)
            for (SyncAction action : _map.values())
                abortAction(action);
    }

    private void sendAbortAtClient(String uuid, NBTTagCompound tag) {
        NetworkMessage.sendToAll(AMDelegate, ActionManager.M_ABORT_CLIENT, uuid, tag);
    }

    private UUID playerUUID(SyncAction action) {
        return playerUUID(action.player);
    }
    
    private UUID playerUUID(EntityPlayer player) {
        return player == null ? dummy : player.getUniqueID();
    }
    
    private int curTick = 0;
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase.equals(Phase.START))
            return;
        ++curTick;
        for (Map<UUID, SyncAction>  m : map.values())
            for (Iterator<SyncAction> i = m.values().iterator(); i.hasNext(); ) {
                SyncAction action = i.next();
                if (action.player != null && action.player.getHealth() <= 0)
                    abortAction(action);
                switch (action.getState()) {
                case CREATED:
                    throw new IllegalStateException();
                case STARTED:
                    action.onTick();
                    if (action.intv > 0 && curTick - action.lastInformed >= action.intv) {
                        action.lastInformed = curTick;
                        NetworkMessage.sendToAll(AMDelegate, ActionManager.M_UPDATE_CLIENT, action.uuid.toString(), action.getNBTUpdate());
                    }
                    break;
                case ENDED:
                case ABORTED:
                    i.remove();
                    break;
                default:
                    break;
                }
            }
        for (UUID u : off)
            map.remove(u);
        off.clear();
    }
    
    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        abortPlayer(event.player);
    }
    
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        map.put(event.player.getUniqueID(), new HashMap<UUID, SyncAction>());
    }
    
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        abortPlayer(event.player);
        off.add(event.player.getUniqueID());
    }

}