package cn.li.forge1201.shim;

import clojure.lang.IFn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Java skeleton for {@link IFluidHandler} — replaces Clojure reify on Forge MC interfaces.
 */
public class UniversalFluidHandler implements IFluidHandler {
    private final IFn getTanksFn;
    private final IFn getFluidInTankFn;
    private final IFn getTankCapacityFn;
    private final IFn isFluidValidFn;
    private final IFn fillFn;
    private final IFn drainMaxFn;
    private final IFn drainResourceFn;

    public UniversalFluidHandler(IFn getTanksFn,
                                 IFn getFluidInTankFn,
                                 IFn getTankCapacityFn,
                                 IFn isFluidValidFn,
                                 IFn fillFn,
                                 IFn drainMaxFn,
                                 IFn drainResourceFn) {
        this.getTanksFn = getTanksFn;
        this.getFluidInTankFn = getFluidInTankFn;
        this.getTankCapacityFn = getTankCapacityFn;
        this.isFluidValidFn = isFluidValidFn;
        this.fillFn = fillFn;
        this.drainMaxFn = drainMaxFn;
        this.drainResourceFn = drainResourceFn;
    }

    @Override
    public int getTanks() {
        return getTanksFn == null ? 0 : ((Number) getTanksFn.invoke()).intValue();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (getFluidInTankFn == null) return FluidStack.EMPTY;
        FluidStack stack = (FluidStack) getFluidInTankFn.invoke(tank);
        return stack != null ? stack : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        if (getTankCapacityFn == null) return 0;
        return ((Number) getTankCapacityFn.invoke(tank)).intValue();
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (isFluidValidFn == null) return true;
        return (boolean) isFluidValidFn.invoke(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (fillFn == null) return 0;
        return ((Number) fillFn.invoke(resource, action)).intValue();
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (drainMaxFn == null) return FluidStack.EMPTY;
        FluidStack stack = (FluidStack) drainMaxFn.invoke(maxDrain, action);
        return stack != null ? stack : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (drainResourceFn == null) return FluidStack.EMPTY;
        FluidStack stack = (FluidStack) drainResourceFn.invoke(resource, action);
        return stack != null ? stack : FluidStack.EMPTY;
    }
}
