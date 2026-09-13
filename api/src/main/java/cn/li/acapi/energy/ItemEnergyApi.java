package cn.li.acapi.energy;

import cn.li.acapi.energy.handle.EnergyItemHandle;

import java.util.Objects;

/**
 * Strongly typed facade for item energy operations.
 */
public final class ItemEnergyApi {

    public interface Bridge {
        boolean isSupported(EnergyItemHandle item);

        double getEnergy(EnergyItemHandle item);

        double getMaxEnergy(EnergyItemHandle item);

        void setEnergy(EnergyItemHandle item, double amount);

        double charge(EnergyItemHandle item, double amount, boolean ignoreBandwidth);

        double pull(EnergyItemHandle item, double amount, boolean ignoreBandwidth);

        String getDescription(EnergyItemHandle item);
    }

    private static final Bridge NO_OP = new Bridge() {
        @Override
        public boolean isSupported(EnergyItemHandle item) {
            return false;
        }

        @Override
        public double getEnergy(EnergyItemHandle item) {
            return 0.0;
        }

        @Override
        public double getMaxEnergy(EnergyItemHandle item) {
            return 0.0;
        }

        @Override
        public void setEnergy(EnergyItemHandle item, double amount) {
            // no-op
        }

        @Override
        public double charge(EnergyItemHandle item, double amount, boolean ignoreBandwidth) {
            return amount;
        }

        @Override
        public double pull(EnergyItemHandle item, double amount, boolean ignoreBandwidth) {
            return 0.0;
        }

        @Override
        public String getDescription(EnergyItemHandle item) {
            return "0/0 IF";
        }
    };

    private static volatile Bridge bridge = NO_OP;

    private ItemEnergyApi() {}

    public static void installBridge(Bridge runtimeBridge) {
        bridge = Objects.requireNonNull(runtimeBridge, "runtimeBridge");
    }

    public static boolean isSupported(EnergyItemHandle item) {
        return bridge.isSupported(item);
    }

    public static double getEnergy(EnergyItemHandle item) {
        return bridge.getEnergy(item);
    }

    public static double getMaxEnergy(EnergyItemHandle item) {
        return bridge.getMaxEnergy(item);
    }

    public static void setEnergy(EnergyItemHandle item, double amount) {
        bridge.setEnergy(item, amount);
    }

    /**
     * @return energy that could not be inserted.
     */
    public static double charge(EnergyItemHandle item, double amount, boolean ignoreBandwidth) {
        return bridge.charge(item, amount, ignoreBandwidth);
    }

    public static double charge(EnergyItemHandle item, double amount) {
        return charge(item, amount, false);
    }

    /**
     * @return energy actually pulled.
     */
    public static double pull(EnergyItemHandle item, double amount, boolean ignoreBandwidth) {
        return bridge.pull(item, amount, ignoreBandwidth);
    }

    public static String getDescription(EnergyItemHandle item) {
        return bridge.getDescription(item);
    }
}
