package cn.academy.support;

import cn.academy.energy.api.IFItemManager;
import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic-typed item energy helper.
 * 
 * @author WeAthFolD
 */
public class EnergyItemHelper {

    private static List<EnergyItemManager> supported = new ArrayList<>();

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        register(IFItemManager.instance);
    }

    public static void register(EnergyItemManager man) {
        supported.add(man);
    }

    public static boolean isSupported(ItemStack stack) {
        for (EnergyItemManager m : supported) {
            if (m.isSupported(stack))
                return true;
        }
        return false;
    }

    public static double getEnergy(ItemStack stack) {
        for (EnergyItemManager m : supported) {
            if (m.isSupported(stack))
                return m.getEnergy(stack);
        }
        return 0.0;
    }

    public static void setEnergy(ItemStack stack, double energy) {
        for (EnergyItemManager m : supported) {
            if (m.isSupported(stack)) {
                m.setEnergy(stack, energy);
                return;
            }
        }
    }

    public static double charge(ItemStack stack, double amt, boolean ignoreBandwidth) {
        for (EnergyItemManager m : supported) {
            if (m.isSupported(stack)) {
                return m.charge(stack, amt, ignoreBandwidth);
            }
        }
        return amt;
    }

    public static double pull(ItemStack stack, double amt, boolean ignoreBandwidth) {
        for (EnergyItemManager m : supported) {
            if (m.isSupported(stack)) {
                return m.pull(stack, amt, ignoreBandwidth);
            }
        }
        return 0;
    }

    public static ItemStack createEmptyItem(Item item) {
        ItemStack ret = new ItemStack(item);
        charge(ret, 0, true);
        return ret;
    }

    public static ItemStack createFullItem(Item item) {
        ItemStack ret = new ItemStack(item);
        charge(ret, Integer.MAX_VALUE, true);
        return ret;
    }

    public interface EnergyItemManager {

        boolean isSupported(ItemStack stack);

        double getEnergy(ItemStack stack);

        void setEnergy(ItemStack stack, double energy);

        /**
         * @return How much energy not transfered into stack(left)
         */
        double charge(ItemStack stack, double amt, boolean ignoreBandwidth);

        /**
         * @return How much energy pulled out of stack
         */
        double pull(ItemStack stack, double amt, boolean ignoreBandwidth);

    }

}