/**
 * 
 */
package cn.academy.core.proxy;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;

/**
 * Generic client information such as models and textures.
 * @author WeathFolD
 */
public class ACClientProps {

	//Textures
	public static final ResourceLocation
		TEX_MDL_DEVELOPER = src("academy:textures/models/ability_developer.png"),
		TEX_MDL_ELEC_CARD = src("academy:textures/models/card.png"),
		TEX_MDL_MAGNET_MODULE = src("academy:textures/models/magincr.png"),
		TEX_HUD_CPBAR = src("academy:textures/guis/cpbar.png"),
		TEX_HUD_LOGO = src("academy:textures/guis/logo.png"),
		TEX_COIN_FRONT = src("academy:textures/items/coin-front.png"),
		TEX_COIN_BACK = src("academy:textures/items/coin-back.png"),
		TEX_EFF_RAILGUN = src("academy:textures/effects/railgun.png"),
		TEX_GUI_RAILGUN = src("academy:textures/guis/railgun.png"),
		TEX_GUI_RAILGUN_PRG = src("academy:textures/guis/railgun_prog.png"),
		TEX_GUI_RAILGUN_DEC = src("academy:textures/guis/dec_railgun.png"),
		TEX_GUI_LOGO_DMASK = src("academy:textures/guis/logo_depth_mask.png"),
		TEX_GUI_AD_MAIN = src("academy:textures/guis/ad_main.png"),
		TEX_GUI_AD_LEARNING = src("academy:textures/guis/ad_learning.png"),
		TEX_GUI_AD_SKILL = src("academy:textures/guis/ad_skill.png"),
		TEX_DBG_STD = src("academy:textures/debug/11.png"),
		TEX_DBG_31 = src("academy:textures/debug/31.png"),
		TEX_MDL_WINDGEN = src("academy:textures/models/windgen.png"),
		TEX_MDL_WINDGEN_FAN = src("academy:textures/models/windgen_fan.png"),
		TEX_MDL_SOLAR = src("academy:textures/models/solar.png"),
		TEX_ELECARC = src("academy:textures/effects/arc.png"),
		TEX_GUI_CLOSE = src("academy:textures/guis/close.png"),
		TEX_GUI_KS_MASK = src("academy:textures/guis/key_settings_mask.png");
	
	//Ability Textures
	public static final ResourceLocation
		SKL_TEST_1 = src("academy:textures/abilities/test/skill1.png"),
		SKL_TEST_2 = src("academy:textures/abilities/test/skill2.png");
		
	//OBJ models
	public static final IModelCustom 
		MDL_ABILITY_DEVELOPER = AdvancedModelLoader.loadModel(src("academy:models/ability_developer.obj")),
		MDL_ELEC_CARD = AdvancedModelLoader.loadModel(src("academy:models/card.obj")),
		MDL_MAGNET_MODULE = AdvancedModelLoader.loadModel(src("academy:models/magincr.obj")),
		MDL_WINDGEN = AdvancedModelLoader.loadModel(src("academy:models/windgen.obj")),
		MDL_SOLAR = AdvancedModelLoader.loadModel(src("academy:models/solar.obj"));
	
	//Animations
	public static final ResourceLocation
		ANIM_ARC_LONG[] = {
		src("academy:textures/effects/elearc0.png"),
		src("academy:textures/effects/elearc1.png"),
		src("academy:textures/effects/elearc2.png")
	}, 
	TEX_ARC_SHELL[] = { 
		src("academy:textures/effects/arcshell0.png") ,
		src("academy:textures/effects/arcshell1.png") ,
		src("academy:textures/effects/arcshell2.png") 
	},
	TEX_MD_SHELL[] = {
		src("academy:textures/effects/mdshell0.png") ,
		src("academy:textures/effects/mdshell1.png") ,
		src("academy:textures/effects/mdshell2.png") 
	},
	TEX_ELEC_SMALL[] = {
		src("academy:textures/effects/eles0.png"),
		src("academy:textures/effects/eles1.png"),
		src("academy:textures/effects/eles2.png"),
		src("academy:textures/effects/eles3.png"),
		src("academy:textures/effects/eles4.png"),
		src("academy:textures/effects/eles5.png"),
	},
	ANIM_MDBALL_STB[] = {
		src("academy:textures/effects/mdball/0.png"),
		src("academy:textures/effects/mdball/1.png"),
		src("academy:textures/effects/mdball/2.png"),
		src("academy:textures/effects/mdball/3.png"),
		src("academy:textures/effects/mdball/4.png"),
	},
	ANIM_MDBALL_ARC[] = {
		src("academy:textures/effects/mdball_arc/0.png"),
		src("academy:textures/effects/mdball_arc/1.png"),
		src("academy:textures/effects/mdball_arc/2.png"),
		src("academy:textures/effects/mdball_arc/3.png"),
	}
	;
	
	//Railgun
	public static final ResourceLocation
		ELEC_LOGO = src("academy:textures/abilities/electromaster/main.png"),
		ELEC_ARC = src("academy:textures/abilities/electromaster/attack_large.png"),
		ELEC_ATTRACT = src("academy:textures/abilities/electromaster/attraction.png"),
		ELEC_CHARGE = src("academy:textures/abilities/electromaster/itemcharge.png"),
		ELEC_VIEWMINE = src("academy:textures/abilities/electromaster/mineview.png"),
		ELEC_MOVE = src("academy:textures/abilities/electromaster/moving.png"),
		ELEC_RAILGUN = src("academy:textures/abilities/electromaster/railgun.png"),
		ELEC_SWORD = src("academy:textures/abilities/electromaster/sword.png"),
		ELEC_ARC_STRONG = src("academy:textures/abilities/electromaster/arc.png");
	
	//Meltdowner
	public static final ResourceLocation 
		MD_LOGO = src("academy:textures/abilities/meltdowner/main.png"),
		MD_GENERATE = src("academy:textures/abilities/meltdowner/generate.png"),
		MD_SINGLE = src("academy:textures/abilities/meltdowner/single.png"),
		MD_MULTIPLE = src("academy:textures/abilities/meltdowner/multiple.png"),
		MD_SHELL = src("academy:textures/abilities/meltdowner/shell.png"),
		MD_MINING = src("academy:textures/abilities/meltdowner/mining.png"),
		MD_EXPLOSION = src("academy:textures/abilities/meltdowner/explosion.png");
	
	//GUI IDs
	public static final int
		GUI_ID_ABILITY_DEV = 0,
		GUI_ID_PRESET_SETTINGS = 1;
	
	private static ResourceLocation src(String s) {
		return new ResourceLocation(s);
	}

}
