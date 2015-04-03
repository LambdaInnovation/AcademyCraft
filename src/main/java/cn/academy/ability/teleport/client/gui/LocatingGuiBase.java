package cn.academy.ability.teleport.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.teleport.data.LocationData;
import cn.academy.ability.teleport.skill.SkillLocatingTele;
import cn.academy.misc.util.ACUtils;
import cn.liutils.api.gui.LIGui;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.FocusedVList;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class LocatingGuiBase extends LIGui {
	protected EntityPlayer player;
	protected LocationData data;
	public BaseScreen mainScreen;
	private ResourceLocation tex;
	private final int[] textColor;
	
	protected final String name;
	
	protected static final int[] color = { 178, 178, 178, 180 };
	
	public class GList extends FocusedVList {
		public GList() {
			super("list", 175, 40, 198, 138);
		}
		
		@Override
		public void onAdded() {
			LocationData data = LocationData.get(Minecraft.getMinecraft().thePlayer);
			for(int i = 0; i < data.getLocCount(); ++i) {
				addWidget(new ListElem(data.getLocation(i), i));
			}
		}
		
		public class ListElem extends Widget {
			public final LocationData.Location data;
			public final int n;
			
			public ListElem(LocationData.Location _data, int _n) {
				data = _data;
				n = _n;
				setSize(198, 25);
			}
			
			@Override
			public void draw(double mx, double my, boolean hov) {
				if(getGui().getFocus() == this) {
					RenderUtils.bindColor(color);
					HudUtils.drawModalRect(0, 0, width, height);
				} else if(hov) {
					RenderUtils.bindColor(178, 178, 178, 60);
					HudUtils.drawModalRect(0, 0, width, height);
				}
				RenderUtils.bindColor(textColor);
				ACUtils.drawText(data.name, 5, 5, 14, 180);
			}
			
			@Override
			public boolean doesNeedFocus() {
				return true;
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				GList.this.setFocus(n);
			}
		}
	}
	
	public LocatingGuiBase(String _name, ResourceLocation _tex, int[] texColor) {
		name = _name;
		tex = _tex;
		textColor = texColor;
	}
	
	protected void init() {
		boolean first = mainScreen == null;
		player = Minecraft.getMinecraft().thePlayer;
		data = LocationData.get(player);
		
		if(!first)
			this.clear();
		long time = first ? Minecraft.getSystemTime() : mainScreen.createTime;
		addWidget(mainScreen = new BaseScreen(time));
		mainScreen.addWidget(new GList());
	}
	
	public abstract String getHint();
	
	public class BaseScreen extends Widget {
		
		long createTime;
		
		public BaseScreen(long time) {
			super(413, 219);
			this.alignStyle = AlignStyle.CENTER;
			createTime = time;
			
			this.scale = SkillLocatingTele.scale;
		}
		
		double[][] offsetData = { //radx, rady, dphase, speed divisor
			{ 50, 30, Math.PI, -6500},
			{ 60, 60, 0, 9000},
			{ 60, 30, Math.PI * .5, -6200},
			{ 30, 60, Math.PI * .25, 5800}
		};
		
		@Override
		public void draw(double mx, double my, boolean hov) {
			long dt = Minecraft.getSystemTime() - createTime;
			double mAlpha = Math.min(1.0, dt / 800.0);
			
			RenderUtils.loadTexture(tex);
			
			GL11.glColor4d(1, 1, 1, mAlpha);
			for(double[] off : offsetData) {
				double t = Minecraft.getSystemTime() / off[3];
				double x = off[0] * Math.sin(t - off[2]), y = off[1] * Math.cos(t - off[2]);
				drawOneShadow(x, y);
			}
			drawMainWindow();
			
			RenderUtils.bindColor(textColor);
			ACUtils.drawText(getHint(), 45, 48, 15);
			
			RenderUtils.bindColor(200, 200, 200, 230);
			ACUtils.drawText(name, 20, -18, 18);
		}
		
		private void drawOneShadow(double ox, double oy) {
			GL11.glPushMatrix();
			rect(ox, oy, 0, 219, 413, 219);
			GL11.glPopMatrix();
		}
		
		private void drawMainWindow() {
			rect(0, 0, 0, 0, 413, 219);
		}
		
		protected void rect(double x, double y, double u, double v, double w2, double h2) {
			HudUtils.drawRect(x, y, u, v, w2, h2, w2, h2);
		}
	}
}
