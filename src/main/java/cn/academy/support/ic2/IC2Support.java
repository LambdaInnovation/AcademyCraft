package cn.academy.support.ic2;

import cn.academy.core.AcademyCraft;
import cn.academy.crafting.ModuleCrafting;
import cn.academy.energy.ModuleEnergy;
import cn.academy.support.BlockConverterBase;
import cn.academy.support.EnergyBlockHelper;
import cn.academy.support.EnergyItemHelper;
import cn.academy.support.EnergyItemHelper.EnergyItemManager;
import com.google.common.base.Preconditions;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.registry.GameRegistry;
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
    
    /**
     * The convert rate from EU to IF(1IF = <CONV_RATE>EU).
     */
    public static final double CONV_RATE = 1;

    private static final String MODID = "IC2";

    private static IC2SkillHelper helper;
    
    public static double eu2if(double euEnergy) {
        return euEnergy / CONV_RATE;
    }
    
    public static double if2eu(double ifEnergy) {
        return ifEnergy * CONV_RATE;
    }

    @Optional.Method(modid=MODID)
    @RegInitCallback
    public static void init() {
        helper = new IC2SkillHelper();
        helper.init();

        BlockEUInput euInput = new BlockEUInput();
        BlockEUOutput euOutput = new BlockEUOutput();
        
        GameRegistry.registerBlock(euInput, BlockConverterBase.Item.class, "eu_input");
        GameRegistry.registerBlock(euOutput, BlockConverterBase.Item.class, "eu_output");
        
        GameRegistry.registerTileEntity(TileEUInput.class, "eu_input");
        GameRegistry.registerTileEntity(TileEUOutput.class, "eu_output");
        
        EnergyBlockHelper.register(new EUSinkManager());
        EnergyBlockHelper.register(new EUSourceManager());
        
        GameRegistry.addRecipe(new ItemStack(euInput), "abc", " d ",
                'a', ModuleEnergy.energyUnit, 'b', ModuleCrafting.machineFrame,
                'c', IC2Items.getItem("insulatedCopperCableItem"), 'd', ModuleCrafting.convComp);
        GameRegistry.addRecipe(new ItemStack(euOutput), "abc", " d ",
                'a', IC2Items.getItem("batBox"), 'b', ModuleCrafting.machineFrame,
                'c', IC2Items.getItem("insulatedCopperCableItem"), 'd', ModuleCrafting.convComp);
        
        GameRegistry.addRecipe(new ItemStack(euInput),"X",'X',new ItemStack(euOutput));
        GameRegistry.addRecipe(new ItemStack(euOutput),"X",'X',new ItemStack(euInput));

        EnergyItemHelper.register(new IC2EnergyItemManager());

        AcademyCraft.log.info("IC2 API Support has been loaded.");
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