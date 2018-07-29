package cn.academy.vanilla.utils;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public class Vec
{
    public static Vec3d rotateAroundZ(Vec3d v, float p_72446_1_)
    {
        float f1 = MathHelper.cos(p_72446_1_);
        float f2 = MathHelper.sin(p_72446_1_);
        double d0 = v.x * (double)f1 + v.y * (double)f2;
        double d1 = v.y * (double)f1 - v.x * (double)f2;
        double d2 = v.z;
        return new Vec3d(d0, d1, d2);
    }
}
