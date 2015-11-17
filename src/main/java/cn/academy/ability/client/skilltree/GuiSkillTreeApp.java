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
package cn.academy.ability.client.skilltree;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.ability.develop.DeveloperType;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.util.helper.Color;

/**
 * @author WeAthFolD
 */
public class GuiSkillTreeApp extends GuiSkillTree {
	
	static Color
		COLOR_PROG_MONO0 = new Color().setColor4i(106, 106, 106, 255),
		COLOR_PROG_NONO1 = new Color().setColor4i(127, 127, 127, 255);

	public GuiSkillTreeApp(EntityPlayer _player) {
		super(_player, DeveloperType.PORTABLE, true);
		initAppPage();
	}
	
	private void initAppPage() {
		ProgressBar p = ProgressBar.get(windowMachine.getWidget("p_energy"));
		p.color = DrawTexture.get(windowMachine.getWidget("i_energy")).color 
			= COLOR_PROG_MONO0;
		p.fluctRegion = 0;
		
		p = ProgressBar.get(windowMachine.getWidget("p_syncrate"));
		p.color = DrawTexture.get(windowMachine.getWidget("i_syncrate")).color
			= COLOR_PROG_NONO1;
		p.fluctRegion = 0;
	}
	
	@Override
	protected Widget createDesc(SkillHandler handler) {
		WidgetSkillDesc ret = new WidgetSkillDesc(handler);
		ret.addWidget(new GuiSkillTree.SkillLevelDesc(handler.skill));
		ret.addWidget(new GuiSkillTree.SkillHint(ret));
		return ret;
	}

}
