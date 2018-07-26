package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMineRayExpert extends EntityRayBase {

    public static ExpertRayRenderer renderer;
    
    public EntityMineRayExpert(EntityPlayer _player) {
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
        Vec3 end = new Motion3D(player, true).move(15).getPosVec();
        this.setFromTo(player.posX, player.posY + (ACRenderingHelper.isThePlayer(player) ? 0.15 : 1.75), player.posZ, end.xCoord, end.yCoord, end.zCoord);
        if(RandUtils.nextDouble() < 0.6) {
            Particle p = MdParticleFactory.INSTANCE.next(worldObj,
                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            worldObj.spawnEntityInWorld(p);
        }
    }
    
    public static class ExpertRayRenderer extends RendererRayComposite {

        public ExpertRayRenderer() {
            super("mdray_expert");
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.setColor4i(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.056;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.width = 0.5;
            this.glow.color.a = 0.7;
        }
        
        @Override
        public void doRender(Entity ent, double x,
                double y, double z, float a, float b) {
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.setColor4i(216, 248, 216, 180);
            
            this.cylinderOut.width = 0.056;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.color.a = 0.5;
            super.doRender(ent, x, y, z, a ,b);
        }
        
    }
}