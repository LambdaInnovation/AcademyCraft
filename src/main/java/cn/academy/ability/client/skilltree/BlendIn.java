/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
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
