/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import cn.academy.api.ability.Abilities;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.template.command.LICommandBase;

/**
 * Cheating commands.
 * @author WeathFolD
 */
public class CmdCheating extends LICommandBase {
	
	/*
	 * Usage:
	 *  /aim playerID keyName ?/<parameters>
	 *  If pars is "?", the command will display the help information.
	 */

	final Map<String, Call> calls = new HashMap();
	{
		calls.put("?", new Help());
		calls.put("cat", new Category());
		calls.put("level", new Level());
		calls.put("god", new God());
		calls.put("cp", new CP());
		calls.put("maxcp", new MaxCP());
		calls.put("clear", new Clear());
	}
	
	public CmdCheating() {}
	
    @Override
	public int getRequiredPermissionLevel() {
        return 2; //OP+cheating
    }

	@Override
	public String getCommandName() {
		return "aim";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "commands.aim.desc";
	}

	@Override
	public void processCommand(ICommandSender ics, String[] pars) {
		if(pars.length  == 0) {
			LICommandBase.sendChat(ics, getCommandUsage(ics));
			return;
		}
		EntityPlayer player = null;
		try { 
			player = getPlayer(ics, pars[0]); 
		} catch(Exception e) {}
		
		if(player == null) { //Fallback and try the "implicit player"
			player = CommandBase.getCommandSenderAsPlayer(ics);
		} else {
			pars = Arrays.copyOfRange(pars, 1, pars.length);
		}
		
		if(player == null) {
			throw new PlayerNotFoundException();
		}
		
		Call target = calls.get(pars[0]);
		if(target != null) {
			if(!target.invoke(ics, player, Arrays.copyOfRange(pars, 1, pars.length))) {
				//display usage
				sendError(ics, "commands.aim.fail");
			} else {
				sendChat(ics, "commands.aim.successful");
			}
		} else {
			sendError(ics, "commands.aim.nokey");
		}
		
	}
	
	static interface Call {
		boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars);
		String getDesc();
	}
	
	class Help implements Call {

		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			//Display all the info
			for(Entry<String, Call> e : calls.entrySet()) {
				sendChat(ics, "commands.aim.infodisp", e.getKey(), new ChatComponentTranslation(e.getValue().getDesc()));
			}
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.help.desc";
		}
		
	}
	
	class Category implements Call {
		
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			Integer i = null;
			try {
				i = Integer.parseInt(pars[0]);
			} catch(Exception e) { 
				return false; 
			}
			if(i == null) {
				return false;
			}
			
			if(i < 0 || i >= Abilities.getCategoryCount()) {
				throw new WrongUsageException("commands.aim.overflow");
			}
			AbilityDataMain.getData(player).setCategoryID(i);
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.category.desc";
		}
		
	}
	
	class Level implements Call {
		
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			Integer i;
			try {
				i = Integer.parseInt(pars[0]);
			} catch(Exception e) { return false; }
			if(i == null) return false;
			
			AbilityData data = AbilityDataMain.getData(player);
			if(i < 0 || i >= data.getLevelCount()) {
				throw new WrongUsageException("commands.aim.overflow");
			}
			data.setLevelID(i);
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.level.desc";
		}
		
	}
	
	class God implements Call {
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			AbilityData data = AbilityDataMain.getData(player);
			if(!data.hasAbility())
				return true;
			int[] arr = data.getSkillLevelArray();
			for(int i = 0; i < arr.length; ++i) {
				arr[i] = data.getMaxSkillLevel(i);
			}
			data.setLevelID(data.getLevelCount() - 1);
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.god.desc";
		}
	}
	
	class CP implements Call {
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			AbilityData data = AbilityDataMain.getData(player);
			if(!data.hasAbility())
				return true;
			Float f = Float.valueOf(pars[0]);
			if(f == null) return false;
			data.setCurrentCP(f);
			
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.cp.desc";
		}
	}
	
	class MaxCP implements Call {
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player, String[] pars) {
			AbilityData data = AbilityDataMain.getData(player);
			if(!data.hasAbility())
				return true;
			Float f = Float.valueOf(pars[0]);
			if(f == null) return false;
			data.setMaxCP(f);
			
			return true;
		}

		@Override
		public String getDesc() {
			return "commands.aim.maxcp.desc";
		}
	}
	
	class Clear implements Call {
		@Override
		public boolean invoke(ICommandSender ics, EntityPlayer player,
				String[] pars) {
			AbilityData data = AbilityDataMain.getData(player);
			data.setCategory(Abilities.catEmpty);
			return true;
		}
		
		@Override
		public String getDesc() {
			return "commands.aim.clear.desc";
		}
	}

}
