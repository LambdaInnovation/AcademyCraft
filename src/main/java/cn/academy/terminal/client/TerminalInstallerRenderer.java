/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.terminal.client;

import cn.academy.core.client.Resources;
import cn.lambdalib.template.client.render.item.RenderModelItem;
import cn.lambdalib.util.deprecated.ItemModelCustom;
import cn.lambdalib.util.generic.VecUtils;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

/**
 * @author WeAthFolD
 */
public class TerminalInstallerRenderer extends RenderModelItem {

    public TerminalInstallerRenderer() {
        super(new ItemModelCustom(Resources.getModel("terminal_installer")), 
                Resources.getTexture("models/terminal_installer"));
        renderEntityItem = true;
        renderInventory = false;
        
        this.scale = 4.0;
        this.equipOffset = VecUtils.vec(0.5, 0.16, -0.24);
        this.thirdPersonOffset = VecUtils.vec(0.2, -0.15, 0.25);
        this.thirdPersonScale = 0.73;
        this.stdRotation.xCoord = 90;
        this.stdRotation.zCoord = 5;
        this.stdOffset.yCoord = 0.0;
        this.entityItemScale = 1.1; 
        this.entityItemOffset.xCoord = -0.1;
        this.entityItemRotation.yCoord = 180;
        this.equipRotation.xCoord = 0;
        this.equipRotation.yCoord = -15;
        this.equipRotation.zCoord = 90;
        this.equipScale = 0.8;
    }
    
    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        super.renderItem(type, item, data);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

}
