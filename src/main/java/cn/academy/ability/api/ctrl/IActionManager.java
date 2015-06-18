package cn.academy.ability.api.ctrl;

interface IActionManager {
	void startAction(SyncAction action);
	void endAction(SyncAction action);
	void abortAction(SyncAction action);
	void abortActionLocally(SyncAction action);
}
