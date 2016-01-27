/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.client.render.ray;

import cn.academy.core.client.render.RendererList;
import cn.academy.core.entity.IRay;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
public class RendererRayComposite extends RendererList {
    
    public RendererRayGlow glow;
    public RendererRayCylinder cylinderIn, cylinderOut;
    
    public RendererRayComposite(String name) {
        append(glow = RendererRayGlow.createFromName(name));
        append(cylinderIn = new RendererRayCylinder(0.05f));
        append(cylinderOut = new RendererRayCylinder(0.08f));
        cylinderIn.headFix = 0.98;
    }
    
    @Override
    public void doRender(Entity ent, double x,
            double y, double z, float a, float b) {
        ((IRay)ent).onRenderTick();
        super.doRender(ent, x, y, z, a, b);
    }
    
    public void plainDoRender(Entity ent, double x,
            double y, double z, float a, float b) {
        super.doRender(ent, x, y, z, a, b);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
        return null;
    }

}
