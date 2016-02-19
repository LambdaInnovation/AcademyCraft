/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.generic.entity;

import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.template.client.render.entity.RenderIcon;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityBloodSplash extends EntityAdvanced {

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
            super.doRender(entity, x, y, z, a, b);
        }

    }

}
