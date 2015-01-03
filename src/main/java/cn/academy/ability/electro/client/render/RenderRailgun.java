/**
 * 
 */
package cn.academy.ability.electro.client.render;

import net.minecraft.util.ResourceLocation;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRayAttn;

/**
 * @author WeathFolD
 *
 */
public class RenderRailgun extends RendererRayAttn {

	/**
	 * @param width
	 * @param tex
	 */
	public RenderRailgun() {
		super(.18D, ACClientProps.TEX_EFF_RAILGUN);
		this.disableLight();
	}

}
