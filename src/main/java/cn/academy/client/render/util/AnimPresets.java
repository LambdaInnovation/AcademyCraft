package cn.academy.client.render.util;

import cn.lambdalib2.vis.CompTransform;
import cn.lambdalib2.vis.animation.presets.CompTransformAnim;
import cn.lambdalib2.vis.curve.CubicCurve;
import cn.lambdalib2.vis.curve.IFittedCurve;

public class AnimPresets
{
    public static CompTransformAnim createPrepareAnim()
    {
        CompTransformAnim anim = new CompTransformAnim(new CompTransform());

        anim.animTransform.cy = new CubicCurve();
        IFittedCurve curvey = anim.animTransform.cy;
        curvey.addPoint(0, 0);
        curvey.addPoint(0.5, 0.2);
        curvey.addPoint(1, 0.4);

        anim.animTransform.cx = new CubicCurve();
        IFittedCurve curvex = anim.animTransform.cx;
        curvex.addPoint(0, 0);
        curvex.addPoint(1, -0.02);

        anim.animTransform.cz = new CubicCurve();
        IFittedCurve curvez = anim.animTransform.cz;
        curvez.addPoint(0, 0);
        curvez.addPoint(1, -0.05);

        anim.animRotation.cx = new CubicCurve();
        IFittedCurve curverx = anim.animRotation.cx;
        curverx.addPoint(0, 0);
        curverx.addPoint(1, -20);

        // anim.animRotation.cx.addPoint(0)
        return anim;
    }

    public static CompTransformAnim createPunchAnim()
    {
        CompTransformAnim anim = new CompTransformAnim(new CompTransform());

        anim.animTransform.cy = new CubicCurve();
        IFittedCurve curvey = anim.animTransform.cy;
        curvey.addPoint(0, 0.8);
        curvey.addPoint(0.5, 0.75);
        curvey.addPoint(1, 0);

        anim.animTransform.cx  = new CubicCurve();
        IFittedCurve curvex = anim.animTransform.cx;
        curvex.addPoint(0, -0.04);
        curvex.addPoint(0.5, -0.04);
        curvex.addPoint(1, 0);

        anim.animTransform.cz = new CubicCurve();
        IFittedCurve curvez = anim.animTransform.cz;
        curvez.addPoint(0, -0.0);
        curvez.addPoint(0.3, -0.4);
        curvez.addPoint(1, 0);

        anim.animRotation.cx = new CubicCurve();
        IFittedCurve curverx = anim.animRotation.cx;
        curverx.addPoint(0, -40);
        curverx.addPoint(0.5, -45);
        curverx.addPoint(1, 0);

        anim.animRotation.cy = new CubicCurve();
        IFittedCurve curvery = anim.animRotation.cy;
        curvery.addPoint(0, 0);
        curvery.addPoint(0.3, 10);
        curvery.addPoint(1, 0);

        return anim;
}
}