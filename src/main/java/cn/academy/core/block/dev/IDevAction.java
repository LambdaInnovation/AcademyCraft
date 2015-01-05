/**
 * 
 */
package cn.academy.core.block.dev;

import cn.academy.api.data.AbilityData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Defines an AbilityDeveloper action. The initialization and judgement are done in server side.
 * @author WeathFolD
 */
public interface IDevAction {
	/**
	 * Get the expected stimulation times.
	 */
	int getExpectedStims(AbilityData data);
	/**
	 * Do something when the consumption is finished.
	 */
	void onActionFinished(AbilityData data);
	
	/**
	 * Get a human readable information about this action. Typically used in GUIs.
	 */
	@SideOnly(Side.CLIENT)
	String getActionInfo(AbilityData data);
}
