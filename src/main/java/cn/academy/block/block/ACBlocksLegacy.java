package cn.academy.block.block;

public class ACBlocksLegacy {
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

    @RegBlock(item = BlockImagPhase.ItemPhaseLiq.class)
    public static BlockImagPhase imagPhase;

    @RegBlock
    @RegBlock.OreDict("machineFrame")
    @RecipeName("frame")
    public static Block machineFrame = new ACBlock("machine_frame", Material.rock).setHardness(4.0f);

    @RegBlock
    @RegBlock.OreDict("oreConstraintMetal")
    @RecipeName("cons_ore")
    public static BlockGenericOre oreConstraintMetal = new BlockGenericOre("constraint_metal_ore", 4.0f, 1);

    @RegBlock
    @RegBlock.OreDict("oreImagCrystal")
    @RecipeName("crystal_ore")
    public static BlockGenericOre oreImagCrystal = new BlockGenericOre("crystal_ore", 3.0f, 2);

    @RegBlock
    @RegBlock.OreDict("oreImagSilicon")
    @RecipeName("imagsil_ore")
    public static BlockGenericOre oreImagSil = new BlockGenericOre("imag_silicon_ore", 3.75f, 2);

    @RegBlock
    @RegBlock.OreDict("oreResonantCrystal")
    @RecipeName("reso_ore")
    public static BlockGenericOre oreResoCrystal = new BlockGenericOre("reso_crystal_ore", 3f, 2);

    // CRAFTING MACHINES
    @RegBlock
    @RecipeName("fusor")
    public static BlockImagFusor imagFusor;

    @RegBlock
    @RecipeName("metal_former")
    public static BlockMetalFormer metalFormer;

    private static void init() {
        oreImagCrystal.setDropData(crystalLow, 1, 3);
        oreResoCrystal.setDropData(resoCrystal, 1, 2);
        machineFrame.setHarvestLevel("pickaxe", 1);
    }
}
