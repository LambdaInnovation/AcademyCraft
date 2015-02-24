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
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Used another class because loadModel shouldn't be called in server side.
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class ACModels {
	
	public static final IModelCustom 
		MDL_ABILITY_DEVELOPER = AdvancedModelLoader.loadModel(src("academy:models/ability_developer.obj")),
		MDL_ELEC_CARD = AdvancedModelLoader.loadModel(src("academy:models/card.obj")),
		MDL_MAGNET_MODULE = AdvancedModelLoader.loadModel(src("academy:models/magincr.obj")),
		MDL_WINDGEN = AdvancedModelLoader.loadModel(src("academy:models/windgen.obj")),
		MDL_SOLAR = AdvancedModelLoader.loadModel(src("academy:models/solar.obj")),
		MDL_GRID = AdvancedModelLoader.loadModel(src("academy:models/grid.obj")),
		MDL_MAGHOOK = AdvancedModelLoader.loadModel(src("academy:models/maghook.obj")),
		MDL_MAGHOOK_OPEN = AdvancedModelLoader.loadModel(src("academy:models/maghook_open.obj")),
		MDL_SILBARN = AdvancedModelLoader.loadModel(src("academy:models/silbarn.obj"));
	
	private static ResourceLocation src(String s) {
		return new ResourceLocation(s);
	}

}
