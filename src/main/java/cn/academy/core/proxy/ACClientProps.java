/**
 * 
 */
package cn.academy.core.proxy;

import cn.liutils.util.render.LambdaFont;
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
		TEX_HUD_BAR = src("academy:textures/guis/bar.png"),
		TEX_COIN_FRONT = src("academy:textures/items/coin-front.png"),
		TEX_COIN_BACK = src("academy:textures/items/coin-back.png"),
		TEX_EFF_RAILGUN = src("academy:textures/effects/railgun.png"),
		TEX_GUI_RAILGUN = src("academy:textures/guis/railgun.png"),
		TEX_GUI_RAILGUN_PRG = src("academy:textures/guis/railgun_prog.png"),
		TEX_GUI_RAILGUN_DEC = src("academy:textures/guis/dec_railgun.png"),
		TEX_GUI_AD_MAIN = src("academy:textures/guis/ad_main.png"),
		TEX_GUI_AD_LEARNING = src("academy:textures/guis/ad_learning.png"),
		TEX_GUI_AD_SKILL = src("academy:textures/guis/ad_skill.png"),
		TEX_GUI_AD_DIAG = src("academy:textures/guis/ad_dialogue.png"),
		TEX_DBG_STD = src("academy:textures/debug/11.png"),
		TEX_DBG_31 = src("academy:textures/debug/31.png"),
		TEX_MDL_WINDGEN = src("academy:textures/models/windgen.png"),
		TEX_MDL_WINDGEN_FAN = src("academy:textures/models/windgen_fan.png"),
		TEX_MDL_SOLAR = src("academy:textures/models/solar.png"),
		TEX_ELECARC = src("academy:textures/effects/arc.png"),
		TEX_GUI_CLOSE = src("academy:textures/guis/close.png"),
		TEX_GUI_KS_MASK = src("academy:textures/guis/key_settings_mask.png"),
		TEX_QUESTION_MARK = src("academy:textures/guis/question.png"),
		TEX_LOGO_RAYS = src("academy:textures/guis/logo_rays.png"),
		TEX_LOGO_FRAME = src("academy:textures/guis/logo_frame.png"),
		TEX_LOGO_GEOM = src("academy:textures/guis/logo_geom.png"),
		TEX_LOGO_BACK = src("academy:textures/guis/logo_back.png");
	
	public static final LambdaFont FONT_YAHEI_32 = new LambdaFont(src("academy:fonts/yahei.png"), "/assets/academy/fonts/yahei.lf");
	
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
		MDL_SOLAR = AdvancedModelLoader.loadModel(src("academy:models/solar.obj")),
		MDL_GRID = AdvancedModelLoader.loadModel(src("academy:models/grid.obj"));
	
	public static final ResourceLocation
		TEX_MDL_GRID = src("academy:textures/models/grid.png"),
		TEX_MDL_GRID_BLOCK = src("academy:textures/models/grid_block.png");
	
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
	ANIM_MDBALL_ARC[] = {
		src("academy:textures/effects/mdball_arc/0.png"),
		src("academy:textures/effects/mdball_arc/1.png"),
		src("academy:textures/effects/mdball_arc/2.png"),
		src("academy:textures/effects/mdball_arc/3.png"),
	},
	ANIM_ELEC_ARC[] = {
		src("academy:textures/effects/elec_arc/0.png"),
		src("academy:textures/effects/elec_arc/1.png"),
		src("academy:textures/effects/elec_arc/2.png"),
	},
	ANIM_ELEC_ARC_STRONG[] = {
		src("academy:textures/effects/elec_arc_l/0.png"),
		src("academy:textures/effects/elec_arc_l/1.png"),
		src("academy:textures/effects/elec_arc_l/2.png"),
		src("academy:textures/effects/elec_arc_l/3.png"),
		src("academy:textures/effects/elec_arc_l/4.png"),
	};
	
	public static ResourceLocation ANIM_SMALL_ARC[];
	static {
		ANIM_SMALL_ARC = new ResourceLocation[20];
		for(int i = 1; i < 10; ++i) {
			ANIM_SMALL_ARC[i - 1] = src("academy:textures/effects/arcs/arcS_0" + i + ".png");
		}
		for(int i = 10; i <= 20; ++i) {
			ANIM_SMALL_ARC[i - 1] = src("academy:textures/effects/arcs/arcS_" + i + ".png");
		}
	}
	
	public static ResourceLocation ANIM_BLOOD_SPLASH[];
	static {
		ANIM_BLOOD_SPLASH = new ResourceLocation[10];
		for(int i = 0; i < 10; ++i) {
			ANIM_BLOOD_SPLASH[i] = src("academy:textures/effects/blood_splash/" + i + ".png");
		}
	}
	
	//Railgun
	public static final ResourceLocation
		EFF_MV_TEST = src("academy:textures/effects/mineview_test.png"),
		EFF_RAILGUN_PREP_CC = src("academy:textures/effects/railgun_prepare.png");

	private static ResourceLocation src(String s) {
		return new ResourceLocation(s);
	}

}
