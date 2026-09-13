package cn.li.acapi.energy;

import cn.li.acapi.wireless.IWirelessNode;

/**
 * Typed node energy operations.
 */
public final class NodeEnergyApi {
    private NodeEnergyApi() {}

    public static boolean isSupported(IWirelessNode node) {
        return node != null;
    }

    public static double getEnergy(IWirelessNode node) {
        return node != null ? node.getEnergy() : 0.0;
    }

    public static void setEnergy(IWirelessNode node, double energy) {
        if (node != null) {
            node.setEnergy(energy);
        }
    }

    /**
     * @return energy that could not be inserted.
     */
    public static double charge(IWirelessNode node, double amount, boolean ignoreBandwidth) {
        if (node == null) {
            return amount;
        }

        double maxInsert = Math.max(0.0, node.getMaxEnergy() - node.getEnergy());
        double inserted = Math.max(0.0, Math.min(amount, maxInsert));
        if (!ignoreBandwidth) {
            inserted = Math.min(inserted, Math.max(0.0, node.getBandwidth()));
        }

        node.setEnergy(node.getEnergy() + inserted);
        return amount - inserted;
    }

    /**
     * @return energy actually pulled.
     */
    public static double pull(IWirelessNode node, double amount, boolean ignoreBandwidth) {
        if (node == null) {
            return 0.0;
        }

        double pulled = Math.max(0.0, Math.min(amount, node.getEnergy()));
        if (!ignoreBandwidth) {
            pulled = Math.min(pulled, Math.max(0.0, node.getBandwidth()));
        }

        node.setEnergy(node.getEnergy() - pulled);
        return pulled;
    }
}
