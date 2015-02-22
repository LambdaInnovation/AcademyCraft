/**
 * 
 */
package cn.academy.core.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegCommand;
import cn.liutils.template.command.LICommandBase;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegCommand
public class CmdDataSet extends LICommandBase {

	public CmdDataSet() {
	}

	@Override
	public String getCommandName() {
		return "aset";
	}

	@Override
	public String getCommandUsage(ICommandSender var1) {
		return "/aset ([cat][level][slv][cp][maxcp][exp][openall][god]) <index> <value>, where <index> <value> are optional parameters.";
	}

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		EntityPlayer player = CommandBase.getCommandSenderAsPlayer(ics);
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
				
			} else if(args[0].equalsIgnoreCase("skilllevel")){
				int ind = Integer.parseInt(args[1]);
				Category cat = data.getCategory();
				if(ind >= 0 && ind < cat.getSkillCount()) {
					int i = Integer.parseInt(args[2]);
					data.setSkillLevel(ind, i);
				} else sendError(ics, "Invalid skill id.");
				
			} else {
				sendError(ics, "Array key doesnt exist.");
			}
			
		} else if(args.length == 1) { 
			if(args[0].equalsIgnoreCase("god")) {
				//Enter god mode
				for(int i = 0; i < data.getSkillCount(); ++i) {
					data.setSkillLevel(i, data.getMaxSkillLevel(i));
				}
				sendChat(ics, "Entered god mode");
			} else if(args[0].equalsIgnoreCase("sexp")) { //give player some experience.
				player.addExperienceLevel(30);
				player.experience = 0;
			} else if(args[0].equalsIgnoreCase("openall")) {
				for(Integer i : data.getCanLearnSkillList()) {
					data.setSkillLevel(i, Math.max(1, data.getSkillLevel(i)));
				}
				sendChat(ics, "learned all available skills");
			}
		} else {
			LICommandBase.sendError(ics, "Invalid argument size");
			LICommandBase.sendChat(ics, getCommandUsage(ics));
		}
	}

}
