package cn.academy.api.ctrl;

import cn.academy.core.AcademyCraftMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

/**
 * The state of an active skill.
 * This class is the base class of all states.
 * Used both on client and server.
 * On client, although ctrl only handles thePlayer, SkillState contains
 * skills of all players. This allows rendering of other players' skills.
 * @author acaly
 *
 */
public class SkillState {
	private int tickToFinish = 0;
	
	public SkillState(EntityPlayer player) {
		this.player = player;
	}
	
	public final EntityPlayer player;
	
	public final void startSkill() {
		SkillStateManager.addState(this);
		
		onStart();
		if (!player.worldObj.isRemote) {
			//sync to client
			AcademyCraftMod.netHandler.sendTo(new SkillStateMessage(this), 
					(EntityPlayerMP) player);
		}
	}
	
	public final void finishSkill() {
		onFinish();
		SkillStateManager.removeState(this);
	}
	
	public final void finishSkillAfter(int ticks) {
		if (tickToFinish != 0) {
			AcademyCraftMod.log.warn("Call finishSkillAfter more than once. Overwritten.");
		}
		tickToFinish = ticks;
	}
	
	protected void fromNBT(NBTTagCompound nbt) {}
	
	protected void toNBT(NBTTagCompound nbt) {}
	
	protected void onStart() {}
	
	protected void onFinish() {}
	
	/**
	 * Will be called every tick while this state is active.
	 * @return Return true if you want to finish this state.
	 */
	protected boolean onTick() {
		return false;
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
