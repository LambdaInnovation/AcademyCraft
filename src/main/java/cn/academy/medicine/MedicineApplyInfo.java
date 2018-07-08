package cn.academy.medicine;


public class MedicineApplyInfo{
    public Properties.Target target;
    public Properties.Strength strengthType;
    public float strengthModifier;
    public Properties.ApplyMethod method;
    public float sensitiveRatio;

    public MedicineApplyInfo(Properties.Target target, Properties.Strength strengthType, float strengthModifier,
                             Properties.ApplyMethod method, float sensitiveRatio)
    {
        this.target=target;
        this.strengthType=strengthType;
        this.strengthModifier=strengthModifier;
        this.method=method;
        this.sensitiveRatio=sensitiveRatio;

        float[] hsb = toHSB(target.baseColor);
        hsb[2] = Math.min(1f, strengthModifier * 0.6666f);
        displayColor=fromHSB(hsb);
    }

    private org.lwjgl.util.Color c2l(cn.lambdalib.util.helper.Color x){
        return new org.lwjgl.util.Color(
                (byte)(x.r*255), (byte)(x.g*255),
                (byte)(x.b*255), (byte)(x.a*255));
    }

    private cn.lambdalib.util.helper.Color l2c(org.lwjgl.util.Color x){
        return new cn.lambdalib.util.helper.Color(x.getRed()/255.0f, x.getGreen()/255.0f,
                x.getBlue()/255.0f, x.getAlpha()/255.0f);
    }


    private float[] toHSB(cn.lambdalib.util.helper.Color color){
        org.lwjgl.util.Color lwjColor = c2l(color);
        float[] arr = lwjColor.toHSB(null);
        return arr;
    }

    private cn.lambdalib.util.helper.Color fromHSB(float[] hsb){
        org.lwjgl.util.Color ret = new org.lwjgl.util.Color();
        ret.fromHSB(hsb[0], hsb[1], hsb[2]);
        return l2c(ret);
    }

    cn.lambdalib.util.helper.Color displayColor;
}