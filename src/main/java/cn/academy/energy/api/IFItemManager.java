package cn.academy.energy.api;

import cn.academy.energy.api.item.ImagEnergyItem;
import cn.academy.support.EnergyItemHelper.EnergyItemManager;
import cn.lambdalib2.util.StackUtils;
import net.minecraft.item.ItemStack;

/**
 * @author WeathFolD
 */
public final class IFItemManager implements EnergyItemManager {

    public static IFItemManager instance = new IFItemManager();

    private IFItemManager() {
    }

    @Override
    public double getEnergy(ItemStack stack) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        return StackUtils.loadTag(stack).getDouble("energy");
    }

    public double getMaxEnergy(ItemStack stack) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        return item.getMaxEnergy();
    }

    @Override
    public void setEnergy(ItemStack stack, double amt) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        amt = Math.min(item.getMaxEnergy(), amt);
        StackUtils.loadTag(stack).setDouble("energy", amt);

        int approxDamage = (int) Math.round((1 - amt / getMaxEnergy(stack)) * stack.getMaxDamage());
        stack.setItemDamage(approxDamage);
    }

    /**
     * @return How much energy NOT transfered into stack
     */
    public double charge(ItemStack stack, double amt) {
        return charge(stack, amt, false);
    }

    /**
     * @param stack
     * @param amt Energy trying to charge into stack, can be negative
     * @param ignoreBandwidth
     * @return How much energy NOT transfered into stack
     */
    @Override
    public double charge(ItemStack stack, double amt, boolean ignoreBandwidth) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();
        double lim = ignoreBandwidth ? Double.MAX_VALUE : item.getBandwidth();
        double cur = getEnergy(stack);
        double spare = 0.0;
        if (amt + cur > item.getMaxEnergy()) {
            spare = cur + amt - item.getMaxEnergy();
            amt = item.getMaxEnergy() - cur;
        }

        double namt = Math.signum(amt) * Math.min(Math.abs(amt), lim);
        spare += amt - namt;

        setEnergy(stack, cur + namt);
        return spare;
    }

    public String getDescription(ItemStack stack) {
        return String.format("%.0f/%.0f IF", getEnergy(stack), getMaxEnergy(stack));
    }

    @Override
    public boolean isSupported(ItemStack stack) {
        return stack.getItem() instanceof ImagEnergyItem;
    }

    @Override
    public double pull(ItemStack stack, double amt, boolean ignoreBandwidth) {
        ImagEnergyItem item = (ImagEnergyItem) stack.getItem();

        double cur = getEnergy(stack);
        double give = Math.min(amt, cur);
        if (!ignoreBandwidth) {
            give = Math.min(give, item.getBandwidth());
        }
        setEnergy(stack, cur - give);

        return give;
    }

}