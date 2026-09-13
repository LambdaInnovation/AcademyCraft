package cn.li.acapi.energy;

import cn.li.acapi.wireless.IWirelessReceiver;

/**
 * Typed receiver energy operations.
 */
public final class ReceiverEnergyApi {
    private ReceiverEnergyApi() {}

    public static boolean isSupported(IWirelessReceiver receiver) {
        return receiver != null;
    }

    /**
     * Receiver contract does not expose direct storage queries.
     */
    public static double getEnergy(IWirelessReceiver receiver) {
        return 0.0;
    }

    /**
     * Receiver contract does not expose direct storage writes.
     */
    public static void setEnergy(IWirelessReceiver receiver, double energy) {
        // no-op by design
    }

    /**
     * @return energy that could not be injected.
     */
    public static double charge(IWirelessReceiver receiver, double amount, boolean ignoreBandwidth) {
        if (receiver == null) {
            return amount;
        }

        double request = Math.max(0.0, amount);
        if (!ignoreBandwidth) {
            request = Math.min(request, Math.max(0.0, receiver.getReceiverBandwidth()));
        }
        return receiver.injectEnergy(request);
    }

    /**
     * @return energy actually pulled.
     */
    public static double pull(IWirelessReceiver receiver, double amount, boolean ignoreBandwidth) {
        if (receiver == null) {
            return 0.0;
        }

        double request = Math.max(0.0, amount);
        if (!ignoreBandwidth) {
            request = Math.min(request, Math.max(0.0, receiver.getReceiverBandwidth()));
        }
        return receiver.pullEnergy(request);
    }
}
