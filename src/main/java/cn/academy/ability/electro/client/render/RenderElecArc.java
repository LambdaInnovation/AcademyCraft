/**
 * 
 */
package cn.academy.ability.electro.client.render;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.entity.EntityArcBase;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;

/**
 * @author WeathFolD
 *
 */
public class RenderElecArc extends RendererRayTiling<EntityArcBase> {

	public RenderElecArc() {
		super(null);
		this.ratio = 6;
		this.width = 0.9;
	}
	
	@Override
	protected void drawAtOrigin(EntityArcBase ent) {
			super.drawAtOrigin(ent);
	}
	
	@Override
	protected ResourceLocation nextTexture(EntityArcBase ent, int i) {
		this.fpOffsetZ = -0.2;
		this.fpOffsetX = -0.3;
		this.fpOffsetY = -0.1;
		return ACClientProps.ANIM_ELEC_ARC[ent.getIndex(i)];
	}
	
	@Override
	protected void drawPerBillboard(EntityArcBase ent, int i) {
		GL11.glRotated(ent.getRotation(i), 0, 0, 1);
	}

}
