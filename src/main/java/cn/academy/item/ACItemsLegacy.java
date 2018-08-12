package cn.academy.item;

import cn.lambdalib2.crafting.CustomMappingHelper.RecipeName;

import java.util.Arrays;

public class ACItemsLegacy {
    //@RegItem.HasRender
    @RecipeName("coin")
    @ObjectHolder("coin")
    public static final ItemCoin coin = null;

    //@RegItem.HasRender
    @RecipeName("mag_hook")
    @ObjectHolder("maghook")
    public static final ItemMagHook magHook = null;

    //@RegItem.HasRender
    @RecipeName("silbarn")
    @ObjectHolder("silbarn")
    public static ItemSilbarn silbarn;

    @RecipeName("needle")
    @ObjectHolder("needle")
    public static final Item needle = null;//= new ACItem("needle");

    @RegItem
    @RecipeName("tutorial")
    public static ItemTutorial itemTutorial;

    @RegItem
    @RecipeName("ene_unit")
    public static Item energyUnit = new ItemEnergyBase("energy_unit", 10000, 20);

    @RegItem
    @RecipeName("mat_core")
    public static ItemMatrixCore matrixCore;

    @RegItem
    @RecipeName("windgen_fan")
    public static Item windgenFan = new ACItem("windgen_fan").setMaxDamage(100).setMaxStackSize(1);

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
    @RegItem.HasRender
    @RecipeName("dev_portable")
    public static ItemDeveloper developerPortable;

    @RegItem
    @RecipeName("magnetic_coil")
    public static ACItem magneticCoil = new ACItem("magnetic_coil") {
        {
            setMaxStackSize(1);
        }

        @Override
        @SideOnly(Side.CLIENT)
        @SuppressWarnings("unchecked")
        public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
            list.addAll(Arrays.asList(StatCollector.translateToLocal("item.ac_magnetic_coil.desc").split("<br>")));
        }
    };

    @RegItem
    @RecipeName("induction_factor")
    public static ItemInductionFactor inductionFactor;

    @StateEventCallback
    public static void preInit(FMLPreInitializationEvent event){
        ModelLoader.setCustomMeshDefinition(coin, ItemCoin.renderCoin);
        ModelLoader.setCustomMeshDefinition(magHook, ItemMagHook.render);
        ModelLoader.setCustomMeshDefinition(silbarn, ItemSilbarn.render);
    }

    @RegInitCallback
    private static void init() {
        FluidContainerRegistry.registerFluidContainer(new FluidStack(ModuleCrafting.fluidImagProj, 1000),
            EnergyItemHelper.createFullItem(energyUnit), EnergyItemHelper.createEmptyItem(energyUnit));

        String[] factorAppearance = { MINESHAFT_CORRIDOR, PYRAMID_DESERT_CHEST, PYRAMID_JUNGLE_CHEST, STRONGHOLD_LIBRARY,
            DUNGEON_CHEST };

        // TODO test generation density
        for (String s : factorAppearance) {
            for (Category c : CategoryManager.INSTANCE.getCategories()) {
                ItemStack stack = inductionFactor.create(c);
                ChestGenHooks.addItem(s, new WeightedRandomChestContent(stack, 1, 1, 4));
            }
        }
    }
}
