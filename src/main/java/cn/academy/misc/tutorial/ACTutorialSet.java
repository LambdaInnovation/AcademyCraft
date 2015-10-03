package cn.academy.misc.tutorial;

import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.misc.tutorial.ACTutorialUtils.LoadOn;
import cn.academy.misc.tutorial.ACTutorialUtils.LoadOn.Type;
import cn.academy.misc.tutorial.ACTutorialUtils.RegTutorialSet;
import cn.annoreg.core.Registrant;
@Registrant
@RegTutorialSet
public class ACTutorialSet {
	@LoadOn(condition=Type.CRAFT,itemClass=ItemMatterUnit.class)
	public static ACTutorial phase_liquid,
	constraint_metal,
	crystal,
	imag_silicon,
	node,
	matrix,
	//WiFi,
	phase_generator,
	solar_gen,
	wind_gen,
	metal_former,
	imag_fusor,
	energy_bridge_eu,
	energy_bridge_rf,
	terminal,
	ability_developer,
	ability,
	ability_electromaster,
	ability_teleporter,
	ability_meltdowner;
}
