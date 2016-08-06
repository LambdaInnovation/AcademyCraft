package cn.academy.core.client.util;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * This class stores render camera position in world space each frame.
 */
@SideOnly(Side.CLIENT)
@Registrant
public class CameraPosition {

    @RegInitCallback
    private static void _init() {
        MinecraftForge.EVENT_BUS.register(new CameraPosition());
    }

    private CameraPosition() {}

    private static Vector3f result;

    private static final Matrix4f matrix = new Matrix4f();
    private static final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static Vector3f get() {
        if (result == null) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer p = mc.thePlayer;
            return new Vector3f((float) p.posX, (float) p.posY, (float) p.posZ);
        } else {
            return new Vector3f(result);
        }
    }

    private static final Vec3 zero = Vec3.createVectorHelper(0, 0, 0);

    public static Vec3 getVec3() {
        return zero;
    }

    @SubscribeEvent
    public void onRender(RenderWorldLastEvent evt) {
        // update result
        floatBuffer.clear();

        glGetFloat(GL_MODELVIEW_MATRIX, floatBuffer);
        matrix.load(floatBuffer);

        matrix.invert();

        if (result == null) {
            result = new Vector3f();
        }

        result.set(matrix.m30, matrix.m31, matrix.m32);
    }

}
