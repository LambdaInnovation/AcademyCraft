package cn.academy.client.render.item;

import cn.academy.Resources;
import cn.lambdalib2.template.client.render.item.RenderModelItem;
import cn.lambdalib2.util.deprecated.ItemModelCustom;
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
        this.equipOffset = new Vec3d(0.5, 0.16, -0.24);
        this.thirdPersonOffset = new Vec3d(0.2, -0.15, 0.25);
        this.thirdPersonScale = 0.73;
        this.stdRotation.x = 90;
        this.stdRotation.z = 5;
        this.stdOffset.y = 0.0;
        this.entityItemScale = 1.1; 
        this.entityItemOffset.x = -0.1;
        this.entityItemRotation.y = 180;
        this.equipRotation.x = 0;
        this.equipRotation.y = -15;
        this.equipRotation.z = 90;
        this.equipScale = 0.8;
    }
    
    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        GL11.glDisable(GL11.GL_CULL_FACE);
        super.renderItem(type, item, data);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

}