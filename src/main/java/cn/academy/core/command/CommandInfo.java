/**
 * 
 */
package cn.academy.core.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.command.LICommandBase;
import cn.liutils.api.util.DebugUtils;

/**
 * 显示玩家当期的能力信息（服务端）
 * @author WeathFolD
 */
public class CommandInfo extends LICommandBase {

	public CommandInfo() {
	}

	@Override
	public String getCommandName() {
		return "ainfo";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "/ainfo <full>";
	}

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer player = this.getCommandSenderAsPlayer(ics);
		if(player == null) return;
		AbilityData data = AbilityDataMain.getData(player);
		StringBuilder sb = new StringBuilder(player.getCommandSenderName()).append("Ability Data\n");
		Category cat = data.getCategory();
		Level lv = data.getLevel();
		sb.append("cat: ").append(data.getCategoryID()).append(" (").append(cat.getInternalName()).append(")\n");
		sb.append("lv : ").append(data.getLevelID()).append("\n");
		sb.append("cp : ").append(data.getCurrentCP()).append("/").append(data.getMaxCP());
		
		if(args.length == 0 || !args[0].equalsIgnoreCase("full")) {
			//Nope
		} else {
			sb.append("\n");
			sb.append("opn: ").append(DebugUtils.formatArray(data.getSkillOpenArray())).append("\n");
			sb.append("exp: ").append(DebugUtils.formatArray(data.getSkillExpArray()));
		}
		this.sendChat(ics, sb.toString());
	}

}
