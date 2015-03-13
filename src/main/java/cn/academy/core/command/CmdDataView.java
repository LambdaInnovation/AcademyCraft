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
package cn.academy.core.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegCommand;
import cn.liutils.template.command.LICommandBase;
import cn.liutils.util.DebugUtils;

/**
 * 显示玩家当前的能力信息（服务端）
 * @author WeathFolD
 */
public class CmdDataView extends LICommandBase {

	public CmdDataView() {}
	
    public int getRequiredPermissionLevel() {
        return 4;
    }

	@Override
	public String getCommandName() {
		return "aview";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "/aview";
	}

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer player = CommandBase.getCommandSenderAsPlayer(ics);
		if(player == null) return;
		AbilityData data = AbilityDataMain.getData(player);
		Category cat = data.getCategory();
		Level lv = data.getLevel();
		
		sendChat(ics, player.getCommandSenderName() + " Ability Data:");
		sendChat(ics, "cat: " + data.getCategoryID() + " (" + cat.getInternalName() + ")");
		sendChat(ics, "lv : " + data.getLevelID());
		sendChat(ics, "cp : " + data.getCurrentCP() + "/" + data.getMaxCP());
		if(args.length >= 1 && args[0].equalsIgnoreCase("full")) {
			sendChat(ics, "slv: " + DebugUtils.formatArray(data.getSkillLevelArray()));
			sendChat(ics, "exp: " + DebugUtils.formatArray(data.getSkillExpArray()));
		}
		String open = "open: ";
		for(int i = 0; i < data.getSkillCount(); ++i) {
			open += data.isSkillLearned(i) ? "1" : "0";
		}
		sendChat(ics, open);
	}

}
