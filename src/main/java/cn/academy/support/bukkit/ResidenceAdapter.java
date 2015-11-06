package cn.academy.support.bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

import cn.academy.core.AcademyCraft;
import cn.academy.core.event.BlockDestroyEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public final class ResidenceAdapter {
	
	public static final ResidenceAdapter INSTANCE = new ResidenceAdapter();
	
	//public static void setWorld(String _world)
	private static Method _setWorld;
	
	//public static void setPlayer(UUID _player)
	private static Method _setPlayer;
	
	//public static boolean checkBlockDestroy(int x, int y, int z)
	private static Method _checkBlockDestroy;
	
	public static void init(Class adapter) {
		try {
			_setWorld = adapter.getMethod("setWorld", String.class);
			_setPlayer = adapter.getMethod("setPlayer", UUID.class);
			_checkBlockDestroy = adapter.getMethod("checkBlockDestroy", int.class, int.class, int.class);
			MinecraftForge.EVENT_BUS.register(INSTANCE);
		}
		catch (Throwable e) {
			AcademyCraft.log.error("Failed to load ResidenceAdapter", e);
		}
	}
	
	public static final UUID dummy = UUID.randomUUID();
	
	private World world = null;
	private EntityPlayer player = null;
	
	@SubscribeEvent
	public void onBlockDestroy(BlockDestroyEvent event) {
		try {
			if (world != event.world) {
				world = event.world;
				_setWorld.invoke(null, world.getWorldInfo().getWorldName());
			}
			if (player != event.player) {
				player = event.player;
				_setPlayer.invoke(null, player == null ? dummy : event.player.getUniqueID());
			}
			event.setCanceled((boolean) _checkBlockDestroy.invoke(null, event.x, event.y, event.z));
		}
		catch (Throwable e) {
			AcademyCraft.log.error("Failed to handle BlockDestroyEvent", e);
		}
	}
}
