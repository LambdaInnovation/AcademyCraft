package cn.academy.ability.client.render;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.block.RenderBlockMultiModel;
import net.minecraft.tileentity.TileEntity;

public class RenderDeveloperAdvanced extends RenderBlockMultiModel {
	
	public RenderDeveloperAdvanced() {
		super(
			new TileEntityModelCustom(Resources.getModel("developer_advanced")), 
			Resources.getTexture("models/developer_advanced"));
		this.scale = 0.5f;
		this.rotateY = 180f;
	}
	
	@Override
	public void drawAtOrigin(TileEntity te) {
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		super.drawAtOrigin(te);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

}
