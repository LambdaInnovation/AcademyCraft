package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.Controllable;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A <code>SkillInstance</code> represents a single time of ability control. <br>
 * A SkillInstance is created when the player presses the corresponding ability key
 *  for the Controllable, and is destroyed when player released the key or something
 *  unexpected occured (e.g. opening gui or player death). <br>
 * This class is CLIENT only. it is only created in the ability user's client. To 
 *  affect the server and the other clients, 
 *  you should use the SyncAction along with the whole Action system. <br>
 * SkillInstance can also mark some SyncAction as its child. When the SkillInstance ends, 
 *  the end(or abort) event will be automatically sent to those SyncActions.
 * @author WeAthFolD
 */
public abstract class SkillInstance {
	
	enum State { FINE, ENDED, ABORTED };
	
	State state = State.FINE;
	
	Controllable controllable;
	
	private List<SyncAction> childs;
	
	public SkillInstance() {}
	
	public void onStart() {}
	
	public void onTick() {}
	
	public void onEnd() {}
	
	public void onAbort() {}
	
	void ctrlEnded() { 
		onEnd();
		
		if(childs != null) {
			for(SyncAction act : childs)
				ActionManager.endAction(act);
		}
	}
	
	void ctrlAborted() { 
		onAbort();
		
		if(childs != null) {
			for(SyncAction act : childs)
				ActionManager.abortAction(act);
		}
	}
	
	/**
	 * End the SkillInstance next tick.
	 */
	protected void endSkill() {
		state = State.ENDED;
	}
	
	/**
	 * Abort the SkillInstance next tick.
	 */
	protected void abortSkill() {
		state = State.ABORTED;
	}
	
	/**
	 * Set a specific cooldown time for this controllable.
	 */
	protected final void setCooldown(int ticks) {
		Cooldown.setCooldown(controllable, ticks);
	}
	
	public void stopOnEnd(SyncAction action) {
		if(childs == null)
			childs = new ArrayList();
		
		childs.add(action);
	}
	
	@SideOnly(Side.CLIENT)
	protected EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().thePlayer;
	}
	
}
