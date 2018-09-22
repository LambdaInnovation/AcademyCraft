package cn.academy.entity;

import cn.academy.client.render.util.SubArc;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityCallback;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityIntensifyEffect extends EntitySurroundArc {

    public EntityIntensifyEffect(EntityPlayer player) {
        super(player);
        
        this.setArcType(ArcType.THIN);
        initEvents();
    }
    
    private void initEvents() {
        genAtHt(2, 0);
        genAtHt(1.8, 1);
        genAtHt(1.5, 3);
        genAtHt(1, 4);
        genAtHt(0.5, 6);
        genAtHt(0, 7);
        genAtHt(-0.1, 8);
        
        this.life = 15;
    }
    
    // Disable the original generation
    @Override
    protected void doGenerate() {}
    
    private void genAtHt(double ht, int after) {
        this.executeAfter((EntityCallback<EntityIntensifyEffect>) target -> {
            //arcHandler.clear();
            int gen = RandUtils.rangei(3, 4);
            while(gen-- > 0) {
                double phi = RandUtils.ranged(0.5, 0.6);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                SubArc arc = arcHandler.generateAt(
                    new Vec3d(phi * Math.sin(theta), ht, phi * Math.cos(theta)));
                arc.life = 3;
            }
        }, after);
    }

}