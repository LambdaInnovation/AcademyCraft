package cn.academy.support.bukkit;

import org.bukkit.Bukkit;

import cn.academy.core.AcademyCraft;

public final class BukkitAdapter {

	public static void init() {
		setupPlugin("Residence", ResidenceAdapter.class);
	}
	
	private static void setupPlugin(String plugin, Class adapter) {
		if (Bukkit.getPluginManager().isPluginEnabled(plugin))
			try {
				AcademyCraft.log.info("Found Bukkit plugin \'" + plugin + "\', hooking...");
				adapter.getMethod("init").invoke(null);
				AcademyCraft.log.info("Successfully hooked into Bukkit plugin \'" + plugin + "\'");
			}
			catch (Exception e) {
				AcademyCraft.log.error("Failed to hook into Bukkit plugin \'" + plugin + "\'");
			}
	}
	
}
