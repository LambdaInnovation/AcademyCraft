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
import cn.academy.ability.api.Category;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.ability.api.data.PresetData.Preset;
import cn.academy.ability.api.data.PresetData.PresetEditor;
import cn.academy.core.command.ACCommand;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;

/**
 * @author WeAthFolD
 */
@Registrant
@RegCommand
public class CommandPreset extends ACCommand {
	
	/*
	 * > view <id>
	 * 0: 1(xxxx)
	 * 1: 3(xxxx)
	 * 2: 4(xxxx)
	 * ...
	 */
	
	/*
	 * > edit 0 0,1,2,3
	 */
	
	/*
	 * > list
	 * Available controllables:
	 * 	0: xxxx
	 *  1: xxxx
	 */
	
	String[] cmds = { "view", "list", "edit" };

	public CommandPreset() {
		super("preset");
	}

	@Override
	public void processCommand(ICommandSender ics, String[] pars) {
		EntityPlayer player = this.getCommandSenderAsPlayer(ics);
		
		if(pars.length == 0) {
			sendChat(ics, getLoc("usage"));
			return;
		}
		
		AbilityData aData = AbilityData.get(player);
		PresetData pData = PresetData.get(player);
		
		Category c = aData.getCategory();
		if(!aData.isLearned()) {
			sendChat(ics, locNotLearned());
			return;
		}
		
		List<Controllable> list = c.getControllableList();
		
		switch(pars[0]) {
		case "help":
		{
			for(String s : cmds) {
				sendChat(ics, getLoc(s));
			}
			break;
		}
		case "view":
		{
			try {
				if(pars.length == 2) {
					int id = Integer.valueOf(pars[1]);
					if(id < 0 || id >= PresetData.MAX_PRESETS) {
						sendChat(ics, locInvalid());
					} else {
						Preset p = pData.getPreset(id);
						String[] strs = p.formatDetail().split("\n");
						if(strs[0].trim().equals("")) {
							sendChat(ics, getLoc("nobind"));
						} else {
							for(String s : strs) {
								sendChat(ics, s);
							}
						}
					}
				} else {
					sendChat(ics, locInvalid());
				}
			} catch(NumberFormatException e) {
				sendChat(ics, locInvalid());
			}
			break;
		}
		case "list":
		{
			sendChat(ics, getLoc("list2"));
			for(int i = 0; i < list.size(); ++i) {
				Controllable cc = list.get(i);
				String str = i + ": " + cc.toString();
				sendChat(ics, str);
			}
 			break;
		}
		case "edit":
		{
			try {
				int id = Integer.valueOf(pars[1]);
				String[] ss = pars[2].split(",");
				PresetEditor editor = pData.createEditor(id);
				for(int i = 0; i < ss.length && i < PresetData.MAX_KEYS; ++i) {
					int cid = Integer.valueOf(ss[i]);
					if(cid != -1)
						list.get(cid); //Validity check, if invalid input will throw a exception
					editor.edit(i, cid);
				}
				editor.save();
				sendChat(ics, locSuccessful());
			} catch(Exception e) {
				sendChat(ics, locInvalid());
			}
			break;
		}
		}
	}

}
