package cn.academy.ability.api.ctrl;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.client.ui.CPBar;
import cn.academy.ability.client.ui.CPBar.IConsumptionHintProvider;
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
public class SkillInstance implements IConsumptionHintProvider {
	
	enum State { CONSTRUCTED, FINE, ENDED, ABORTED };
	
	State state;
	
	Controllable controllable;
	
	private List<SyncAction> childs;
	
	private int ticks;
	
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
	
	public SkillInstance() {
		state = State.CONSTRUCTED;
	}
	
	public SkillInstance setNonMutex() {
		isMutex = false;
		return this;
	}
	
	public void onStart() {}
	
	public void onTick() {}
	
	public void onEnd() {}
	
	public void onAbort() {}
	
	void ctrlStarted() {
		state = State.FINE;
		
		CPBar.setHintProvider(this);
		onStart();
		
		if(childs != null) {
			for(SyncAction act : childs)
				ActionManager.startAction(act);
		}
	}
	
	void ctrlTick() {
		++ticks;
		onTick();
		
		for(SyncAction act : childs)
			if(act.getState() == SyncAction.State.ABORTED || 
				act.getState() == SyncAction.State.ENDED)
				this.abortSkill();
	}
	
	void ctrlEnded() { 
		onEnd();
		
		//System.out.println("SI#ENDED");
		state = State.ENDED;
		if(childs != null) {
			for(SyncAction act : childs)
				ActionManager.endAction(act);
		}
	}
	
	void ctrlAborted() { 
		onAbort();
		
		//System.out.println("SI#ABORTED");
		state = State.ABORTED;
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
	
	protected int getTicks() {
		return ticks;
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
	
	public SkillInstance setEstmCP(float amt) {
		this.estimatedCP = amt;
		return this;
	}
	
	@SideOnly(Side.CLIENT)
	protected EntityPlayer getPlayer() {
		return Minecraft.getMinecraft().thePlayer;
	}

	/**
	 * Return whether this SkillInstance is still in execution.
	 */
	@Override
	public boolean alive() {
		return state == State.FINE;
	}

	@Override
	public float getConsumption() {
		return estimatedCP;
	}
	
}
