/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.misc.achievements.aches;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * @author EAirPeter
 */
public interface IAchEventDriven<Ev extends Event> {
    
    boolean accept(Ev event);

}
