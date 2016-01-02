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

import cn.academy.core.client.component.Glow;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.GameTimer;

/**
 * @author WeAthFolD
 */
public class BlendIn extends Component {

    public int timeOffset = 0;
    public int blendTime = 240;

    double dta, tba, ta, ga;

    public BlendIn() {
        super("BlendIn");

        long time = GameTimer.getTime();
        listen(FrameEvent.class, (w, event) ->
        {
            long delta = GameTimer.getTime() - time + timeOffset;
            double factor = MathUtils.clampd(0, 1, (double) delta / blendTime);
            setAlpha(factor);

            if(factor == 1)
                BlendIn.this.enabled = false;
        });
    }

    @Override
    public void onAdded() {
        super.onAdded();

        DrawTexture dt = DrawTexture.get(widget);
        TextBox tb = TextBox.get(widget);
        Tint t = Tint.get(widget);
        Glow g = Glow.get(widget);
        if(dt != null) dta = dt.color.a;
        if(tb != null) tba = tb.option.color.a;
        if(t != null) ta = t.idleColor.a;
        if(g != null) ga = g.color.a;

        setAlpha(0);
    }

    private void setAlpha(double value) {
        DrawTexture dt = DrawTexture.get(widget);
        TextBox tb = TextBox.get(widget);
        Tint t = Tint.get(widget);
        Glow g = Glow.get(widget);
        if(dt != null) dt.color.a = dta * value;
        if(tb != null) tb.option.color.a = .1 + .9 * tba * value;
        if(t != null) t.idleColor.a = t.hoverColor.a = ta * value;
        if(g != null) g.color.a = ga * value;
    }

}
