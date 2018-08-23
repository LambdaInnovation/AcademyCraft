package cn.academy.entity;

import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMineRayBasic extends EntityRayBase {

    public static BasicMineRayRender renderer;
    
    public EntityMineRayBasic(EntityPlayer _player) {
        super(_player);
        
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 233333;
        this.length = 15.0;
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        EntityPlayer player = getPlayer();
        Vec3d end = new Motion3D(player, true).move(15).getPosVec();
        this.setFromTo(player.posX, player.posY + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), player.posZ, end.x, end.y, end.z);
        if(RandUtils.nextDouble() < 0.5) {
            Particle p = MdParticleFactory.INSTANCE.next(world,
                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    new Vec3d(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            world.spawnEntity(p);
        }
    }
    
    public static class BasicMineRayRender extends RendererRayComposite {

        public BasicMineRayRender() {
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