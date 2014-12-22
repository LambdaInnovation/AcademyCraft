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
	
	public void fromNBT(NBTTagCompound nbt) {}
	
	public void toNBT(NBTTagCompound nbt) {}
	
	public void onStart() {}
	
	public void onFinish() {}
}
