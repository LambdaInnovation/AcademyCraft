package cn.li.acapi.integration;

/**
 * Energy Converter interface for external mod integration.
 * Provides access to converter state and configuration.
 */
public interface IEnergyConverter {
    /**
     * Get current stored energy in IF (Imaginary Energy).
     * @return Current energy stored
     */
    double getStoredEnergy();

    /**
     * Get maximum energy capacity in IF.
     * @return Maximum energy capacity
     */
    double getMaxEnergy();

    /**
     * Get conversion rate from IF to Forge Energy.
     * @return Conversion rate (1 IF = X FE)
     */
    double getConversionRate();

    /**
     * Set conversion rate from IF to Forge Energy.
     * @param rate Conversion rate (1 IF = X FE)
     */
    void setConversionRate(double rate);

    /**
     * Get current operation mode.
     * @return Mode string ("charge-items", "export-fe", "import-fe")
     */
    String getMode();

    /**
     * Set operation mode.
     * @param mode Mode string ("charge-items", "export-fe", "import-fe")
     */
    void setMode(String mode);
}
