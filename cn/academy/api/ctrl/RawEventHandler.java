package cn.academy.api.ctrl;

/**
 * This class handles raw event and send the proper event to skill.
 * It has its own timer based on tick event called by EventHandler.
 * There's no difference in its behavior of client side and server side instances. 
 * @author acaly
 *
 */
public class RawEventHandler {
	
	/*
	 * Global constants used by EventHandlers
	 */

	public static final int KA_INTERVAL = 20,
						KA_DELAY = 10,
						DBL_DELAY = 10;
	
	/**
	 * Receive raw event. Judge with time given time.
	 * Trigger skill event with its own time.
	 * @param type
	 * @param time On server, it's time on client (sent in Message). On client, it's time get by getTime.
	 */
	public void onEvent(SkillEventType type, int time) {
		switch (type) {
		case RAW_DOWN:
			//never get dblclick directly from eventhandler.
		case RAW_UP:
			//directly send
		case RAW_TICK_DOWN:
			//time is server time
		case RAW_CLIENT_DOWN:
			//received time from client
			//consider carefully how to use it
		case RAW_CLIENT_UP:
			//single click
		//case RAW_TICK_UP:
		//case RAW_TICK:
		case RAW_CANCEL:
		default:
			break;
		}
	}
	
	/**
	 * Get current skill time on this side.
	 * @return
	 */
	public int getTime() {
		return 0;
	}
}
