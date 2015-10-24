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
package cn.academy.energy;

import net.minecraft.item.Item;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.crafting.block.BlockImagPhase;
import cn.academy.energy.block.BlockCatEngine;
import cn.academy.energy.block.BlockMatrix;
import cn.academy.energy.block.BlockNode;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.academy.energy.block.BlockPhaseGen;
import cn.academy.energy.block.BlockSolarGen;
import cn.academy.energy.block.wind.BlockWindGenBase;
import cn.academy.energy.block.wind.BlockWindGenMain;
import cn.academy.energy.block.wind.BlockWindGenPillar;
import cn.academy.energy.item.ItemMatrixCore;
import cn.academy.energy.template.ItemEnergyBase;
import cn.academy.support.EnergyItemHelper;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegBlock;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;
import cn.liutils.crafting.CustomMappingHelper.RecipeName;
import cn.liutils.template.block.ItemBlockMulti;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
@RegACRecipeNames
public class ModuleEnergy {
	
	// BLOCKS
    @RegBlock
    @RecipeName("node0")
    public static BlockNode nodeBasic = new BlockNode(NodeType.BASIC);
    
    @RegBlock
    @RecipeName("node1")
    public static BlockNode nodeStandard = new BlockNode(NodeType.STANDARD);
    
    @RegBlock
    @RecipeName("node2")
    public static BlockNode nodeAdvanced = new BlockNode(NodeType.ADVANCED);
    
    @RegBlock(item = ItemBlockMulti.class)
    @RecipeName("mat")
    public static BlockMatrix matrix = new BlockMatrix();
    
    @RegBlock
    public static BlockCatEngine infiniteGen;
    
    @RegBlock
    @RecipeName("solar_gen")
    public static BlockSolarGen solarGen;
    
    @RegBlock
    @RecipeName("phase_gen")
    public static BlockPhaseGen phaseGen;
    
    @RegBlock(item = ItemBlockMulti.class)
    @RecipeName("windgen_base")
    public static BlockWindGenBase windgenBase;
    
    @RegBlock
    @RecipeName("windgen_pillar")
    public static BlockWindGenPillar windgenPillar;
    
    @RegBlock(item = ItemBlockMulti.class)
    @RecipeName("windgen_main")
    public static BlockWindGenMain windgenMain;
    
    // ITEMS
    @RegItem
    @RecipeName("ene_unit")
    public static Item energyUnit = new ItemEnergyBase("energy_unit", 10000, 20);
    
    @RegItem
    @RecipeName("mat_core")
    public static ItemMatrixCore matrixCore;
    
    @RegItem
    @RecipeName("windgen_fan")
    public static Item windgenFan = new ACItem("windgen_fan").setMaxDamage(100).setMaxStackSize(1);
    
    public static void init() {
		FluidContainerRegistry.registerFluidContainer(new FluidStack(ModuleCrafting.fluidImagProj, 1000), 
			EnergyItemHelper.createFullItem(energyUnit), EnergyItemHelper.createEmptyItem(energyUnit));
	}
	
}
