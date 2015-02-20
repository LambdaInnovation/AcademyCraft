/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.space.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityMagHook extends EntityX {
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static HookRender renderer;
	
	public EntityMagHook(EntityPlayer player) {
		super(player.worldObj);
		new Motion3D(player, true).applyToEntity(this);
	}

	@SideOnly(Side.CLIENT)
	public EntityMagHook(World world) {
		super(world);
	}
	
	@SideOnly(Side.CLIENT)
	public static class HookRender extends Render {
		
		final IModelCustom
			model = ACClientProps.MDL_MAGHOOK,
			model_open = ACClientProps.MDL_MAGHOOK_OPEN;

		@Override
		public void doRender(Entity ent, double x, double y,
				double z, float a, float b) {
			EntityMagHook hook = (EntityMagHook) ent;
			GL11.glPushMatrix();
			RenderUtils.loadTexture(ACClientProps.TEX_MDL_MAGHOOK);
			GL11.glTranslated(x, y, z);
			GL11.glRotated(-hook.rotationYaw + 90, 0, 1, 0);
			GL11.glRotated(hook.rotationPitch - 90, 0, 0, 1);
			double scale = 0.007;
			GL11.glScaled(scale, scale, scale);
			model.renderAll();
			GL11.glPopMatrix();
		}

		@Override
		protected ResourceLocation getEntityTexture(Entity var1) {
			return null;
		}
		
	}

}
