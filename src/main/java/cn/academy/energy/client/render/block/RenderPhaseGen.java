/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.client.render.block;

import cn.academy.core.client.Resources;
import cn.academy.energy.block.TilePhaseGen;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.generic.MathUtils;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class RenderPhaseGen extends TileEntitySpecialRenderer {
    
    IModelCustom model;
    ResourceLocation[] textures;
    
    public RenderPhaseGen() {
        model = Resources.getModel("ip_gen");
        textures = Resources.getTextureSeq("models/ip_gen", 5);
    }

    @Override
    public void renderTileEntityAt(TileEntity te, 
        double x, double y, double z, float wtf) {
        TilePhaseGen gen = (TilePhaseGen) te;
        
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y, z + 0.5);
        int tid = MathUtils.clampi(0, 4, 
            (int) Math.round(4.0 * gen.getLiquidAmount() / gen.getTankSize()));
        RenderUtils.loadTexture(textures[tid]);
        model.renderAll();
        GL11.glPopMatrix();
    }

}
