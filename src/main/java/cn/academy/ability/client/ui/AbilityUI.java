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
package cn.academy.ability.client.ui;

import net.minecraft.client.gui.ScaledResolution;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;

/**
 * @author WeAthFolD
 */
@RegistrationClass
@RegAuxGui
public class AbilityUI extends AuxGui {
	
	LIGui scene = new LIGui();
	
	public AbilityUI() {
		CPBar cpbar = new CPBar();
		cpbar.transform.x = 900;
		
		scene.addWidget(cpbar);
	}

	@Override
	public boolean isOpen() {
		return true;
	}
	
	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		scene.draw(sr.getScaledWidth_double(), sr.getScaledHeight_double());
	}

}
