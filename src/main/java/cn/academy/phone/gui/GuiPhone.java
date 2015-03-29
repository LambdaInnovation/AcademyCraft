/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * This project is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.phone.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import cn.academy.generic.client.ClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.liutils.api.gui.AuxGui;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.registry.AuxGuiRegistry.RegAuxGui;
import cn.liutils.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
public class GuiPhone extends AuxGui {
    
    @RegAuxGui
    public static GuiPhone instance = new GuiPhone();
    
    LIGui gui; //Forward to the rendering of this gui.
    
    final int MAX_MX = 317, MAX_MY = 512;
    final double SENSITIVITY = 1.0;
    double mouseX, mouseY;
    
    boolean open;
    long lastOpenTime;
    
    ItemStack stack;
    
    public GuiPhone() {
        gui = new LIGui();
        gui.addWidget(new PagePhone());
        this.addKeyHandler("quit", LIKeyProcess.MOUSE_RIGHT, false, new IKeyHandler() {

            @Override
            public void onKeyDown(int keyCode, boolean tickEnd) {
                if(Minecraft.getSystemTime() - lastOpenTime > 500L)
                    close();
            }

            @Override
            public void onKeyUp(int keyCode, boolean tickEnd) {}

            @Override
            public void onKeyTick(int keyCode, boolean tickEnd) {}
            
        });
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isForeground() {
        return true;
    }
    
    public void open(ItemStack _stack) {
        if(Minecraft.getSystemTime() - lastOpenTime < 400)
            return;
        
        open = true;
        lastOpenTime = Minecraft.getSystemTime();
        Minecraft.getMinecraft().mouseHelper = PhoneMouseHelper.instance;
        stack = _stack;
    }
    
    public void close() {
        open = false;
        lastOpenTime = Minecraft.getSystemTime();
        Minecraft.getMinecraft().mouseHelper = PhoneMouseHelper.def;
    }

    @Override
    public void draw(ScaledResolution sr) {
        Minecraft mc = Minecraft.getMinecraft();
        
        mouseX += PhoneMouseHelper.instance.dx * SENSITIVITY;
        mouseY -= PhoneMouseHelper.instance.dy * SENSITIVITY;
        mouseX = Math.max(0, Math.min(MAX_MX, mouseX));
        mouseY = Math.max(0, Math.min(MAX_MY, mouseY));
        
        PhoneMouseHelper.instance.dx = PhoneMouseHelper.instance.dy = 0;
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        float aspect = (float)mc.displayWidth / mc.displayHeight;
        GLU.gluPerspective(60, 
             aspect, 
             0.001f, 100);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4d(1, 1, 1, 1);
        
        GL11.glTranslated(.22 * aspect, 0.77, -1.5);
        RenderUtils.loadTexture(ClientProps.TEX_GUI_PHONE_BACK);
        
        GL11.glTranslated(1, 0, 0);
        GL11.glRotated(-12 + 1 * Math.sin(Minecraft.getSystemTime() / 800.0), 0, 1, 0);
        GL11.glTranslated(-1, 0, 0);
        
        final double scale = 1.0 / 300;
        GL11.glScaled(scale, -scale, scale);
        gui.draw(mouseX, mouseY);
        
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glCullFace(GL11.GL_BACK);
        
        if(!stack.equals(mc.thePlayer.getCurrentEquippedItem()))
            close();
    }

}
