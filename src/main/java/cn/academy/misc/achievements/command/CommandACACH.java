package cn.academy.misc.achievements.command;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import cn.academy.core.command.ACCommand;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegCommand;

/**
 * @author EAirPeter
 */
@Registrant
@RegCommand
public class CommandACACH extends ACCommand {

	public CommandACACH() {
		super("acach");
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		// /acach ACHIEVEMENT_NAME [PLAYER_NAME]
		String nAch = null;
		String nPlayer = null;
		if (args.length > 0)
			nAch = args[0];
		if (nAch == null) {
			sendChat(sender, "Usage: /acach ACHIEVEMENT_NAME [PLAYER_NAME]");
			return;
		}
		if (args.length > 1)
			nPlayer = args[1];
		if (nPlayer == null)
			nPlayer = sender.getCommandSenderName();
		EntityPlayer player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(nPlayer);
		if (player == null) {
			sendChat(sender, locNoPlayer());
			return;
		}
		if (ModuleAchievements.trigger(player, nAch))
			sendChat(sender, locSuccessful());
		else
			sendChat(sender, "No such achievement found");
	}

}
