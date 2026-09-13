package cn.li.acapi.integration;

/**
 * IC2 Energy Converter interface for external mod integration.
 *
 * This interface extends the base energy converter with IC2-specific
 * functionality for EU (Energy Units) conversion.
 */
public interface IIC2EnergyConverter extends IEnergyConverter {
    /**
     * Get conversion rate from IF to EU (Energy Units).
     * @return Conversion rate (1 IF = X EU)
     */
    double getEUConversionRate();

    /**
     * Set conversion rate from IF to EU.
     * @param rate Conversion rate (1 IF = X EU)
     */
    void setEUConversionRate(double rate);

    /**
     * Check if IC2 integration is enabled for this converter.
     * @return true if IC2 integration is active
     */
    boolean isIC2Enabled();

    /**
     * Enable or disable IC2 integration for this converter.
     * @param enabled true to enable IC2 integration
     */
    void setIC2Enabled(boolean enabled);

    /**
     * Get the IC2 energy tier for this converter.
     * Tier 1 = Low Voltage (32 EU/t)
     * Tier 2 = Medium Voltage (128 EU/t)
     * Tier 3 = High Voltage (512 EU/t)
     * Tier 4 = Extreme Voltage (2048 EU/t)
     * @return IC2 energy tier (1-4)
     */
    int getIC2Tier();

    /**
     * Set the IC2 energy tier for this converter.
     * @param tier IC2 energy tier (1-4)
     */
    void setIC2Tier(int tier);
}
