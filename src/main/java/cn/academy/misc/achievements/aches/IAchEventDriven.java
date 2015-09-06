package cn.academy.misc.achievements.aches;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * @author EAirPeter
 */
public interface IAchEventDriven<Ev extends Event> {
	
	boolean accept(Ev event);

}
