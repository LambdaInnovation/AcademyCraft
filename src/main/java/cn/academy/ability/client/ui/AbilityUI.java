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
import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.component.Transform.WidthAlign;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;

/**
 * @author WeAthFolD
 */
@Registrant
public class AbilityUI extends AuxGui {
	
	@RegAuxGui
	public static AbilityUI instance = new AbilityUI();
	
	LIGui scene = new LIGui();
	
	CPBar cpbar;
	
	NotifyUI notifyUI;
	
	KeyHintUI keyHintUI;
	
	/**
	 * 
	 */
	public AbilityUI() {
		cpbar = new CPBar();
		cpbar.transform.alignWidth = WidthAlign.RIGHT;
		cpbar.transform.y = 20;
		cpbar.transform.x = -20;
		
		notifyUI = new NotifyUI();
		notifyUI.transform.setPos(0, 33);
		
		keyHintUI = new KeyHintUI();
		
		scene.addWidget(cpbar);
		scene.addWidget(notifyUI);
		scene.addWidget(keyHintUI);
	}
	
	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		scene.resize(sr.getScaledWidth_double(), sr.getScaledHeight_double());
		scene.draw(0, 0);
	}

}
