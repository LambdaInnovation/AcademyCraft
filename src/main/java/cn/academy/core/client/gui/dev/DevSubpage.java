/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.liutils.api.client.gui.LIGuiPage;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;

/**
 * @author WeathFolD
 *
 */
public abstract class DevSubpage extends LIGuiPage {
	
	protected static final float
		PG_OFFSET_X = 3.5F, PG_OFFSET_Y = 34.6F,
		PG_WIDTH = 136.5F, PG_HEIGHT = 146.5F;
	
	private String name;
	private final ResourceLocation back;

	public DevSubpage(GuiScreen parent, String s, ResourceLocation back) {
		super(parent, s, PG_OFFSET_X, PG_OFFSET_Y);
		name = s;
		this.back = back;
	}
	
	@Override
	public void drawPage() {
		RenderUtils.loadTexture(back);
		GL11.glEnable(GL11.GL_BLEND);
		HudUtils.drawTexturedModalRect(0, 0, 0, 0, PG_WIDTH, PG_HEIGHT, 273, 293);
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal(name);
	}

}
