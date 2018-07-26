package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMDRay extends EntityRayBase {

    public static MDRayRender renderer;
    
    public EntityMDRay(EntityPlayer _player, double length) {
        this(_player, new Motion3D(_player, true), length);
    }

    public EntityMDRay(EntityPlayer spawner, Motion3D mo, double length) {
        super(spawner);

        Vec3 start = mo.getPosVec(), end = mo.move(length).getPosVec();
        this.setFromTo(start, end);
        this.blendInTime = 200;
        this.blendOutTime = 700;
        this.life = 50;
        this.length = length;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        if(RandUtils.nextDouble() < 0.8) {
            Particle p = MdParticleFactory.INSTANCE.next(worldObj,
                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            worldObj.spawnEntityInWorld(p);
        }
    }
    
    public static class MDRayRender extends RendererRayComposite {

        public MDRayRender() {
        super("mdray");
            this.cylinderIn.width = 0.17;
            this.cylinderIn.color.setColor4i(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.22;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.width = 1.5;
            this.glow.color.a = 0.8;
        }
        
    }
}