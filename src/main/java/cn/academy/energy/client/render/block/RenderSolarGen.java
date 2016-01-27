/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.render.block;

import cn.academy.core.client.Resources;
import cn.lambdalib.multiblock.RenderBlockMultiModel;
import cn.lambdalib.util.deprecated.TileEntityModelCustom;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderSolarGen extends RenderBlockMultiModel {

    public RenderSolarGen() {
        super(new TileEntityModelCustom(Resources.getModel("solar")), 
            Resources.getTexture("models/solar"));
        scale = 0.014;
    }
    
    @Override
    public void drawAtOrigin(TileEntity te) {
        GL11.glRotated(90, 0, 1, 0);
        super.drawAtOrigin(te);
    }

}
