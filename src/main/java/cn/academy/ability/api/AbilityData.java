/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.ability.api.event.CategoryChangedEvent;
import cn.academy.core.registry.RegDataPart;
import cn.academy.core.util.DataPart;
import cn.academy.core.util.PlayerData;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cpw.mods.fml.relauncher.Side;

/**
 * @author WeAthFolD
 */
@Registrant
@RegDataPart("ability")
public class AbilityData extends DataPart {
	
	private int catID = -1;
	private BitSet learnedSkills;

	public AbilityData() {
		learnedSkills = new BitSet(32);
	}
	
	/**
	 * Only effective in server. If c==null then set the player state to unlearned.
	 */
	public void setCategory(Category c) {
		setCategoryID(c == null ? -1 : c.getCategoryID());
	}
	
	/**
	 * Only effective in server. If id==-1 then set the player state to unlearned.
	 */
	public void setCategoryID(int id) {
		if(id != catID && !isRemote()) {
			catID = id;
			doCompleteSync();
			MinecraftForge.EVENT_BUS.post(new CategoryChangedEvent(getPlayer()));
		}
	}
	
	public boolean isLearned() {
		return catID >= 0;
	}
	
	public Category getCategory() {
		if(catID == -1)
			return null;
		return CategoryManager.INSTANCE.getCategory(catID);
	}
	
	/**
	 * Get all the learned skills. This method creates a new list.
	 */
	public List<Skill> getLearnedSkillList() {
		Category c = getCategory();
		if(c == null)
			return new ArrayList();
		return c.getSkillList()
				.stream()
				.filter((Skill s) -> learnedSkills.get(s.getID()))
				.collect(Collectors.toList());
	}
	
	public boolean isSkillLearned(Skill s) {
		return s.getCategory() == getCategory() && learnedSkills.get(s.getID());
	}
	
	@Override
	public void tick() {}

	@Override
	public void fromNBT(NBTTagCompound tag) {
		catID = tag.getByte("c");
		
		byte[] arr = tag.getByteArray("l");
		if(arr.length != 0)
			learnedSkills = BitSet.valueOf(arr);
	}

	@Override
	public NBTTagCompound toNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		
		tag.setByte("c", (byte) catID); //There cant be more than 128 categories yeah? >)
		tag.setByteArray("l", learnedSkills.toByteArray());
		
		return tag;
	}
	
	private void doCompleteSync() {
		receivedCompleteSync(getPlayer(), catID);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	private void receivedCompleteSync(@Target EntityPlayer player, @Data Integer catID) {
		this.catID = catID;
	}
	
	public static AbilityData get(EntityPlayer player) {
		return PlayerData.get(player).getPart(AbilityData.class);
	}

}
