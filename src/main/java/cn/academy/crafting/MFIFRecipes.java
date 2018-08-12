package cn.academy.crafting;

import cn.academy.worldgen.WorldGenInit;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

// TODO: Make recipes data driven
public class MFIFRecipes {

    static void init(FMLInitializationEvent ev) {
        ImagFusorRecipes ifr = ImagFusorRecipes.INSTANCE;
        ifr.addRecipe(new ItemStack(crystalLow), 3000, new ItemStack(crystalNormal));
        ifr.addRecipe(new ItemStack(crystalNormal), 8000, new ItemStack(crystalPure));

        MetalFormerRecipes mfr = MetalFormerRecipes.INSTANCE;
        mfr.add(new ItemStack(ingotImagSil), new ItemStack(wafer, 2), Mode.INCISE);
        mfr.add(new ItemStack(wafer), new ItemStack(silPiece, 4), Mode.INCISE);
        mfr.add(new ItemStack(dataChip), new ItemStack(calcChip), Mode.ETCH);
        mfr.add(new ItemStack(Items.iron_ingot), new ItemStack(rfIronPlate), Mode.PLATE);
        mfr.add(new ItemStack(ingotConst), new ItemStack(WorldGenInit.constPlate), Mode.PLATE);
        mfr.add(new ItemStack(oreImagSil), new ItemStack(ingotImagSil, 4), Mode.REFINE);
        mfr.add(new ItemStack(oreConstraintMetal), new ItemStack(ingotConst, 2), Mode.REFINE);
        mfr.add(new ItemStack(oreResoCrystal), new ItemStack(resoCrystal, 3), Mode.REFINE);
        mfr.add(new ItemStack(oreImagCrystal), new ItemStack(crystalLow, 4), Mode.REFINE);

        addOreDictRefineRecipe("oreGold",new ItemStack(Items.gold_ingot,2));
        addOreDictRefineRecipe("oreIron",new ItemStack(Items.iron_ingot,2));
        addOreDictRefineRecipe("oreEmerald",new ItemStack(Items.emerald,2));
        addOreDictRefineRecipe("oreQuartz",new ItemStack(Items.quartz,2));
        addOreDictRefineRecipe("oreDiamond",new ItemStack(Items.diamond,2));
        addOreDictRefineRecipe("oreRedstone",new ItemStack(Blocks.redstone_block));
        addOreDictRefineRecipe("oreLapis",new ItemStack(Items.dye,12,4));
        addOreDictRefineRecipe("oreCoal",new ItemStack(Items.coal,2));
        addDefaultOreDictRefineRecipe("Copper");
        addDefaultOreDictRefineRecipe("Tin");
        addDefaultOreDictRefineRecipe("Lead");
        addDefaultOreDictRefineRecipe("Platinum");
        addDefaultOreDictRefineRecipe("Silver");
        addDefaultOreDictRefineRecipe("Nickel");
    }

    private static void addOreDictRefineRecipe(String orename,ItemStack output)
    {
        for (ItemStack ore : OreDictionary.getOres(orename))
            MetalFormerRecipes.INSTANCE.add(ore,output,Mode.REFINE);
    }

    private static void addOreDictRefineRecipe(String orename, String outputname)
    {
        ArrayList<ItemStack> outputList = OreDictionary.getOres(outputname);
        if (outputList == null || outputList.size() == 0)
            return;
        ItemStack output = outputList.get(0).copy();
        List<ItemStack> oreList = OreDictionary.getOres(orename);
        if (oreList == null || oreList.size() == 0)
            return;
        ItemStack stack = FurnaceRecipes.smelting().getSmeltingResult(oreList.get(0));
        if (stack == null)
            return;
        int outputsize = stack.stackSize;
        outputsize = outputsize < 32 ? (2 * outputsize) : 64;
        output.stackSize = outputsize;
        for(ItemStack ore : oreList)
        {
            MetalFormerRecipes.INSTANCE.add(ore, output, Mode.REFINE);
        }
    }

    private static void addDefaultOreDictRefineRecipe(String orename)
    {
        addOreDictRefineRecipe("ore"+orename,"ingot"+orename);
    }
}
