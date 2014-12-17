package cn.academy.api.ctrl;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Handles event delegation to actual skills. Also stores player control info in case of querying.
 * Main class in ctrl package. Responsible for initialization of this part.
 * Directly interact with DataHandler (for data) and EventHandler (for event).
 * Note that there's one ControlHandler in client thread and one for each player in server thread.
 * There's no difference in it's behavior of client side and server side instances. 
 * @author acaly
 */

public class ControlHandler {
	
	/**
	 * True if it's a client side handler.
	 */
	public boolean isRemote;
	
	/**
	 * Category of this player. Used to get skill-key binding.
	 */
	AbilityData abilityData;

	public ControlHandler(EntityPlayer player) {
		abilityData = AbilityDataMain.getData(player);
	}
	
	/**
	 * Receive event from EventHandler (server or client).
	 * @param skillId
	 * @param type
	 */
	public void onEvent(int skillId, SkillEventType type, int time) {
		SkillBase skill = abilityData.getSkill(skillId);
		//TODO notice skill according to type. 
		switch (type) {
		case RAW_DOWN:
		case RAW_UP:
		//case RAW_TICK_UP:
		case RAW_TICK_DOWN:
		default:
			break;
		}
	}
	
	
}
