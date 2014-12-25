/**
 * 
 */
package cn.academy.ability.electro.client.render;

import net.minecraft.util.ResourceLocation;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRay;

/**
 * 电弧实体的渲染器
 * @author WeathFolD
 *
 */
public class RenderElecArc extends RendererRay {

	static final double WIDTH = 0.4;
	
	public RenderElecArc() {
		super(WIDTH, ACClientProps.ANIM_ARC_LONG);
		this.setRatio(4);
		this.enableLight = false;
	}

}
