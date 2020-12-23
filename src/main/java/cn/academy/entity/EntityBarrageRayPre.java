package cn.academy.entity;

import cn.academy.client.render.entity.ray.RendererRayComposite;
import cn.academy.client.sound.ACSounds;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.registry.mc.RegEntityRender;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@SideOnly(Side.CLIENT)
public class EntityBarrageRayPre extends EntityRayBase {

    public EntityBarrageRayPre(World world, boolean hit) {
        super(world);
        
        this.blendInTime = 200;
        this.blendOutTime = 400;
        this.life = hit ? 50 : 30;
        this.length = 15.0;
    }
    
    @Override
    protected void onFirstUpdate() {
        super.onFirstUpdate();
        ACSounds.playClient(world,posX, posY, posZ, "md.ray_small",SoundCategory.AMBIENT, 0.8f,1.0f);
    }
    
    @Override
    public double getWidth() {
        double dt = getDeltaTime();
        int blendTime = 500;

        if(dt > this.life * 50 - blendTime) {
            return 1 - MathUtils.clampd(1, 0, (dt - (this.life * 50 - blendTime)) / blendTime);
        }
        
        return 1.0;
    }

    @RegEntityRender(EntityBarrageRayPre.class)
    public static class BRPRender extends RendererRayComposite {

        public BRPRender(RenderManager manager) {
            super(manager, "mdray_small");
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.set(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.052;
            this.cylinderOut.color.set(106, 242, 106, 50);
            
            this.glow.width = 0.4;
            this.glow.color.setAlpha(Colors.f2i(0.5f));
        }
        
    }

}