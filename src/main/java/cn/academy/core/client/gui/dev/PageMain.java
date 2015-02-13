package cn.academy.core.client.gui.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cn.academy.api.ability.Category;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cn.liutils.util.render.TrueTypeFont;

public class PageMain extends Widget {

	public static final float TITLE_CENTER_X = 165.75F, TITLE_CENTER_Y = 4.5F;

	ModelBiped model;
	GuiDeveloper dev;

	public PageMain(GuiDeveloper gd) {
		super(0, 0, gd.WIDTH, gd.HEIGHT);
		this.alignStyle = AlignStyle.CENTER;
		this.initTexDraw(ACClientProps.TEX_GUI_AD_MAIN, 0, 0, 456, 369);
		dev = gd;
		model = new ModelBiped();
		model.isChild = false;
	}
	
	@Override
	public void onAdded() {
		addWidgets(new Widget(88.5, 6.5, 8.5, 7.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				dev.pageID = Math.max(dev.pageID - 1, 0);
				dev.updateVisiblility();
			}
		},
		new Widget(215.5, 6.5, 8.5, 7.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				dev.pageID = Math.min(dev.pageID + 1, dev.subs.size() - 1);
				dev.updateVisiblility();
			}
		});
	}

	@Override
	public void draw(double mx, double my, boolean mouseHovering) {
		// Background
		GL11.glEnable(GL11.GL_BLEND);
		super.draw(mx, my, mouseHovering);
		
		//Player
		GL11.glPushMatrix(); {
			drawPlayer();
		} GL11.glPopMatrix();
		GL11.glColor4f(1, 1, 1, 1);
		
		// Page name
		String pname = dev.getCurPage().getDisplayName();
		RenderUtils.bindColor(dev.DEFAULT_COLOR);
		TextUtils.drawText(GuiDeveloper.FONT, pname,
				TITLE_CENTER_X, TITLE_CENTER_Y, 12, TrueTypeFont.ALIGN_CENTER);
		
		//Titles
		dev.drawText(ACLangs.holoView(), 147, 20.5, 10);
		
		dev.drawText(ACLangs.ad_UserInfo(), 147, 119, 10);
		
		//Misc
		drawUserInfo();
	}
	
	private void drawUserInfo() {
		dev.drawText("CP.", 148, 153, 8);
		dev.drawText("PRG.", 148, 167.5, 8);
		
		AbilityData data = AbilityDataMain.getData(dev.user);
		ResourceLocation logo = null;
		if(data.hasAbility()) {
			Category cat = data.getCategory();
			logo = cat.getLogo();
			//Cat and level
			TextUtils.drawText(GuiDeveloper.FONT, cat.getDisplayName(), 167.5, 130, 11);
			TextUtils.drawText(GuiDeveloper.FONT, data.getLevel().getDisplayName(), 167.5, 140, 8);
			//Progress Bar
			RenderUtils.loadTexture(ACClientProps.TEX_GUI_AD_MAIN);
			//CP
			GL11.glColor4f(1, 1, 1, 1);
			double prog = data.getCurrentCP() / data.getMaxCP();
			HudUtils.drawRect(163, 155F, 3, 372, prog * 58.5, 5.5, prog * 117, 11);
			//Update prog
			prog = 0.5;
			HudUtils.drawRect(163, 168.5F, 3, 387, prog * 58.5, 5.5, prog * 117, 11);
		} else {
			logo = ACClientProps.TEX_QUESTION_MARK;
			TextUtils.drawText(GuiDeveloper.FONT, ACLangs.notLearned(), 167.5, 130, 10);
		}
		RenderUtils.loadTexture(logo);
		HudUtils.drawRect(148.5, 130.5, 15.5, 15.5);
	}

	private void drawPlayer() {
		EntityPlayer player = dev.user;
		RenderUtils.loadTexture(RenderUtils.STEVE_TEXTURE);
		float x = 100, y = 100, scale = 2.1F;
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(true);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glPushMatrix(); {
			GL11.glTranslatef(183, 58, 100F);
			GL11.glScalef((float) (-scale), (float) scale, (float) scale);
			GL11.glRotated(-25, 1, 0, 0);
			RenderHelper.enableStandardItemLighting();
			GL11.glRotatef(Minecraft.getSystemTime() / 100F, 0F, 1F, 0F); //Rotate around Y
			model.render(player, 0, 0, 0, 0, 0, 1F);
		} GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glDepthFunc(GL11.GL_ALWAYS);
	}
}