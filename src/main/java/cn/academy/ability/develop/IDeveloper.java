package cn.academy.ability.develop;

/**
 * Abstract interface for developer, used by DevelopData.
 * A IDeveloper must be Instance-Serializable, so that progress can be displayed correctly in client.
 */
public interface IDeveloper {

    DeveloperType getType();

    boolean tryPullEnergy(double amount);

    double getEnergy();

    double getMaxEnergy();

    default void onGuiClosed() {}

}