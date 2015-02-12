/**
 * 
 */
package cn.academy.misc.client.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.misc.block.energy.BlockNode;
import cn.academy.misc.block.energy.container.ContainerNode;
import cn.academy.misc.block.energy.tile.impl.TileNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.gui.GuiHandlerBase;
import cn.annoreg.mc.gui.RegGuiHandler;
import cn.liutils.api.gui.LIGuiContainer;
import cn.liutils.api.gui.Widget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
public class GuiNode extends LIGuiContainer {
	
	ResourceLocation TEX = new ResourceLocation("academy:textures/guis/wireless_node.png");

	public GuiNode(ContainerNode c) {
		super(c);
		gui.addWidget(new Page());
	}
	
	private class Page extends Widget {
		public Page() {
			this.alignStyle = AlignStyle.CENTER;
			this.setSize(155, 150);
			this.initTexDraw(TEX, 14, 2, 310, 299);
			this.setTexResolution(512, 512);
		}
	}
	
}
