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
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.ability.api.ctrl.ClientHandler;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cn.annoreg.mc.s11n.SerializationManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * SpecialSkill is a kind of skill that OVERRIDES current preset. Once activated,
 * 	it creates a Preset Override that remaps all the SubSkill onto player control.
 * 
 * @see cn.academy.ability.api.SubSkill
 * @author WeAthFolD
 */
public abstract class SpecialSkill extends Skill {
	
	List<SubSkill> subSkills = new ArrayList();

	public SpecialSkill(String _name, int atLevel) {
		super(_name, atLevel);
	}
	
	@Override
	protected void initSkill() {
		Category cat = getCategory();
		
		for(SubSkill s : subSkills) {
			s.addedInto(this);
			cat.addControllable(s);
		}
	}
	
	/**
	 * Add a sub skill. This must be called before a SpecialSkill is added into Category, 
	 * 	 so that creation event can be correctly sent to SubSkills.
	 * @param skill The SubSkill to add
	 */
	public void addSubSkill(SubSkill skill) {
		if(this.getCategory() != null) {
			throw new IllegalStateException("You must add SubSkills first!");
		}
		
		subSkills.add(skill);
	}
	
	/**
	 * Called when player started SpecialSkill. Validate at SERVER to proceed.
	 * @param player
	 * @return
	 */
	public boolean validateExecution(EntityPlayer player) {
		return true;
	}
	
	/**
	 * Called in both client and server when executing the SpecialSkill.
	 */
	public void execute(EntityPlayer player) {}
	
	@Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addExecution(new SSAction(this));
	}
	
	public static class SSAction extends SyncActionInstant {
		
		static final InstanceSerializer<Controllable> ctrlSer = new ControllableSerializer();
		SpecialSkill skill;
		
		public SSAction(SpecialSkill _skill) {
			skill = _skill;
		}
		
		public SSAction() {}
		
		@Override
		public void readNBTStart(NBTTagCompound tag) {
			try {
				skill = (SpecialSkill) ctrlSer.readInstance(tag.getTag("s"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void writeNBTStart(NBTTagCompound tag) {
			try {
				tag.setTag("s", ctrlSer.writeInstance(skill));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public boolean validate() {
			return skill.validateExecution(player);
		}

		@Override
		public void execute() {
			PresetData pData = PresetData.get(player);
			Preset preset = pData.createPreset();
			
			int nNormal = 0, nRemap = 0;
			for(SubSkill ss : skill.subSkills) {
				if(ss.isRemapped()) {
					if(++nRemap > 4) {
						throw new RuntimeException("There can't be more than 4 remap SubSkills.");
					}
					
					ClientHandler.remap(nRemap - 1, ss.getRemappedKey());
					preset.data[4 + (nRemap - 1)] = (byte) ss.getControlID();
				} else {
					if(++nNormal > 4) {
						throw new RuntimeException("There can't be more than 4 normal SubSkills.");
					}
					
					preset.data[nNormal - 1] = (byte) ss.getControlID();
				}
			}
			
			pData.override(preset);
			
			skill.execute(player);
		}
		
	}

}
