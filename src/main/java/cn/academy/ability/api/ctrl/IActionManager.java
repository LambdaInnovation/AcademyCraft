package cn.academy.ability.api.ctrl;

/**
 * @author EAirPeter
 */
interface IActionManager {
	void startAction(SyncAction action);
	void endAction(SyncAction action);
	void abortAction(SyncAction action);
	void abortActionLocally(SyncAction action);
}
