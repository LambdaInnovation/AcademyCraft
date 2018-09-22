package cn.academy.support.rf;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.support.EnergyBlockHelper;
import cn.lambdalib2.registry.RegistryCallback;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

public class RFSupport {
    
    /** The convert rate (1IF = <CONV_RATE> RF) */
    public static final double CONV_RATE = 4;

    public static Block rfInput = new BlockRFInput();
    public static Block rfOutput = new BlockRFOutput();

    public static final ItemBlock item_rfInput = new ItemBlock(rfInput);
    public static final ItemBlock item_rfOutput = new ItemBlock(rfOutput);

    
    // Convert macros, dividing by hand is error-prone
    /**
     * Converts RF to equivalent amount of IF.
     */
    public static double rf2if(int rfEnergy) {
        return rfEnergy / CONV_RATE;
    }
    /**
     * Converts IF to equivalent amount of RF.
     */
    public static int if2rf(double ifEnergy) {
        return (int) (ifEnergy * CONV_RATE);
    }

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        EnergyBlockHelper.register(new RFProviderManager());
        EnergyBlockHelper.register(new RFReceiverManager());
        
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","rf_input"),null,
                new ItemStack(rfInput), "abc", " d ",
                'a', ACItems.energy_unit, 'b', ACBlocks.machine_frame,
                'c', ACItems.constraint_plate, 'd', ACItems.energy_convert_component);
        
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","rf_output"),null,
                new ItemStack(rfOutput), "abc", " d ",
                'a', ACItems.energy_unit, 'b', ACBlocks.machine_frame,
                'c', ACItems.reso_crystal, 'd', ACItems.energy_convert_component);
        
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","rf_input_output"),null,
                new ItemStack(rfInput), "X",'X',new ItemStack(rfOutput));
        GameRegistry.addShapedRecipe(new ResourceLocation("academy","rf_output_input"),null,
                new ItemStack(rfOutput), "X",'X',new ItemStack(rfInput));
    }

    @RegistryCallback
    private static void registerBlocks(RegistryEvent.Register<Block> event) {

        rfInput.setRegistryName("academy:ac_rf_input");
        rfInput.setUnlocalizedName("ac_rf_input");
        event.getRegistry().register(rfInput);

        rfOutput.setRegistryName("academy:rf_output");
        rfOutput.setUnlocalizedName("rf_output");
        event.getRegistry().register(rfOutput);

    }

    @RegistryCallback
    private static void registerItems(RegistryEvent.Register<Item> event){
        item_rfInput.setRegistryName(rfInput.getRegistryName());
        item_rfInput.setUnlocalizedName(rfInput.getUnlocalizedName());
        item_rfOutput.setRegistryName(rfOutput.getRegistryName());
        item_rfOutput.setUnlocalizedName(rfOutput.getUnlocalizedName());

    }
    
}