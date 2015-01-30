/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;

/**
 * Base class of any subpage
 * @author WeathFolD
 */
public abstract class DevSubpage extends Widget {
	
	protected static final float
		PG_OFFSET_X = 3.5F, PG_OFFSET_Y = 34.6F,
		PG_WIDTH = 136.5F, PG_HEIGHT = 146.5F;
	
	protected final GuiDeveloper base;
	protected final String name;

	public DevSubpage(GuiDeveloper gd, String name, ResourceLocation back) {
		super(PG_OFFSET_X, PG_OFFSET_Y, PG_WIDTH, PG_HEIGHT);
		this.base = gd;
		this.name = name;
		this.initTexDraw(back, 0, 0, 273, 293);
		doesDraw = false;
	}
	
	public String getDisplayName() {
		return StatCollector.translateToLocal(name);
	}

}
