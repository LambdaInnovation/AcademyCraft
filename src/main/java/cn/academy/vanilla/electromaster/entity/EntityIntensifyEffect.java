/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.entity;

import cn.academy.vanilla.electromaster.client.effect.SubArc;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.util.entityx.EntityCallback;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
@Registrant
@RegEntity(clientOnly = true)
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
        this.executeAfter(new EntityCallback<EntityIntensifyEffect>() {

            @Override
            public void execute(EntityIntensifyEffect target) {
                //arcHandler.clear();
                int gen = RandUtils.rangei(3, 4);
                while(gen-- > 0) {
                    double phi = RandUtils.ranged(0.5, 0.6);
                    double theta = RandUtils.ranged(0, Math.PI * 2);
                    SubArc arc = arcHandler.generateAt(
                        VecUtils.vec(phi * Math.sin(theta), ht, phi * Math.cos(theta)));
                    arc.life = 3;
                }
            }
            
        }, after);
    }

}
