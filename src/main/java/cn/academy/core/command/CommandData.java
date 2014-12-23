/**
 * 
 */
package cn.academy.core.command;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.command.LICommandBase;

/**
 * @author WeathFolD
 *
 */
public class CommandData extends LICommandBase {

	public CommandData() {
	}

	@Override
	public String getCommandName() {
		return "adata";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "/adata [cat][level][cp][maxcp][exp][open] <index> [value]";
	}

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer player = this.getCommandSenderAsPlayer(ics);
		if(player == null) return;
		
		AbilityData data = AbilityDataMain.getData(player);
		if(args.length == 2) { //Normal value
			
			if(args[0].equalsIgnoreCase("cat")) {
				int val = Integer.parseInt(args[1]);
				Category cat = Abilities.getCategory(val);
				if(cat != null) {
					data.setCategoryID(val);
					sendChat(ics, "Set player cat to: " + cat.getDisplayName());
				} else sendError(ics, "Invalid category id");
				
			} else if(args[0].equalsIgnoreCase("level")) {
				int val = Integer.parseInt(args[1]);
				Level lv = data.getCategory().getLevel(val);
				if(lv != null) {
					data.setLevelID(val);
					sendChat(ics, "Changed to level " + lv.getDisplayName());
				} else sendError(ics, "Invalid level id");
				
			} else if(args[0].equalsIgnoreCase("cp")) {
				float val = Float.parseFloat(args[1]);
				if(val > 0) {
					val = Math.min(val, data.getMaxCP());
					data.setCurrentCP(val);
				} else sendError(ics, "CP must be larger than zero");
				
			} else if(args[0].equalsIgnoreCase("maxcp")) {
				float val = Float.parseFloat(args[1]);
				if(val > 0) {
					data.setMaxCP(val);
				} else sendError(ics, "Invalid maxcp");
				
			} else {
				sendError(ics, "Key doesnt exist.");
			}
		} else if(args.length == 3) { //Array
			
			if(args[0].equalsIgnoreCase("exp")) {
				int ind = Integer.parseInt(args[1]);
				Category cat = data.getCategory();
				if(ind >= 0 && ind < cat.getSkillCount()) {
					float f = Float.parseFloat(args[2]);
					if(f > 0F) {
						data.setSkillExp(ind, f);
					} else sendError(ics, "Invalid exp");
				} else sendError(ics, "Invalid skill id.");
				
			} else if(args[0].equalsIgnoreCase("open")){
				int ind = Integer.parseInt(args[1]);
				Category cat = data.getCategory();
				if(ind >= 0 && ind < cat.getSkillCount()) {
					int i = Integer.parseInt(args[2]);
					data.setSkillOpen(ind, i != 0);
				} else sendError(ics, "Invalid skill id.");
				
			} else {
				sendError(ics, "Array key doesnt exist.");
			}
			
		} else {
			this.sendError(ics, "Invalid argument size");
		}
	}

}
