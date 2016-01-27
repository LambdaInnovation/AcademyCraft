/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy;

import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.block.*;
import cn.academy.energy.block.BlockNode.NodeType;
import cn.academy.energy.block.wind.BlockWindGenBase;
import cn.academy.energy.block.wind.BlockWindGenMain;
import cn.academy.energy.block.wind.BlockWindGenPillar;
import cn.academy.energy.item.ItemMatrixCore;
import cn.academy.energy.template.ItemEnergyBase;
import cn.academy.support.EnergyItemHelper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegBlock;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import cn.lambdalib.multiblock.ItemBlockMulti;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * @author WeAthFolD
 */
@Registrant
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

    @RegInitCallback
    public static void init() {
        FluidContainerRegistry.registerFluidContainer(new FluidStack(ModuleCrafting.fluidImagProj, 1000),
                EnergyItemHelper.createFullItem(energyUnit), EnergyItemHelper.createEmptyItem(energyUnit));
    }

}
