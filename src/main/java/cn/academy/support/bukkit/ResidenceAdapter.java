package cn.academy.support.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import cn.academy.core.AcademyCraft;
import cn.academy.core.event.BlockDestroyEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.SERVER)
public final class ResidenceAdapter {
	
	public static final ResidenceAdapter INSTANCE = new ResidenceAdapter();
	
	public static void init() {
		MinecraftForge.EVENT_BUS.register(INSTANCE);
	}
	
	@SubscribeEvent
	public void onBlockDestroy(BlockDestroyEvent event) {
		World world = Bukkit.getWorld(event.world.getWorldInfo().getWorldName());
		FlagPermissions perms = Residence.getPermsByLoc(new Location(world, event.x, event.y, event.z));
		if (!perms.has("destroy", perms.has("build", true)))
			event.setCanceled(true);
	}
}
