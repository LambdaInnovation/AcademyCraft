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

import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

/**
 * @author WeAthFolD
 */
@Registrant
public abstract class CommandAIMBase extends ACCommand {
	
	/**
	 * This is the command used by the client, doesn't specify the player and works on the user.
	 * This command will display a warning before you can use it.
	 */
	@RegCommand
	public static class CommandAIM extends CommandAIMBase {
		
		static final String ID = "aim_cheats";

		public CommandAIM() {
			super("aim");
		}
		
		@Override
		public void processCommand(ICommandSender commandSender, String[] pars) {
			EntityPlayer player = super.getCommandSenderAsPlayer(commandSender);
			if(pars.length == 1) {
				switch(pars[0]) {
				case "cheats_on":
					setActive(player, true);
					sendChat(commandSender, locSuccessful());
					sendChat(commandSender, getLoc("warning"));
					return;
				case "cheats_off":
					setActive(player, false);
					sendChat(commandSender, locSuccessful());
					return;
				}
			}
			
			if(!isActive(player)) {
				sendChat(commandSender, getLoc("notactive"));
				sendChat(commandSender, getLoc("warning"));
				return;
			}
			
			if(pars.length == 0) {
				sendChat(commandSender, getLoc("help"));
				return;
			}
			
			matchCommands(commandSender, this.getCommandSenderAsPlayer(commandSender), pars);
		}
		
		private void setActive(EntityPlayer player, boolean data) {
			player.getEntityData().setBoolean(ID, data);
		}
		
		private boolean isActive(EntityPlayer player) {
			return player.getEntityData().getBoolean(ID);
		}
		
	}
	
	/**
	 * This is the command for the OPs and server console. You must specify the player name.
	 */
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
		"level", "exp"
	};

	public CommandAIMBase(String name) {
		super(name);
		localName = "aim";
	}
	
	protected void matchCommands(ICommandSender ics, EntityPlayer player, String[] pars) {
		AbilityData aData = AbilityData.get(player);
		switch(pars[0]) {
		case "?":
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
			
			Skill s = tryParseSkill(cat, pars[1]);
			if(s == null) {
				sendChat(ics, getLoc("noskill"));
			} else {
				aData.learnSkill(s);
			}
			return;
		}
		
		case "unlearn": {
			Category cat = aData.getCategory();
			if(cat == null) {
				sendChat(ics, getLoc("nocat"));
				return;
			}
			
			Skill s = tryParseSkill(cat, pars[1]);
			if(s == null) {
				sendChat(ics, getLoc("noskill"));
			} else {
				aData.setSkillLearnState(s, false);
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
			
			if(pars.length == 1) {
				sendChat(ics, "" + aData.getLevel());
			} else {
				int lv = Integer.valueOf(pars[1]);
				if(lv > 0 && lv <= 5) {
					aData.setLevel(lv);
					sendChat(ics, locSuccessful());
				} else {
					sendChat(ics, locInvalid());
				}
			}
			
			return;
		}
		
		case "fullcp": {
			CPData cpData = CPData.get(player);
			cpData.setCP(cpData.getMaxCP());
			sendChat(ics, locSuccessful());
			return;
		}
		
		case "exp": {
			Category cat = aData.getCategory();
			if(cat == null) {
				sendChat(ics, getLoc("nocat"));
				return;
			}
			
			Skill skill = tryParseSkill(cat, pars[1]);
			
			if(skill == null) {
				sendChat(ics, getLoc("noskill"));
			} else {
				if(pars.length == 2) {
					sendChat(ics, this.getLoc("curexp"), skill.getDisplayName(), aData.getSkillExp(skill) * 100);
				} else if(pars.length == 3) {
					Float exp = tryParseFloat(pars[2]);
					aData.setSkillExp(skill, exp);
					sendChat(ics, this.locSuccessful());
				} else {
					sendChat(ics, this.locInvalid());
				}
			}
			
			return;
		}
		}
		
		return;
	}
	
	private Integer tryParseInt(String str) {
		try {
			return Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	private Float tryParseFloat(String str) {
		try {
			return Float.parseFloat(str);
		} catch(NumberFormatException e) {
			return null;
		}
	}
	
	private Skill tryParseSkill(Category cat, String str) {
		if(cat == null)
			return null;
		Integer i = tryParseInt(str);
		if(i != null)
			return cat.getSkill(i);
		return cat.getSkill(str);
	}

}
