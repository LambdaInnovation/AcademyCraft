package cn.academy.command;

import cn.academy.advancements.ACAdvancements;
import cn.academy.util.ACCommand;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.PlayerUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * @author EAirPeter
 */
public class CommandACACH extends ACCommand {

    @StateEventCallback
    private static void serverInit(FMLServerStartingEvent ev) {
        ev.registerServerCommand(new CommandACACH());
    }

    public CommandACACH() {
        super("acach");
    }
    
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        // /acach ACHIEVEMENT_NAME [PLAYER_NAME]
        String nAch = null;
        String nPlayer = null;
        if (args.length > 0)
            nAch = args[0];
        if (nAch == null) {
            PlayerUtils.sendChat(sender, "Usage: /acach ACHIEVEMENT_NAME [PLAYER_NAME]");
            return;
        }
        if (args.length > 1)
            nPlayer = args[1];
        if (nPlayer == null)
            nPlayer = sender.getName();
        EntityPlayer player = server.getPlayerList().getPlayerByUsername(nPlayer);
        if (player == null) {
            PlayerUtils.sendChat(sender, locNoPlayer());
            return;
        }
        if (ACAdvancements.trigger(player, nAch))
            PlayerUtils.sendChat(sender, locSuccessful());
        else
            PlayerUtils.sendChat(sender, "No such achievement found");
    }
}