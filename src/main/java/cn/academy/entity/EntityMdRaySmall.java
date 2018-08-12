package cn.academy.entity;

import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMdRaySmall extends EntityRayBase {

    public static SmallMdRayRender renderer;

    public EntityMdRaySmall(World world) {
        super(world);
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 14;
        this.length = 15.0;
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        ACSounds.playClient(world,posX, posY, posZ, "md.ray_small",SoundCategory.AMBIENT, 0.8f,1.0f);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        Particle p = MdParticleFactory.INSTANCE.next(world,
            new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
            new Vec3d(RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015), RandUtils.ranged(-.015, .015)));
        world.spawnEntity(p);
    }
    
    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        int blendTime = 500;

        if(dt > this.life * 50 - blendTime) {
            double timeFactor = MathUtils.clampd(0, 1, (double) (dt - (this.life * 50 - blendTime)) / blendTime);
            return 1 - timeFactor;
        }
        
        return 1.0;
    }
    
    public static class SmallMdRayRender extends RendererRayComposite {

        public SmallMdRayRender() {
            super("mdray_small");
            this.cylinderIn.width = 0.03;
            this.cylinderIn.color.setColor4i(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.045;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.width = 0.3;
            this.glow.color.a = 0.5;
        }
        
    }

}