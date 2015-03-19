/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.block.dev;

import java.util.Random;

import cn.academy.api.data.AbilityData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Defines an AbilityDeveloper action. The initialization and judgement are done in server side.
 * @author WeathFolD
 */
public interface IDevAction {
	
	Random RNG = new Random();
	
	/**
	 * Get the expected stimulation times.
	 */
	int getExpectedStims(AbilityData data);
	/**
	 * Do something when the consumption is finished.
	 */
	void onActionFinished(AbilityData data);
	/**
	 * Base successful probability.
	 */
	double getSuccessfulRate(AbilityData data);
	
	/**
	 * Validation method, in both client and server.
	 */
	boolean canPerform(AbilityData data);
	
	/**
	 * Get a human readable information about this action. Typically used in GUIs.
	 */
	@SideOnly(Side.CLIENT)
	String getActionInfo(AbilityData data);
}
