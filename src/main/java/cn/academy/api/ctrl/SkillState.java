/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.api.client.render.SkillRenderer;
import cn.academy.core.AcademyCraft;

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
	
	private boolean alive = false;
	
	private int tickTime = 0;
	
	/**
	 * The per-state render
	 */
	private SkillRenderer render;
	
	public SkillState(EntityPlayer player) {
		this.player = player;
		
		isRemote = player.worldObj.isRemote;
	}
	
	public final EntityPlayer player;
	
	public final void startSkill() {
		SkillStateManager.addState(this);
		
		onStart();
		alive = true;
		if (!player.worldObj.isRemote) {
			//sync to client
			this.stateID = nextID++;
			//AcademyCraft.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.START));
		}
	}
	
	public final void finishSkill() {
		//Finish the state next tick.
		//This can avoid modification of the player state list while iterating.
		finishSkillAfter(1);
	}
	
	public final void updateSkill() {
		if (!player.worldObj.isRemote) {
			//AcademyCraft.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.UPDATE));
		}
	}
	
	public final void finishSkillAfter(int ticks) {
		if (ticks != 1 && tickToFinish != 0) {
			AcademyCraft.log.warn("Call finishSkillAfter more than once. Overwritten.");
		}
		tickToFinish = ticks;
	}
	
	public final void reallyFinishSkill() {
		onFinish();
		alive = false;
		if (!player.worldObj.isRemote) {
			//AcademyCraft.netHandler.sendToAll(new SkillStateMessage(this, SkillStateMessage.Action.FINISH));
		}
	}
	
	/**
	 * Return if this SkillState is in the executing queue.
	 */
	public final boolean isAlive() {
		return alive;
	}
	
	protected void fromNBT(NBTTagCompound nbt) {}
	
	protected void toNBT(NBTTagCompound nbt) {}
	
	protected void onStart() {}
	
	protected void onFinish() {}
	
	/**
	 * Will be called every tick while this state is active.
	 * @param time Ticks from the start of this State.
	 * @return Return true if you want to finish this state.
	 */
	protected boolean onTick(int time) {
		return false;
	}
	
	protected int getTickTime() {
		return tickTime;
	}
	
	/**
	 * Called when client receives an update message.
	 * Note that the player who is the owner of this state, will not receive update.
	 * @param nbt
	 */
	public void onUpdate(NBTTagCompound nbt) {
		fromNBT(nbt);
	}
	
	public boolean isRemote() {
		return player.worldObj.isRemote;
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
		return onTick(++tickTime);
	}
}
