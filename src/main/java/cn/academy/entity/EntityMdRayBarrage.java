package cn.academy.entity;

import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This class uses some little hacks. By rendering all the barrage rays within a 
 * single render we avoid a fair amount of perfomance overheads.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityMdRayBarrage extends EntityRayBase {

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
        ACSounds.playClient(world,posX, posY, posZ, "md.ray_small",SoundCategory.AMBIENT, 0.5f,1.0f);
    }
    
    @Override
    public boolean needsViewOptimize() {
        return false;
    }

    @RegEntityRender(EntityMdRayBarrage.class)
    public static class BarrageRenderer extends EntityMdRaySmall.SmallMdRayRender {

        public BarrageRenderer(RenderManager manager) {
            super(manager);
        }

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