/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.liutils.api.client.gui.Widget;
import cn.liutils.api.client.util.HudUtils;
import cn.liutils.api.client.util.RenderUtils;

/**
 * Base class of any subpage
 * @author WeathFolD
 */
public abstract class DevSubpage extends Widget {
	
	protected static final float
		PG_OFFSET_X = 3.5F, PG_OFFSET_Y = 34.6F,
		PG_WIDTH = 136.5F, PG_HEIGHT = 146.5F;
	
	protected final GuiDeveloper dev;

	public DevSubpage(GuiDeveloper parent, String s, ResourceLocation back) {
		super(s, parent.getGui(), PG_OFFSET_X, PG_OFFSET_Y, PG_WIDTH, PG_HEIGHT);
		this.dev = parent;
		this.setTexture(back, 512, 512);
		this.setTexMapping(0, 0, 273, 293);
		visible = false;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal(ID);
	}

}
