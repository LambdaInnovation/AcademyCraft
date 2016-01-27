/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
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
