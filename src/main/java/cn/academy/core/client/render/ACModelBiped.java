/**
 * 
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
    		GL11.glRotated(200, 0, 1, 0);
    		GL11.glRotated(90, 1, 0, 0);
    		GL11.glRotated(180, 0, 1, 0);
    		super.render(par1Entity, par2, par3, par4, par5, par6, par7);
    	} GL11.glPopMatrix();
    }
	
    @Override
	public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity ent) {
    	
    }
}
