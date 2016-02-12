/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.crafting;

import cn.academy.core.AcademyCraft;
import cn.academy.core.block.ACBlock;
import cn.academy.core.item.ACItem;
import cn.academy.core.registry.ACRecipeNamesRegistration.RegACRecipeNames;
import cn.academy.crafting.api.ImagFusorRecipes;
import cn.academy.crafting.api.MetalFormerRecipes;
import cn.academy.crafting.block.BlockGenericOre;
import cn.academy.crafting.block.BlockImagFusor;
import cn.academy.crafting.block.BlockImagPhase;
import cn.academy.crafting.block.BlockMetalFormer;
import cn.academy.crafting.block.TileMetalFormer.Mode;
import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.crafting.world.ACWorldGen;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.*;
import cn.lambdalib.annoreg.mc.RegEventHandler.Bus;
import cn.lambdalib.crafting.CustomMappingHelper.RecipeName;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * This is the main registry of all the crafting materials. Oredict name and
 * Recipe script names are all provided here.
 * 
 * @author WeAthFolD, Shielian, KS
 */
@Registrant
@RegEventHandler(Bus.Forge)
@RegACRecipeNames
public class ModuleCrafting {
    
    // CONFIGS
    public static boolean GENERATE_ORES, GENERATE_PHASE_LIQUID;

    // MATERIAL BLOCKS
    @RegBlock(item = BlockImagPhase.ItemPhaseLiq.class)
    public static BlockImagPhase imagPhase;

    @RegBlock
    @RegBlock.OreDict("machineFrame")
    @RecipeName("frame")
    public static Block machineFrame = new ACBlock("machine_frame", Material.rock).setHardness(4.0f);

    @RegBlock
    @RegBlock.OreDict("oreConstraintMetal")
    @RecipeName("cons_ore")
    public static BlockGenericOre oreConstraintMetal = new BlockGenericOre("constraint_metal_ore", 3.0f, 1);

    @RegBlock
    @RegBlock.OreDict("oreImagCrystal")
    @RecipeName("crystal_ore")
    public static BlockGenericOre oreImagCrystal = new BlockGenericOre("crystal_ore", 2.0f, 2);

    @RegBlock
    @RegBlock.OreDict("oreImagSilicon")
    @RecipeName("imagsil_ore")
    public static BlockGenericOre oreImagSil = new BlockGenericOre("imag_silicon_ore", 2.75f, 2);

    @RegBlock
    @RegBlock.OreDict("oreResonantCrystal")
    @RecipeName("reso_ore")
    public static BlockGenericOre oreResoCrystal = new BlockGenericOre("reso_crystal_ore", 2f, 2);

    // CRAFTING MACHINES
    @RegBlock
    @RecipeName("fusor")
    public static BlockImagFusor imagFusor;

    @RegBlock
    @RecipeName("metal_former")
    public static BlockMetalFormer metalFormer;

    // ITEMS
    // BASIC MATERIALS
    @RegItem
    @RegItem.OreDict("gemImagCrystalLow")
    @RecipeName("crystal0")
    public static Item crystalLow = new ACItem("crystal_low");

    @RegItem
    @RegItem.OreDict("gemImagCrystalNormal")
    @RecipeName("crystal1")
    public static Item crystalNormal = new ACItem("crystal_normal");

    @RegItem
    @RegItem.OreDict("gemImagCrystalPure")
    @RecipeName("crystal2")
    public static Item crystalPure = new ACItem("crystal_pure");

    @RegItem
    @RegItem.OreDict("calcChip")
    @RecipeName("calc_chip")
    public static Item calcChip = new ACItem("calc_chip");

    @RegItem
    @RegItem.OreDict("dataChip")
    @RecipeName("data_chip")
    public static Item dataChip = new ACItem("data_chip");

    @RegItem
    @RegItem.OreDict("wafer")
    @RecipeName("wafer")
    public static Item wafer = new ACItem("wafer");

    @RegItem
    @RegItem.OreDict("ingotConstraintMetal")
    @RecipeName("cons_ingot")
    public static Item ingotConst = new ACItem("constraint_ingot");

    @RegItem
    @RegItem.OreDict("ingotImagSilicon")
    @RecipeName("imagsil_ingot")
    public static Item ingotImagSil = new ACItem("imag_silicon_ingot");

    @RegItem
    @RegItem.OreDict("plateIron")
    @RecipeName("rf_iron_plate")
    public static Item rfIronPlate = new ACItem("reinforced_iron_plate");

    @RegItem
    @RegItem.OreDict("pieceImagSilicon")
    @RecipeName("si_piece")
    public static Item silPiece = new ACItem("imag_silicon_piece");

    @RegItem
    @RegItem.OreDict("gemResonantCrystal")
    @RecipeName("reso_crystal")
    public static Item resoCrystal = new ACItem("reso_crystal");

    @RegItem
    @RegItem.OreDict("plateConstraintMetal")
    @RecipeName("cons_plate")
    public static Item constPlate = new ACItem("constraint_plate");

    // ADVANCED MATERIALS
    @RegItem
    @RecipeName("brain_comp")
    public static Item brainComp = new ACItem("brain_component");

    @RegItem
    @RecipeName("info_comp")
    public static Item infoComp = new ACItem("info_component");

    @RegItem
    @RecipeName("reso_comp")
    public static Item resoComp = new ACItem("resonance_component");

    @RegItem
    @RecipeName("conv_comp")
    public static Item convComp = new ACItem("energy_convert_component");

    // MISC
    @RegItem
    @RegItem.HasRender
    @RecipeName("matter_unit")
    public static ItemMatterUnit matterUnit;

    @RegWorldGen(2)
    public static ACWorldGen worldGen;

    public static Fluid fluidImagProj = new Fluid("imagProj");

    static {
        fluidImagProj.setLuminosity(8).setDensity(7000).setViscosity(6000).setTemperature(0).setDensity(1);
        FluidRegistry.registerFluid(fluidImagProj);
    }

    @RegInitCallback
    public static void init() {
        GENERATE_ORES = AcademyCraft.config.getBoolean("genOres", "generic", true, "Whether the ores will be generated in overworld.");
        GENERATE_PHASE_LIQUID = AcademyCraft.config.getBoolean("genPhaseLiquid", "generic", true, "Whether phase liquid will be generated in overworld.");
        
        FluidContainerRegistry.registerFluidContainer(new FluidStack(fluidImagProj, 1000),
                matterUnit.create("phase_liquid"), matterUnit.create("none"));

        oreImagCrystal.setDropData(crystalLow, 1, 3);
        oreResoCrystal.setDropData(resoCrystal, 1, 2);

        machineFrame.setHarvestLevel("pickaxe", 3);

        ImagFusorRecipes.INSTANCE.addRecipe(new ItemStack(crystalLow), 3000, new ItemStack(crystalNormal));
        ImagFusorRecipes.INSTANCE.addRecipe(new ItemStack(crystalNormal), 8000, new ItemStack(crystalPure));

        MetalFormerRecipes.INSTANCE.add(new ItemStack(ingotImagSil), new ItemStack(wafer, 2), Mode.INCISE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(wafer), new ItemStack(silPiece, 4), Mode.INCISE);

        MetalFormerRecipes.INSTANCE.add(new ItemStack(dataChip), new ItemStack(calcChip), Mode.ETCH);

        MetalFormerRecipes.INSTANCE.add(new ItemStack(Items.iron_ingot), new ItemStack(rfIronPlate), Mode.PLATE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(ingotConst), new ItemStack(ModuleCrafting.constPlate),
                Mode.PLATE);

        MetalFormerRecipes.INSTANCE.add(new ItemStack(oreImagSil), new ItemStack(ingotImagSil, 4), Mode.REFINE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(oreConstraintMetal), new ItemStack(ingotConst, 2), Mode.REFINE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(oreResoCrystal), new ItemStack(resoCrystal, 3), Mode.REFINE);
        MetalFormerRecipes.INSTANCE.add(new ItemStack(oreImagCrystal), new ItemStack(crystalLow, 4), Mode.REFINE);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void textureHook(TextureStitchEvent.Post event) {
        fluidImagProj.setIcons(imagPhase.fluidIcon);
    }

}
