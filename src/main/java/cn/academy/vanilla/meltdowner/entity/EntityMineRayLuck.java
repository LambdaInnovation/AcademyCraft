package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.Resources;
import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMineRayLuck extends EntityRayBase {
    
    @RegEntity.Render
    public static LuckRayRender renderer;
    
    static final ResourceLocation texture = Resources.getTexture("effects/md_particle_luck");
    
    public EntityMineRayLuck(EntityPlayer _player) {
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
        this.setFromTo(player.posX, player.posY + (ACRenderingHelper.isThePlayer(player) ? -0.15 : 1.55), player.posZ, end.xCoord, end.yCoord, end.zCoord);
        
        if(RandUtils.nextDouble() < 0.6) {
            Particle p = MdParticleFactory.INSTANCE.next(worldObj,
                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            p.texture = texture;
            worldObj.spawnEntityInWorld(p);
        }
    }
    
    public static class LuckRayRender extends RendererRayComposite {

        public LuckRayRender() {
        super("mdray_luck");
            this.cylinderIn.width = 0.04;
            this.cylinderIn.color.setColor4i(241, 229, 247, 230);
            
            this.cylinderOut.width = 0.05;
            this.cylinderOut.color.setColor4i(205, 166, 232, 50);
            
            this.glow.width = 0.45;
            this.glow.color.a = 0.6;
        }
        
    }
}