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

import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.helper.GameTimer;

/**
 * @author WeAthFolD
 */
public class BlendIn extends Component {
	
	public int timeOffset = 0;
	public int blendTime = 300;

	private double targetAlpha = 1.0;

	public BlendIn() {
		super("BlendIn");
	}
	
	@Override
	public void onAdded() {
		targetAlpha = getAlpha();
		
		long time = GameTimer.getTime();
		widget.regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				long dt = GameTimer.getTime() - time + timeOffset;
				double factor = MathUtils.wrapd(0, 1, (double) dt / blendTime);
				setAlpha(factor);
			}
			
		});
	}
	
	private double getAlpha() {
		DrawTexture dt = DrawTexture.get(widget);
		if(dt != null) return dt.color.a;
		else return TextBox.get(widget).color.a;
	}
	
	private void setAlpha(double value) {
		DrawTexture dt = DrawTexture.get(widget);
		TextBox tb = TextBox.get(widget);
		if(dt != null) dt.color.a = value;
		if(tb != null) tb.color.a = value;
	}

}
