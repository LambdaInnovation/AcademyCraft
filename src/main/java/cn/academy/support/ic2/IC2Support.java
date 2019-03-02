package cn.academy.support.ic2;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyItemHelper;
import cn.academy.support.EnergyItemHelper.EnergyItemManager;
import cn.lambdalib2.registry.RegistryCallback;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import ic2.api.item.ElectricItem;
import ic2.api.item.IC2Items;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import net.minecraft.item.ItemStack;

/**
 * 
 * @author KSkun
 */
public class IC2Support {
    static final String IC2_MODID = "ic2";

    // A placeholder interface to express @Optional.Interface dependency
    static final String IC2_IFACE = "ic2.api.energy.tile.IEnergySource";

    /**
     * The convert rate from EU to IF(1IF = <CONV_RATE>EU).
     */
    public static final double CONV_RATE = 1;

    private static IC2SkillHelper helper;

    public static final BlockEUInput euInput = new BlockEUInput();
    public static final BlockEUOutput euOutput = new BlockEUOutput();


    public static final ItemBlock item_euInput = new ItemBlock(euInput);
    public static final ItemBlock item_euOutput = new ItemBlock(euOutput);
    
    public static double eu2if(double euEnergy) {
        return euEnergy / CONV_RATE;
    }
    
    public static double if2eu(double ifEnergy) {
        return ifEnergy * CONV_RATE;
    }

    @Optional.Method(modid= IC2_MODID)
    @StateEventCallback
    public static void init(FMLInitializationEvent event){
        EnergyBlockHelper.register(new EUSinkManager());
        EnergyBlockHelper.register(new EUSourceManager());

        // https://github.com/TinyModularThings/IC2Classic/blob/master/src/main/java/ic2/api/item/IC2Items.java
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","eu_input"), null,
                new ItemStack(euInput), "abc", " d ",
                'a', ACItems.energy_unit, 'b', ACBlocks.machine_frame,
                'c', IC2Items.getItem("cable", "type:copper,insulation:1"), 'd', ACItems.energy_convert_component);
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","eu_output"), null,
                new ItemStack(euOutput), "abc", " d ",
                'a', IC2Items.getItemAPI().getItemStack("te", "batbox"), 'b', ACBlocks.machine_frame,
                'c', IC2Items.getItem("cable", "type:copper,insulation:1"), 'd', ACItems.energy_convert_component);

        GameRegistry.addShapedRecipe(new ResourceLocation("academy","eu_input_output"), null,
                new ItemStack(euInput),"X",'X',new ItemStack(euOutput));
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","eu_output_input"), null,
                new ItemStack(euOutput),"X",'X',new ItemStack(euInput));

        EnergyItemHelper.register(new IC2EnergyItemManager());

        AcademyCraft.log.info("IC2 API Support has been loaded.");
    }


    @Optional.Method(modid= IC2_MODID)
    @RegistryCallback
    private static void registerBlocks(RegistryEvent.Register<Block> event) {
        helper = new IC2SkillHelper();
        helper.init();

        euInput.setRegistryName("academy:eu_input");
        euInput.setTranslationKey("ac_eu_input");
        event.getRegistry().register(euInput);

        euOutput.setRegistryName("academy:eu_output");
        euOutput.setTranslationKey("ac_eu_output");
        event.getRegistry().register(euOutput);
    }

    @Optional.Method(modid= IC2_MODID)
    @RegistryCallback
    private static void registerItems(RegistryEvent.Register<Item> event){
        item_euInput.setRegistryName(euInput.getRegistryName());
        item_euInput.setTranslationKey(euInput.getTranslationKey());
        event.getRegistry().register(item_euInput);
        if(SideUtils.isClient()){
            ModelLoader.setCustomModelResourceLocation(item_euInput, 0,
                    new ModelResourceLocation("academy:eu_input", "inventory"));
        }


        item_euOutput.setRegistryName(euOutput.getRegistryName());
        item_euOutput.setTranslationKey(euOutput.getTranslationKey());
        event.getRegistry().register(item_euOutput);
        if(SideUtils.isClient()){
            ModelLoader.setCustomModelResourceLocation(item_euOutput, 0,
                    new ModelResourceLocation("academy:eu_output", "inventory"));
        }
    }

    public static IC2SkillHelper getHelper() {
        return helper;
    }

}

class IC2EnergyItemManager implements EnergyItemManager {

    private IElectricItemManager manager() {
        return Preconditions.checkNotNull(ElectricItem.manager);
    }

    @Override
    public boolean isSupported(ItemStack stack) {
        return stack.getItem() instanceof IElectricItem;
    }

    @Override
    public double getEnergy(ItemStack stack) {
        return manager().getCharge(stack);
    }

    @Override
    public void setEnergy(ItemStack stack, double energy) {
        double current = getEnergy(stack);
        double delta = energy - current;

        if (delta > 0) {
            manager().charge(stack, delta, 10, true, false);
        } else {
            manager().discharge(stack, -delta, 10, true, false, false);
        }
    }

    @Override
    public double charge(ItemStack stack, double amt, boolean ignoreBandwidth) {
        double transferred = manager().charge(stack, amt, 10, ignoreBandwidth, false);
        return amt - transferred;
    }

    @Override
    public double pull(ItemStack stack, double amt, boolean ignoreBandwidth) {
        double pulled = manager().discharge(stack, amt, 10, ignoreBandwidth, true, false);
        return pulled;
    }
}