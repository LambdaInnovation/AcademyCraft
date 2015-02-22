package cn.academy.ability.electro.client.render.entity;

import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.RendererRaySimple;

public class RenderRailgun extends RendererRaySimple<EntityRailgun>{
	public RenderRailgun() {
		super(ACClientProps.TEX_EFF_RAILGUN, 4);
		widthFp = 0.12;
		widthTp = 0.3;
	}
}
