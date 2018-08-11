package cn.academy.vanilla.generic.entity;

import cn.academy.core.Resources;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static org.lwjgl.opengl.GL11.glDepthMask;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityBloodSplash extends EntityAdvanced
{

    static ResourceLocation[] SPLASH = Resources.getEffectSeq("blood_splash", 10);

    @RegEntity.Render
    public static SplashRenderer render;

    int frame;

    public EntityBloodSplash(World world) {
        super(world);
        ignoreFrustumCheck = true;
        setSize(RandUtils.rangef(0.8f, 1.3f));
    }

    public void setSize(float size) {
        this.width = this.height = size;
    }

    public float getSize() {
        return this.width;
    }

    @Override
    public void onUpdate() {
        if (++frame == SPLASH.length) {
            setDead();
        }
        super.onUpdate();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public static class SplashRenderer extends RenderIcon {

        public SplashRenderer() {
            super(null);
            setSize(1.0f);
            this.color.setColor4i(213, 29, 29, 200);
        }

        @Override
        public void doRender(Entity entity, double x, double y, double z, float a, float b) {
            EntityBloodSplash splash = (EntityBloodSplash) entity;
            icon = (SPLASH[MathUtils.clampi(0, SPLASH.length - 1, splash.frame)]);
            this.size = splash.getSize();

            glDepthMask(false);
            super.doRender(entity, x, y, z, a, b);
            glDepthMask(true);
        }

    }

}