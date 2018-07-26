/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.render;

import cn.academy.core.Resources;
import cn.lambdalib2.multiblock.RenderBlockMultiModel;
import cn.lambdalib2.util.deprecated.TileEntityModelCustom;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderDeveloperNormal extends RenderBlockMultiModel {
    
    public RenderDeveloperNormal() {
        super(
            new TileEntityModelCustom(Resources.getModel("developer_normal")), 
            Resources.getTexture("models/developer_normal"));
        this.scale = 0.5f;
        this.rotateY = 180f;
    }
    
    @Override
    public void drawAtOrigin(TileEntity te) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        super.drawAtOrigin(te);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

}
