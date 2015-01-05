package cn.academy.core.client.gui.dev;

import java.util.Set;

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
import cn.liutils.api.gui.Widget.Alignment;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.render.TextUtils;
import cn.liutils.util.render.TrueTypeFont;

public class PageMainBase extends Widget {

	public static final float TITLE_CENTER_X = 165.75F, TITLE_CENTER_Y = 4.5F;

	ModelBiped model;
	GuiDeveloper dev;

	public PageMainBase(GuiDeveloper gd) {
		super("main", gd.getGui(),  0, 0, gd.WIDTH, gd.HEIGHT);
		this.setAlignStyle(Alignment.CENTER);
		this.setTexMapping(0, 0, 456, 369);
		this.setTexture(ACClientProps.TEX_GUI_AD_MAIN, 512, 512);
		dev = gd;
		model = new ModelBiped();
		model.isChild = false;
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
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, pname,
				TITLE_CENTER_X, TITLE_CENTER_Y, 12, TrueTypeFont.ALIGN_CENTER);
		
		//Titles
		String str = "Holographic View"; //TODO localization
		TextUtils.drawText(TextUtils.FONT_YAHEI_64, str, 147, 20.5, 10);
		
		str = "User Info";
		TextUtils.drawText(TextUtils.FONT_YAHEI_64, str, 147, 119, 10);
		
		//Misc
		drawUserInfo();

	}
	
	private void drawUserInfo() {
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, "CP.", 148, 153, 8);
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, "PRG.", 148, 167.5, 8);
		
		AbilityData data = AbilityDataMain.getData(dev.user);
		ResourceLocation logo = null;
		if(data.hasLearned()) {
			Category cat = data.getCategory();
			logo = cat.getLogo();
			//Cat and level
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, cat.getDisplayName(), 167.5, 130, 11);
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, data.getLevel().getDisplayName(), 167.5, 140, 8);
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
			TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, ACLangs.notLearned(), 167.5, 130, 10);
		}
		RenderUtils.loadTexture(logo);
		HudUtils.drawRect(148.5, 130.5, 15.5, 15.5);
	}

	private void drawPlayer() {
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		EntityPlayer player = dev.user;
		RenderUtils.loadTexture(RenderUtils.STEVE_TEXTURE);
		float x = 100, y = 100, scale = 2.1F;
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
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