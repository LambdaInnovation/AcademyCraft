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
package cn.academy.core.client.ui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.ScaledResolution;
import cn.annoreg.core.Registrant;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;

/**
 * AC global HUD drawing dispatcher.
 * TODO: Support position customizing
 * @author WeAthFolD
 */
@Registrant
public class ACHud extends AuxGui {
	
	@RegAuxGui
	public static ACHud instance = new ACHud();
	
	List<Node> nodes = new ArrayList();
	
	LIGui gui = new LIGui();

	ACHud() {}

	@Override
	public boolean isForeground() {
		return false;
	}

	@Override
	public void draw(ScaledResolution sr) {
		gui.resize(sr.getScaledWidth_double(), sr.getScaledHeight_double());
		for(Node n : nodes) {
			n.w.transform.enabled = n.cond.shows();
		}
		
		gui.draw();
	}
	
	public void addElement(Widget w, Condition showCondition) {
		nodes.add(new Node(w, showCondition));
		gui.addWidget(w);
	}
	
	public interface Condition {
		boolean shows();
	}
	
	private class Node {
		Widget w;
		Condition cond;
		
		public Node(Widget wi, Condition c) {
			w = wi;
			cond = c;
		}
	}

}
