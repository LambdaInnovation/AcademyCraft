/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.entity.EntityRayBase;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * This class uses some little hacks. By rendering all the barrage rays within a 
 * single render we avoid a fair amount of perfomance overheads.
 * @author WeAthFolD
 */
@Registrant
@SideOnly(Side.CLIENT)
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMdRayBarrage extends EntityRayBase {
    
    @RegEntity.Render
    public static BarrageRenderer renderer;
    
    private SubRay[] subrays;

    public EntityMdRayBarrage(World world, double x, double y, double z, float yaw, float pitch) {
        super(world);
        
        setPosition(x, y, z);
        rotationYaw = yaw;
        rotationPitch = pitch;
        
        this.life = 50;
        
        // Init the subrays
        final float range = RandUtils.rangef(50, 60);
        int max = RandUtils.rangei(25, 30);
        
        subrays = new SubRay[max];
        for(int i = 0; i < max; ++i)
            subrays[i] = new SubRay(range);
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        worldObj.playSound(posX, posY, posZ, "academy:md.ray_small", 0.5f, 1.0f, false);
    }
    
    @Override
    public boolean needsViewOptimize() {
        return false;
    }
    
    public static class BarrageRenderer extends EntityMdRaySmall.SmallMdRayRender {

        public BarrageRenderer() {}
        
        @Override
        public void doRender(Entity ent, double x,
                double y, double z, float a, float b) {
            EntityMdRayBarrage ray = (EntityMdRayBarrage) ent;
            ray.onRenderTick();
            
            float rYaw = ent.rotationYaw, rPitch = ent.rotationPitch;
            
            for(SubRay sr : ray.subrays) {
                ent.rotationYaw = rYaw + sr.yawOffset;
                ent.rotationPitch = rPitch + sr.pitchOffset;
                this.plainDoRender(ent, x, y, z, a, b);
            }
            
            ent.rotationYaw = rYaw;
            ent.rotationPitch = rPitch;
        }
        
    }
    
    private class SubRay {
        
        float yawOffset;
        float pitchOffset;
        
        public SubRay(float max) {
            yawOffset = RandUtils.rangef(-max, max);
            pitchOffset = RandUtils.rangef(-max / 2, max / 2);    
        }
        
    }

}
