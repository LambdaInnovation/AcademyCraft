package cn.academy.ability.meltdowner.client.render;

import cn.liutils.util.RenderUtils;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class RenderElecDart extends Render {

	@Override
	public void doRender(Entity var1, double var2, double var4, double var6, float var8, float var9) {
		RenderUtils.drawCube(1, 1, 1, true);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}
