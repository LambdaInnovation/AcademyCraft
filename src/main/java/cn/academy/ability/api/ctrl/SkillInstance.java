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
public class SkillInstance {
	
	enum State { FINE, ENDED, ABORTED };
	
	State state = State.FINE;
	
	Controllable controllable;
	
	private List<SyncAction> childs;
	
	/**
	 * Return: Whether this SkillInstance mutex others.
	 * Mutex SkillInstance will not be opened at the same time.
	 * ( That is, a player can only execute ONE mutex skill at once ).
	 */
	boolean isMutex = true;
	
	/**
	 * The estimated consumption of this SkillInstance when release.
	 * This will be drawn onto AbilityUI dynamically, when this instance is active.
	 */
	public float estimatedCP;
	
	public SkillInstance() {}
	
	public SkillInstance setNonMutex() {
		isMutex = false;
		return this;
	}
	
	public void onStart() {}
	
	public void onTick() {}
	
	public void onEnd() {}
	
	public void onAbort() {}
	
	void ctrlStarted() {
		onStart();
		
		if(childs != null) {
			for(SyncAction act : childs)
				ActionManager.startAction(act);
		}
	}
	
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
	
	/**
	 * Add a sub SyncAction to be automatically started at SI execution and 
	 *  end(abort)ed at end.
	 */
	public SkillInstance addChild(SyncAction action) {
		if(childs == null)
			childs = new ArrayList();
		
		childs.add(action);
		return this;
	}
	
	@SideOnly(Side.CLIENT)
	protected EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().thePlayer;
	}
	
}
