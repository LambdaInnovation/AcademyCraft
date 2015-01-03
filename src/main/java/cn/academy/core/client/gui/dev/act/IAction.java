/**
 * 
 */
package cn.academy.core.client.gui.dev.act;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



import cn.academy.api.data.AbilityData;

/**
 * Defines a single developer action.
 * The synchronization is automatically handled.
 * @author WeathFolD
 */
@Deprecated
public abstract class IAction {
	private List<IAction> dependencies = new ArrayList<IAction>();
	
	/**
	 * Called only in server side, when EU and EXP are already consumed and do the action.
	 */
	public abstract void execute(AbilityData data);
	/**
	 * Get the EU needed to consume corresponding to the ability data
	 */
	public abstract double getRawEU(AbilityData data);
	/**
	 * Get the EXP needed to consume corresponding to the ability data
	 */
	public abstract double getRawExp(AbilityData data);
	/**
	 * If some other IActions are necessary before this action executes, use this to add dependency.
	 */
	protected final void addDependency(IAction... act) {
		dependencies.addAll(Arrays.asList(act));
	}
	/**
	 * Get all the dependency actions of this action.
	 */
	public List<IAction> getDependencies() {
		return dependencies;
	}
}
