package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.entity.EntityRayBase;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityBarrageRayPre extends EntityRayBase {

    public static BRPRender renderer;

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
    
    public static class BRPRender extends RendererRayComposite {

        public BRPRender() {
            super("mdray_small");
            this.cylinderIn.width = 0.045;
            this.cylinderIn.color.setColor4i(216, 248, 216, 230);
            
            this.cylinderOut.width = 0.052;
            this.cylinderOut.color.setColor4i(106, 242, 106, 50);
            
            this.glow.width = 0.4;
            this.glow.color.a = 0.5;
        }
        
    }

}