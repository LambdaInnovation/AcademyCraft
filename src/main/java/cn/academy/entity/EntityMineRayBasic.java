package cn.academy.entity;

import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityMineRayBasic extends EntityRayBase {

    public EntityMineRayBasic(EntityPlayer _player) {
        super(_player);

        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = 233333;
        this.length = 15.0;

        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        updatePos();
        EntityPlayer player = getPlayer();
        if(RandUtils.nextDouble() < 0.5) {
            Particle p = MdParticleFactory.INSTANCE.next(world,
//                    new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
                    VecUtils.lookingPos(player, RandUtils.ranged(0, 10)),
                    new Vec3d(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
            world.spawnEntity(p);
        }
    }

    private void updatePos() {
        EntityPlayer player = getPlayer();
        Vec3d end = VecUtils.lookingPos(player, 15);
        this.setFromTo(player.posX, player.posY + player.eyeHeight, player.posZ, end.x, end.y, end.z);
    }

    @RegEntityRender(EntityMineRayBasic.class)
    public static class BasicMineRayRender extends RendererRayComposite {

        public BasicMineRayRender(RenderManager manager) {
            super(manager, "mdray_small");
            this.cylinderIn.width = 0.03;
            this.cylinderIn.color.set(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.045;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.width = 0.3;
            this.glow.color.setAlpha(Colors.f2i(0.5f));
        }
        
    }
}