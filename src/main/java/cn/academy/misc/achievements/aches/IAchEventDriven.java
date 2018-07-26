package cn.academy.misc.achievements.aches;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * @author EAirPeter
 */
public interface IAchEventDriven<Ev extends Event> {
    
    boolean accept(Ev event);

}