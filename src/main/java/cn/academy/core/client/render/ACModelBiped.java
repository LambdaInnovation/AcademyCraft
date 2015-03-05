/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;

import org.lwjgl.opengl.GL11;

/**
 * 强行黑透MC
 * @author WeathFolD
 */
public class ACModelBiped extends ModelBiped {

    public ACModelBiped()
    {
        this(0.0F);
    }

    public ACModelBiped(float par1)
    {
    	super(par1);
    }
	
    @Override
	public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7)
    {
    	GL11.glPushMatrix(); {
    		if(par1Entity == Minecraft.getMinecraft().thePlayer) {
    			GL11.glPopMatrix();
    			return;
    		}
    		//System.out.println(DebugUtils.formatArray(par2, par3, par4, par5, par6, par7));
    		GL11.glTranslated(0, 1, 0);
    		GL11.glRotated(180, 0, 1, 0);
    		GL11.glRotated(90, 1, 0, 0);
    		GL11.glRotated(180, 0, 1, 0);
    		super.render(par1Entity, par2, par3, par4, par5, par6, par7);
    	} GL11.glPopMatrix();
    }
	
    @Override
	public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity ent) {
    	
    }
}
