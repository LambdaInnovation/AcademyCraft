package cn.academy.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.client.render.SkillRenderDebug;

/**
 * The state of an active skill.
 * This class is the base class of all states.
 * Used both on client and server.
 * On client, although ctrl only handles thePlayer, SkillState contains
 * skills of all players. This allows rendering of other players' skills.
 * 
 * The render is provided directly inside the SkillState.
 * @author acaly
 */
public class SkillState {
	
	/**
	 * Set on server. Used in sync.
	 * Note that for `the_player`, stateID is not valid,
	 * because states on the_player is not synchronized with server
	 * but is handled by client itself.
	 */
	public int stateID;
	
	/**
	 * Used on server. The next state id.
	 */
	public static int nextID;
	
	public final boolean isRemote;
	
	private int tickToFinish = 0;
	
	/**
	 * The per-state render
	 */
	private SkillRenderer render;
	
	public SkillState(EntityPlayer player) {
		this.player = player;
		
		isRemote = player.worldObj.isRemote;
		if(isRemote) {
			render = createRenderer();
		}
	}
	
	public final EntityPlayer player;
	
	public final void startSkill() {
		SkillStateManager.addState(this);
		
		onStart();
		if (!player.worldObj.isRemote) {
			//sync to client
			this.stateID = nextID++;
			AcademyCraftMod.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.START));
		}
	}
	
	public final void finishSkill() {
		//Finish the state next tick.
		//This can avoid modification of the player state list while iterating.
		finishSkillAfter(1);
	}
	
	public final void updateSkill() {
		if (!player.worldObj.isRemote) {
			AcademyCraftMod.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.UPDATE));
		}
	}
	
	public final void finishSkillAfter(int ticks) {
		if (ticks != 1 && tickToFinish != 0) {
			AcademyCraftMod.log.warn("Call finishSkillAfter more than once. Overwritten.");
		}
		tickToFinish = ticks;
	}
	
	public final void reallyFinishSkill() {
		onFinish();
		if (!player.worldObj.isRemote) {
			AcademyCraftMod.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.FINISH));
		}
	}
	
	protected void fromNBT(NBTTagCompound nbt) {}
	
	protected void toNBT(NBTTagCompound nbt) {}
	
	protected void onStart() {}
	
	protected void onFinish() {}
	
	/**
	 * Called when client receives an update message.
	 * Note that the player who is the owner of this state, will not receive update.
	 * @param nbt
	 */
	public void onUpdate(NBTTagCompound nbt) {
		fromNBT(nbt);
	}
	
	/**
	 * Will be called every tick while this state is active.
	 * @return Return true if you want to finish this state.
	 */
	protected boolean onTick() {
		return false;
	}
	
	/**
	 * Called at the initialization of SkillState. 
	 * Return a instance that can handle this state.
	 * You could create a new instance for each SkillState, 
	 * or you can use one instance for many SkillStates 
	 * (When no per-state data needs to be handled, this is more efficient)
	 * @return a SkillRender instance
	 */
	protected SkillRenderer createRenderer() {
		return SkillRenderer.EMPTY;
	}
	
	/**
	 * Get the SkillRender for this SkillState instance.
	 * @return the SkillRender instance
	 */
	public SkillRenderer getRender() {
		return render;
	}
	
	/**
	 * Called by SkillStateManager. Handle tick events in SkillState.
	 * @return Return true to indicate that this State is finished.
	 *          As this is called during iteration, we can't directly remove it.
	 */
	final boolean tickSkill() {
		if (tickToFinish != 0) {
			if (--tickToFinish == 0) {
				return true;
			}
		}
		return onTick();
	}
	
}
