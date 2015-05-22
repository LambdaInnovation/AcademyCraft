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
import cn.academy.ability.api.Category;
import cn.academy.ability.api.CategoryManager;
import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;

/**
 * @author WeAthFolD
 */
@Registrant
@RegCommand
public class CommandAIM extends ACCommand {
	
	String[] commands = { "help", "cat", "catlist" };

	public CommandAIM() {
		super("aim");
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
			
			if(matchCommands(ics, player, newPars))
				return;
		}
		
		//Not using player parameter
		player = this.getCommandSenderAsPlayer(ics);
		matchCommands(ics, player, pars);
	}
	
	private boolean matchCommands(ICommandSender ics, EntityPlayer player, String[] pars) {
		AbilityData aData = AbilityData.get(player);
		switch(pars[0]) {
		case "help":
		{
			for(String c : commands) {
				sendChat(ics, getLoc(c));
			}
			return true;
		}
		case "cat":
		{
			if(pars.length == 1) {
				Category cat = aData.getCategory();
				sendChat(ics, getLoc("curcat"), cat == null ? getLoc("nonecat") : cat.getDisplayName());
				return true;
			} else if(pars.length == 2) {
				String catName = pars[1];
				Category cat = CategoryManager.INSTANCE.getCategory(catName);
				if(cat != null) {
					aData.setCategory(cat);
					sendChat(ics, locSuccessful());
				} else {
					sendChat(ics, getLoc("nocat"));
				}
				return true;
			} else {
				sendChat(ics, locInvalid());
			}
			break;	
		}
		case "catlist":
		{
			sendChat(ics, getLoc("cats"));
			StringBuilder sb = new StringBuilder();
			List<Category> catList = CategoryManager.INSTANCE.getCategories();
			for(int i = 0; i < catList.size(); ++i) {
				sb.append(catList.get(i).getName()).append(i == catList.size() - 1 ? "" : ", ");
			}
			sendChat(ics, sb.toString());
			return true;
		}
		}
		
		return false;
	}

}
