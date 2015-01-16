/**
 * 
 */
package cn.academy.ability.electro.client.render;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.entity.EntityElecArc;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayTiling;

/**
 * @author WeathFolD
 *
 */
public class RenderElecArc extends RendererRayTiling<EntityElecArc> {

	public RenderElecArc() {
		super(null);
		this.ratio = 5;
		this.width = 1.6;
	}
	
	@Override
	protected void drawAtOrigin(EntityElecArc ent) {
		if(ent.isDrawing) {
			super.drawAtOrigin(ent);
		}
	}
	
	@Override
	protected ResourceLocation nextTexture(EntityElecArc ent, int i) {
		this.fpOffsetZ = -0.2;
		this.fpOffsetX = -0.3;
		this.fpOffsetY = -0.1;
		return ACClientProps.ANIM_ELEC_ARC[ent.getIndex(i)];
	}

}
