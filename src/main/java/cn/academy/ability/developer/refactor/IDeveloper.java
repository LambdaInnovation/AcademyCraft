package cn.academy.ability.developer.refactor;

import cn.academy.ability.developer.DeveloperType;

/**
 * Abstract interface for developer, used by DevelopData.
 * A IDeveloper must be Instance-Serializable, so that progress can be displayed correctly in client.
 */
public interface IDeveloper {

    DeveloperType getType();

    boolean pullEnergy(double amount);

    double getEnergy();

    double getMaxEnergy();

}
