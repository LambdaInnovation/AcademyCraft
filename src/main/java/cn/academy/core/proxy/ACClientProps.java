/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.proxy;

import net.minecraft.util.ResourceLocation;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.ForcePreloadTexture;
import cn.liutils.util.render.LambdaFont;

/**
 * Generic client information such as models and textures.
 * @author WeathFolD
 */
@RegistrationClass
@ForcePreloadTexture
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
		//TEX_MDL_WINDGEN = src("academy:textures/models/windgen.png"),
		//TEX_MDL_WINDGEN_FAN = src("academy:textures/models/windgen_fan.png"),
		TEX_MDL_SOLAR = src("academy:textures/models/solar.png"),
		TEX_ELECARC = src("academy:textures/effects/arc.png"),
		TEX_GUI_CLOSE = src("academy:textures/guis/close.png"),
		TEX_GUI_KS_MASK = src("academy:textures/guis/key_settings_mask.png"),
		TEX_QUESTION_MARK = src("academy:textures/guis/question.png"),
		TEX_LOGO_RAYS = src("academy:textures/guis/logo_rays.png"),
		TEX_LOGO_FRAME = src("academy:textures/guis/logo_frame.png"),
		TEX_LOGO_GEOM = src("academy:textures/guis/logo_geom.png"),
		TEX_LOGO_BACK = src("academy:textures/guis/logo_back.png"),
		TEX_GUI_PRESET = src("academy:textures/guis/preset.png"),
		TEX_EFF_MD_SHIELD = src("academy:textures/effects/mdshield.png");
	
	public static LambdaFont FONT_YAHEI_32 = new LambdaFont(src("academy:fonts/yahei.png"), "/assets/academy/fonts/yahei.lf");;
	
	//Model Textures
	public static final ResourceLocation
		TEX_MDL_GRID = src("academy:textures/models/grid.png"),
		TEX_MDL_GRID_BLOCK = src("academy:textures/models/grid_block.png"),
		TEX_MDL_MAGHOOK = src("academy:textures/models/maghook.png"),
		TEX_MDL_SILBARN = src("academy:textures/models/silbarn.png");
	
	//Animations
	public static final ResourceLocation
		ANIM_ELEC_ARC[] = eff("elec_arc", 5),
		ANIM_ELEC_ARC_STRONG[] = eff("elec_arc_l", 5),
		ANIM_ARC_W[] = eff("arcw", 6),
		ANIM_TP_MARK[] = eff("tp_mark", 8),
		ANIM_MD_RAY_S[] = eff("mdray_s", 5),
		ANIM_MD_RAY_SA[] = eff("mdray_sa", 5),
		ANIM_MD_RAY_SF[] = eff("mdray_sf", 5),
		ANIM_BLOOD_SPLASH[] = eff("blood_splash", 10),
		ANIM_MDBALL[] = eff("mdball", 5),
		ANIM_MD_RAY_L[] = eff("mdray_l", 5);
	
	public static ResourceLocation ANIM_SMALL_ARC[] = eff("arcs", 20);
	
	//Railgun
	public static final ResourceLocation
		EFF_MV = src("academy:textures/effects/mineview.png"),
		EFF_RAILGUN_PREP_CC = src("academy:textures/effects/railgun_prepare.png");

	private static ResourceLocation src(String s) {
		return new ResourceLocation(s);
	}
	
	private static ResourceLocation[] eff(String s, int n) {
		ResourceLocation[] ret = new ResourceLocation[n];
		for(int i = 0; i < n; ++i) {
			ret[i] = src("academy:textures/effects/" + s + "/" + i + ".png");
		}
		return ret;
	}

}
