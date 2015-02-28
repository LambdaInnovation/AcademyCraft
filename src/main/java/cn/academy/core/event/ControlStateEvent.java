/**
 * 
 */
package cn.academy.core.event;

import cn.academy.api.ctrl.EventHandlerClient;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only event. Fired when player activates/deactivates the ability.
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class ControlStateEvent extends Event {
	
	public final boolean newState;
	
	public ControlStateEvent() {
		newState = EventHandlerClient.isSkillEnabled();
	}

}
