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
import cn.academy.core.block.MsgDeveloper;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;

public class ProxyCommon {
	
	public void preInit() {}
	
	public void init() {
		AcademyCraftMod.INSTANCE.log.info("Loading common proxy of Academy Craft.");

		AcademyCraftMod.netHandler.registerMessage(MsgDeveloper.Handler.class, 
				MsgDeveloper.class, AcademyCraftMod.getNextChannelID(), Side.CLIENT);
		
		//----------
		//Abilities registry
		
		Abilities.registerCat(new CatElectro());
		Abilities.registerCat(new CatMeltDowner());
		
		//----------
	}
	
	public void postInit() {}
	
	public void commandInit(CommandHandler cm) {}

}
