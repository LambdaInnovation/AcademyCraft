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
package cn.academy.ability.command;

import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import cn.academy.ability.api.AbilityData;
import cn.academy.ability.api.CPData;
import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.Skill;
import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;

/**
 * @author WeAthFolD
 */
@Registrant
public abstract class CommandAIMBase extends ACCommand {
	
	@RegCommand
	public static class CommandAIM extends CommandAIMBase {

		public CommandAIM() {
			super("aim");
		}
		
		@Override
		public void processCommand(ICommandSender ics, String[] pars) {
			if(pars.length == 0) {
				sendChat(ics, getLoc("help"));
				return;
			}
			
			matchCommands(ics, this.getCommandSenderAsPlayer(ics), pars);
		}
		
	}
	
	@RegCommand
	public static class CommandAIMP extends CommandAIMBase {

		public CommandAIMP() {
			super("aimp");
		}
		
		@Override
		public void processCommand(ICommandSender ics, String[] pars) {
			if(pars.length == 0) {
				sendChat(ics, getLoc("help"));
				return;
			}
			
			//Try to locate the player.
			EntityPlayer player = null;
			
			//Using player parameter
			for(Object p : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
				EntityPlayer p2 = (EntityPlayer) p;
				if(p2.getCommandSenderName().equals(pars[0])) {
					player = p2;
					break;
				}
			}
			
			if(player != null) {
				String[] newPars = new String[pars.length - 1];
				for(int i = 0; i < newPars.length; ++i) {
					newPars[i] = pars[i + 1];
				}
				
				matchCommands(ics, player, newPars);
			} else {
				sendChat(ics, locNoPlayer());
			}
		}
		
	}
	
	String[] commands = {
		"help", "cat", "catlist", 
		"learn", "learn_all", "reset",
		"learned", "skills", "fullcp",
		"level"
	};

	public CommandAIMBase(String name) {
		super(name);
		localName = "aim";
	}
	
	protected void matchCommands(ICommandSender ics, EntityPlayer player, String[] pars) {
		AbilityData aData = AbilityData.get(player);
		switch(pars[0]) {
		case "help": {
			for(String c : commands) {
				sendChat(ics, getLoc(c));
			}
			return;
		}
		
		case "cat": {
			if(pars.length == 1) {
				Category cat = aData.getCategory();
				sendChat(ics, getLoc("curcat"), cat == null ? getLoc("nonecat") : cat.getDisplayName());
				return;
			} else if(pars.length == 2) {
				String catName = pars[1];
				Category cat = CategoryManager.INSTANCE.getCategory(catName);
				if(cat != null) {
					aData.setCategory(cat);
					sendChat(ics, locSuccessful());
				} else {
					sendChat(ics, getLoc("nocat"));
				}
				return;
			}
			break;	
		}
		
		case "catlist": {
			sendChat(ics, getLoc("cats"));
			StringBuilder sb = new StringBuilder();
			List<Category> catList = CategoryManager.INSTANCE.getCategories();
			for(int i = 0; i < catList.size(); ++i) {
				sb.append(catList.get(i).getName()).append(i == catList.size() - 1 ? "" : ", ");
			}
			sendChat(ics, sb.toString());
			return;
		}
		
		case "learn": {
			Category cat = aData.getCategory();
			if(cat == null) {
				sendChat(ics, getLoc("nocat"));
				return;
			}
			
			if(pars.length != 2)
				return;
			else {
				Integer i = null;
				try {
					i = Integer.valueOf(pars[1]);
				} catch(NumberFormatException e) {}
				if(i != null) { // Parse as id
					if(i < 0 || i >= cat.getSkillCount()) {
						sendChat(ics, getLoc("noskill"));
						return;
					}
					aData.learnSkill(i);
					sendChat(ics, locSuccessful());
				} else { // Using skill name
					Skill s = cat.getSkill(pars[1]);
					if(s != null) {
						aData.learnSkill(s);
						sendChat(ics, locSuccessful());
					} else {
						sendChat(ics, getLoc("noskill"));
					}
				}
			}
			return;
		}
		
		case "learn_all": {
			if(aData.getCategory() == null) {
				sendChat(ics, getLoc("nocat"));
				return;
			}
			aData.learnAllSkills();
			sendChat(ics, locSuccessful());
			return;
		}
		
		case "reset": {
			aData.setCategory(null);
			sendChat(ics, locSuccessful());
			return;
		}
		
		case "learned": {
			StringBuilder sb = new StringBuilder();
			
			boolean begin = true;
			for(Skill s : aData.getLearnedSkillList()) {
				sb.append(begin ? "" : ", " + s.getName());
				begin = false;
			}
			
			sendChat(ics, getLoc("learned.format"), sb.toString());
			return;
		}
		
		case "skills": {
			Category cat = aData.getCategory();
			if(cat == null) {
				sendChat(ics, getLoc("nocat"));
				return;
			}
			
			for(Skill s : cat.getSkillList()) {
				sendChat(ics, s.getName() + " [" + s.getID() + "]");
			}
			return;
		}
		
		case "level": {
			int lv = Integer.valueOf(pars[1]);
			if(lv > 0 && lv <= 5) {
				aData.setLevel(lv);
				sendChat(ics, locSuccessful());
			} else {
				sendChat(ics, locInvalid());
			}
			return;
		}
		
		case "fullcp": {
			CPData cpData = CPData.get(player);
			cpData.setCP(cpData.getMaxCP());
			sendChat(ics, locSuccessful());
			return;
		}
		}
		
		return;
	}

}
