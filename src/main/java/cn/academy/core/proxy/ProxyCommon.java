package cn.academy.core.proxy;

import net.minecraft.command.CommandHandler;
import net.minecraft.entity.Entity;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.core.AcademyCraftMod;
import cpw.mods.fml.common.registry.EntityRegistry;

public class ProxyCommon {
	
	public void preInit() {}
	
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading common proxy of Academy Craft.");
		EventHandlerServer.init();
		
		//Entity Registry
		registerEntity(EntityRailgun.class, "ac_railgun", nextEntityId());
		
		//----------
		//Abilities registry
		
		Abilities.registerCat(new CatElectro());
		
		//----------
	}
	
	public void postInit() {}
	
	public void commandInit(CommandHandler cm) {}
	
	int nextEntityId = 0;
	private int nextEntityId() {
		return nextEntityId++;
	}
	
	private void registerEntity(Class<? extends Entity> cl, String name, int id) {
		registerEntity(cl, name, id, 32, 3, true);
	}
	
	private void registerEntity(Class<? extends Entity> cl, String name, int id, int trackRange, int freq, boolean updateVel) {
		EntityRegistry.registerModEntity(cl, name, id, AcademyCraftMod.INSTANCE, trackRange, freq, updateVel);
	}
	
}
