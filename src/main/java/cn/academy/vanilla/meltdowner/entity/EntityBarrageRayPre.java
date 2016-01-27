/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.generic.MathUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 *
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityBarrageRayPre extends EntityRayBase {
    
    @RegEntity.Render
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
        worldObj.playSound(posX, posY, posZ, "academy:md.ray_small", 0.8f, 1.0f, false);
    }
    
    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        int blendTime = 500;

        if(dt > this.life * 50 - blendTime) {
            return 1 - MathUtils.clampd(1, 0, (double) (dt - (this.life * 50 - blendTime)) / blendTime);
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