/**
 * 
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
