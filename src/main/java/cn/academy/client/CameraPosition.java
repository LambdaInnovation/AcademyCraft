package cn.academy.client;

import cn.lambdalib2.registry.StateEventCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * This class stores render camera position in world space each frame.
 */
@SideOnly(Side.CLIENT)
public class CameraPosition {

    @StateEventCallback
    private static void _init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new CameraPosition());
    }

    private CameraPosition() {}

    private static Vector3f result;

    private static final Matrix4f matrix = new Matrix4f();
    private static final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static Vector3f get() {
        if (result == null) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer p = mc.player;
            return new Vector3f((float) p.posX, (float) p.posY, (float) p.posZ);
        } else {
            return new Vector3f(result);
        }
    }

    private static final Vec3d zero = new Vec3d(0, 0, 0);

    public static Vec3d getVec3d() {
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