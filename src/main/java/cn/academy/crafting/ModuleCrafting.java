/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.crafting;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.core.registry.InstanceEjector;
import cn.academy.core.registry.InstanceEjector.FromLoader;
import cn.academy.core.registry.LoaderHelper;
import cn.academy.crafting.block.BlockGenericOre;
import cn.academy.crafting.block.BlockImagFusor;
import cn.academy.crafting.block.BlockInciser;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegBlock;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.crafting.CustomMappingHelper.RecipeName;
import cn.liutils.loading.item.ItemLoader;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
@RegACRecipeNames
public class ModuleCrafting {
	
	public static ItemLoader items;
	
	@RegItem
	@RegItem.HasRender
	@RecipeName("matter_unit")
	public static ItemMatterUnit matterUnit;
	
	@FromLoader
	@RecipeName("crystal0")
	public static Item crystalLow;

	@RegItem
	@RecipeName("frame")
	public static Item machineFrame = new ACItem("machine_frame");
	
	@RegItem
	@RecipeName("calc_chip")
	public static Item calcChip = new ACItem("calc_chip");
	
	@FromLoader
	@RecipeName("reso_part")
	public static Item resonancePart;
	
	@RegBlock
	@RecipeName("fusor")
    public static BlockImagFusor imagFusor;
	
	@RegBlock
	@RecipeName("inciser")
	public static BlockInciser inciser;
	
	@RegBlock
	@RecipeName("cons_ore")
	public static BlockGenericOre oreConstraintMetal = new BlockGenericOre("constraint_metal_ore", 3.0f, 2);
	
	@RegBlock
	@RecipeName("crystal_ore")
	public static BlockGenericOre oreCrystal = new BlockGenericOre("crystal_ore", 2.0f, 2);
	
	public static Fluid fluidImagProj = new Fluid("imagProj");
    static {
    	fluidImagProj.setLuminosity(8).setDensity(7000)
    		.setViscosity(6000).setTemperature(0).setDensity(1);
		FluidRegistry.registerFluid(fluidImagProj);
    }
	
	public static void init() {
		FluidContainerRegistry.registerFluidContainer(new FluidStack(fluidImagProj, 1000), 
			matterUnit.create("imag_proj"), matterUnit.create("none"));
		
		items = LoaderHelper.createItemLoader();
		items.feed(new ResourceLocation("academy:items_crafting.json"));
		
		items.loadAll();
		
		InstanceEjector.fromItemLoader(ModuleCrafting.class, items);
		
		oreCrystal.setDropData(crystalLow, 1, 3);
	}
	
}
