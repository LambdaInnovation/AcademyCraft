package cn.academy.api.ctrl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.academy.core.AcademyCraft;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * This class manages all SkillStates, both on client and on server.
 * @author acaly
 *
 */
@RegistrationClass
@RegEventHandler
public class SkillStateManager {
	
	private static Map<String, List<SkillState>> client = new HashMap();
	private static Map<String, List<SkillState>> server = new HashMap();

	private static final Map<String, List<SkillState>> getMapForSide(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			return client;
		} else {
			return server;
		}
	}
	
	/**
	 * Called by EventHandler.
	 * @param player
	 */
	static void removePlayer(EntityPlayer player) {
		Map<String, List<SkillState>> stateMap = getMapForSide(player);
		if (stateMap.containsKey(player)) {
			stateMap.remove(player);
		}
	}
	
	/**
	 * Add a new state. Called by SkillState.
	 * @param state
	 */
	static void addState(SkillState state) {
		Map<String, List<SkillState>> stateMap = getMapForSide(state.player);
		
		if (stateMap.containsKey(state.player)) {
			stateMap.get(state.player).add(state);
		} else {
			List<SkillState> list = new ArrayList();
			list.add(state);
			stateMap.put(state.player.getCommandSenderName(), list);
		}
	}

	/**
	 * Remove a finished state. Called by SkillState.
	 * @param state
	 */
	static void removeState(SkillState state) {
		Map<String, List<SkillState>> stateMap = getMapForSide(state.player);
		
		if (stateMap.containsKey(state.player)) {
			stateMap.get(state.player).remove(state);
		}
	}
	
	/**
	 * Internal use only. Use SkillState.removeState instead.
	 * @param player
	 * @param clazz
	 */
	public static void removeStateWithClass(EntityPlayer player, Class<? extends SkillState> clazz) {
		List<SkillState> playerList = getState(player);
		Iterator<SkillState> itor = playerList.iterator();
		while (itor.hasNext()) {
			SkillState state = itor.next();
			if (state.getClass().equals(clazz)) {
				state.reallyFinishSkill();
				itor.remove();
			}
		}
	}

	/**
	 * Get the first state with class of clazz for player.
	 * @param player
	 * @param clazz
	 * @return
	 */
	public static SkillState getStateWithClass(EntityPlayer player, 
			Class<? extends SkillState> clazz) {
		for (SkillState state : getState(player)) {
			if (state.getClass().equals(clazz)) {
				return state;
			}
		}
		return null;
	}
	
	/**
	 * Get all skill states of the given player. The result can not be modified.
	 * @param player
	 * @return
	 */
	public static List<SkillState> getState(EntityPlayer player) {
		Map<String, List<SkillState>> stateMap = getMapForSide(player);
		if (stateMap.containsKey(player)) {
			return Collections.unmodifiableList(stateMap.get(player));
		} else {
			return Collections.unmodifiableList(new ArrayList<SkillState>());
		}
	}
	
	/**
	 * Send tick event to all active State in the server map. Called by EventHandlerServer.
	 */
	static void tickServer() {
		for (List<SkillState> playerList : server.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (state.tickSkill()) {
					state.reallyFinishSkill();
					itor.remove();
				}
			}
		}
	}

	/**
	 * Send tick event to all active State in the client map. Called by EventHandlerClient.
	 */
	static void tickClient() {
		for (List<SkillState> playerList : client.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (state.tickSkill()) {
					state.reallyFinishSkill();
					itor.remove();
				}
			}
		}
		if (++clientTickRemovePlayer == 5000) {
			removePlayerOnClient();
		}
	}
	
	static int clientTickRemovePlayer = 0;
	
	@SideOnly(Side.CLIENT)
	private static void removePlayerOnClient() {
		World world = Minecraft.getMinecraft().theWorld;
		for (List<SkillState> playerList : client.values()) {
			Iterator<SkillState> itor = playerList.iterator();
			while (itor.hasNext()) {
				SkillState state = itor.next();
				if (world.getEntityByID(state.player.getEntityId()) != state.player) {
					state.reallyFinishSkill();
					itor.remove();
				}
			}
		}
	}
	
	//Only used by SkillStateMessage.
	//Should NEVER be called on server.
	static SkillState getStateById(EntityPlayer player, int id) {
		List<SkillState> playerList = client.get(player);
		if (playerList == null) return null;
		for (SkillState s : playerList) {
			if (s.stateID == id) return s;
		}
		return null;
	}
	
	static DimensionSkillStateMessage constructDimensionMessage(int dimension) {
	    DimensionSkillStateMessage ret = new DimensionSkillStateMessage();
	    ret.dimension = dimension;

        for (List<SkillState> playerList : server.values()) {
            for (SkillState state : playerList) {
                if (state.player.worldObj.provider.dimensionId != dimension) {
                    break;
                }
                ret.states.add(new SkillStateMessage(state, SkillStateMessage.Action.SYNC));
            }
        }
	    
	    return ret;
	}

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.entity;
            AcademyCraft.netHandler.sendTo(constructDimensionMessage(event.world.provider.dimensionId), player);
        }
    }
}
