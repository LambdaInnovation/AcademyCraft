package cn.academy.api.player;

import java.util.HashMap;
import java.util.List;

import cn.academy.core.AcademyCraftMod;
import cn.liutils.util.ClientUtils;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

public class ControlManager {
	
	private static ControlManager INSTANCE = null;
	
	private ControlManager() {	
	}
	
	private HashMap<String, ControlData> map = new HashMap<String, ControlData>();
	
	public static void init() {
		if (INSTANCE == null)
			INSTANCE = new ControlManager();
		MinecraftForge.EVENT_BUS.register(INSTANCE);
		AcademyCraftMod.netHandler.registerMessage(MsgControlSync.Handler.class, MsgControlSync.class, AcademyCraftMod.getNextChannelID(), Side.CLIENT);
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if (event.phase == Phase.START) {
			if (ClientUtils.isPlayerInGame())
				onTick(Minecraft.getMinecraft().thePlayer, event.side);
		}
	}
	
	private int ticker = 0;
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.phase == Phase.START) {
			List players = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
			for (int i = 0; i < players.size(); ++i)
				onTick((EntityPlayerMP) players.get(i), event.side);
			if (ticker-- == 0)
				ticker = 600;
		}
	}
	
	public void onTick(EntityPlayer player, Side side) {
		ControlData data = ControlData.get(player);
		if (data == null) {
			AcademyCraftMod.log.error("No ControlData for " + player.getDisplayName());
			return;
		}
		data.onTick(player, side, true);//ticker == 0);
	}
	
	@SubscribeEvent
	public void onEntityConstructing(EntityConstructing event) {
		if (event.entity instanceof EntityPlayer) {
			if (ControlData.get(event.entity) == null)
				event.entity.registerExtendedProperties(ControlData.IDENTIFIER, new ControlData());
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		ControlData data = ControlData.get(event.player);
		if (data == null)
			((EntityPlayerMP) event.player).playerNetServerHandler.kickPlayerFromServer("INVALID SITUATION");
	}
}
