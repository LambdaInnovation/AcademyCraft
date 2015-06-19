package cn.academy.ability.api.ctrl.test;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SyncAction;
import cn.academy.core.command.ACCommand;
import cn.academy.core.registry.RegACKeyHandler;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegCommand;
import cn.liutils.util.helper.KeyHandler;

/*
 * TODO CLIENT:
 * listen disconnect from server
 */
/*
 * TODO SERVER:
 * listen disconnect from client
 */

@Registrant
public class TM {
	
	public static SyncAction server = null;
	public static SyncAction client = null;
	
	@RegACKeyHandler(name = "sendai", defaultKey = Keyboard.KEY_U)
	public static KeyHandler start = new KeyHandler() {
		public void onKeyUp() {
			if (client != null)
				return;
			client = new AT1();
			ActionManager.startAction(client);
		}
	};
	
	@RegACKeyHandler(name = "jintuu", defaultKey = Keyboard.KEY_I)
	public static KeyHandler end = new KeyHandler() {
		public void onKeyUp() {
			if (client == null)
				return;
			ActionManager.endAction(client);
			client = null;
		}
	};
	
	@RegACKeyHandler(name = "naka", defaultKey = Keyboard.KEY_O)
	public static KeyHandler abort = new KeyHandler() {
		public void onKeyUp() {
			if (client == null)
				return;
			ActionManager.abortAction(client);;
			client = null;
		}
	};
	
	@RegCommand
	public static class Start extends ACCommand {
		public Start() {
			super("tst");
		}
		
		public void processCommand(ICommandSender sender, String[] args) {
			if (server != null)
				return;
			int intv = 20;
			if (args.length > 0)
				intv = Integer.valueOf(args[0]);
			server = new AT1();
			ActionManager.startAction(server);
		}
	}
	
	@RegCommand
	public static class End extends ACCommand {
		public End() {
			super("ted");
		}
		
		public void processCommand(ICommandSender sender, String[] args) {
			if (server == null)
				return;
			ActionManager.endAction(server);
			server = null;
		}
	}
	
	@RegCommand
	public static class Abort extends ACCommand {
		public Abort() {
			super("tab");
		}
		
		public void processCommand(ICommandSender sender, String[] args) {
			if (server == null)
				return;
			ActionManager.abortAction(server);
			server = null;
		}
	}
	
	public static void msg(String mod, String msg) {
		String side = FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? "S" : "C";
		Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(side + "[" + mod + "] " + msg));
	}
	
	public static void log(String mod, String msg) {
		String side = FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? "S" : "C";
		System.out.println(side + "[" + mod + "] " + msg);
	}
	
}
