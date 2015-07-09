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
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.ClientController;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.annoreg.mc.s11n.InstanceSerializer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * SpecialSkill is a kind of skill that OVERRIDES current preset. Once activated,
 * 	it creates a Preset Override that remaps all the SubSkill onto player control.
 *  At the same time it creates a SpecialSkillAction that can be treated as 'environment'
 *  of this execution. When you end this action, the override is ended; When override is
 *  ended, this action is ended/aborted to. You can do something (e.g. consume CP) within
 *  this SpecialSkillAction.
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
	 * Will only get called in CLIENT. Return a SpecialSkillAction that
	 * 	automatically starts when player enters SpecialSkill.
	 */
	protected SpecialSkillAction getSpecialAction(EntityPlayer player) {
		return new DefaultSSAction(this);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
    public final SkillInstance createSkillInstance(EntityPlayer player) {
		return new SkillInstanceInstant().addExecution(new ExecuteAction(this));
	}
	
	/**
	 * SpecialSkillAction provides an environment for SpecialSkill. Every time player enters a
	 * 	SpecialSkill, a corresponding SpecialSkillAction is opened. Aborting or ending that
	 *  action will end the overriding state as well, and when override is ended the action is also ended.
	 * @author WeAthFolD
	 */
	public static class SpecialSkillAction extends SyncAction {
		
		static final InstanceSerializer<Controllable> ctrlSer = new ControllableSerializer();
		SpecialSkill skill;
		PresetData presetData;
		
		public SpecialSkillAction(SpecialSkill _skill, int interval) {
			super(interval);
			skill = _skill;
		}
		
		public SpecialSkillAction(int interval) {
			super(interval);
		}
		
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
		public final void onStart() {
			presetData = PresetData.get(player);
			onSkillStart();
		}
		
		@Override
		public final void onTick() {
			if(!presetData.isOverriding()) {
				ActionManager.endAction(this);
			} else {
				onSkillTick();
			}
		}
		
		@Override
		public final void onEnd() {
			presetData.endOverride();
			
			onSkillEnd();
		}
		
		@Override
		public final void onAbort() {
			presetData.endOverride();
			
			onSkillAbort();
		}
		
		/*
		 * EVENT DELEGATES
		 * Those methods works exactly the same as SyncActions.
		 * extend and override them
		 */
		
		protected void onSkillStart() {
			System.out.println("[SS]Started");
		}
		
		protected void onSkillTick() {
			System.out.println("[SS]Ticked");
		}
		
		protected void onSkillEnd() {
			System.out.println("[SS]Ended");
		}
		
		protected void onSkillAbort() {
			System.out.println("[SS]Aborted");
		}
	}
	
	public static final class DefaultSSAction extends SpecialSkillAction {
		public DefaultSSAction(SpecialSkill _skill) {
			super(_skill, -1);
		}
		
		public DefaultSSAction() {
			super(-1);
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static final class ExecuteAction extends SyncActionInstant {
		
		static final InstanceSerializer<Controllable> ctrlSer = new ControllableSerializer();
		SpecialSkill skill;
		
		public ExecuteAction(SpecialSkill _skill) {
			skill = _skill;
		}
		
		public ExecuteAction() {}
		
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
					
					ClientController.remap(nRemap - 1, ss.getRemappedKey());
					preset.data[4 + (nRemap - 1)] = (byte) ss.getControlID();
				} else {
					if(++nNormal > 4) {
						throw new RuntimeException("There can't be more than 4 normal SubSkills.");
					}
					
					preset.data[nNormal - 1] = (byte) ss.getControlID();
				}
			}
			
			pData.override(preset);
			
			if(isRemote) {
				ActionManager.startAction(skill.getSpecialAction(player));
			}
		}
		
	}

}
