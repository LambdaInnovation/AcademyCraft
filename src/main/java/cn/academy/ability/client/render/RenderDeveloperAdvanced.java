package cn.academy.ability.client.render;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.block.RenderBlockMultiModel;
import net.minecraft.tileentity.TileEntity;

public class RenderDeveloperAdvanced extends RenderBlockMultiModel {
	
	public RenderDeveloperAdvanced() {
		super(
			new TileEntityModelCustom(Resources.getModel("developer_normal")), 
			Resources.getTexture("models/developer_normal"));
		this.scale = 0.5f;
		
	}
	
	@Override
	public void drawAtOrigin(TileEntity te) {
		GL11.glDisable(GL11.GL_CULL_FACE);
		super.drawAtOrigin(te);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

}
