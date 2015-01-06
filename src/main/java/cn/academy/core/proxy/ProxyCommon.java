package cn.academy.core.proxy;

import net.minecraft.command.CommandHandler;
import net.minecraft.entity.Entity;
import cn.academy.ability.electro.CatElectro;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.ability.meltdowner.CatMeltDowner;
import cn.academy.ability.meltdowner.entity.EntityElecDart;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ctrl.EventHandlerServer;
import cn.academy.core.AcademyCraftMod;
import cn.academy.core.block.dev.MsgActionStart;
import cn.academy.core.block.dev.MsgDeveloper;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;

public class ProxyCommon {
	
	public void preInit() {}
	
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading common proxy of Academy Craft.");
		
		//----------
		//Abilities registry
		
		//Abilities.registerCat(new CatElectro());
		//Abilities.registerCat(new CatMeltDowner());
		//Use @RegAbility to register a category.
		
		//----------
	}
	
	public void postInit() {}
	
	public void commandInit(CommandHandler cm) {}

}
