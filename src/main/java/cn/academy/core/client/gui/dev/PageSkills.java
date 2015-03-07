/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under  
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.core.client.gui.dev;

import javax.vecmath.Vector2d;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.util.ACUtils;
import cn.liutils.api.draw.prop.AssignColor;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.gui.LIGui.WidgetNode;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.DragBar;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.Pair;

/**
 * @author WeathFolD
 *
 */
public class PageSkills extends DevSubpage {
	
	SkillList sl;
	Bar bar;
	
	private static class HoverEffect extends Widget {
		static final int MAXCHARS = 60;
		static final float LW = 1;
		int[] color = new int[] { 28, 28, 28 , 255};
		static final float FONT_SIZE = 5F;
		
		long lastDeac = 0;
		
		@Override
		public void draw(double mx, double my, boolean hovering) {
			mx = getGui().mouseX;
			my = getGui().mouseY;
			this.posX = mx + 5;
			this.posY = my + 5;
			this.updatePos();
			
			long time = Minecraft.getSystemTime();
			
			WidgetNode top = getGui().getTopNode(mx, my);
			if(top != null && top.widget instanceof SkillList.SkillElement) {
				String str = ((SkillList.SkillElement)top.widget).skill.getDescription();
				
				int alpha = (int) (200 * Math.min((time - lastDeac) / 400.0, 1.0));
				color[3] = alpha;

				//Calc the maxium window length.
				final double SIDE = 10, TOP = 5;
				RenderUtils.bindColor(65, 163, 220, alpha);
				Vector2d rect = GuiDeveloper.FONT.simDrawLinebreak(str, SIDE, TOP, 6, 140);
				
				GL11.glDisable(GL11.GL_TEXTURE_2D);
				//rect
				double w = rect.x + SIDE * 2, h = rect.y + TOP * 2;
				RenderUtils.bindColor(color);
				HudUtils.drawModalRect(0, 0, w, h);
				
				//outline
				RenderUtils.bindColor(65, 163, 220, alpha);
				HudUtils.drawRectOutline(0, 0, w, h, LW);
				GL11.glEnable(GL11.GL_TEXTURE_2D);
				
				//really draw
				GuiDeveloper.FONT.drawLinebreak(str, SIDE, TOP, 6, 140);
				
				//description
			} else{
				lastDeac = time;
			}
		}
		
		@Override
		public int getDrawPriority() {
			return 5;
		}
	}
	
	class SkillList extends ListVertical {
		
		class SkillElement extends Widget {
			
			final int skillID;
			final SkillBase skill;
			
			//Inferred data
			final boolean learned;
			final boolean fullyLearned;
			final boolean canUpgrade;
			final int level;
			final int expectedExp;
			final int expectedEnergy;
			

			public SkillElement(int id) {
				super(0, 0, 110.5, 33.5);
				this.initTexDraw(ACClientProps.TEX_GUI_AD_SKILL, 291, 0, 221, 67);
				
				skillID = id;
				skill = base.data.getSkill(id);
				
				level = base.data.getSkillLevel(skillID);
				learned = base.data.isSkillLearned(skillID);
				fullyLearned = base.data.getSkillLevel(skillID) == skill.getMaxSkillLevel();
				canUpgrade = base.data.canSkillUpgrade(skillID);
				Pair<Integer, Double> exp = 
					base.dev.getExpectation(TileDeveloper.getAction(TileDeveloper.ID_SKILL_ACQUIRE, id), base.data);
				expectedExp = exp.first;
				expectedEnergy = exp.second.intValue();
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				HudUtils.setTextureResolution(512, 512);
				
				double v0;
				if(fullyLearned) {
					v0 = 139;
				} else if(canUpgrade && mouseHovering) {
					v0 = 1;
				} else if(!learned){
					v0 = 70;
				} else {
					v0 = 208;
				}
				
				GL11.glEnable(GL11.GL_BLEND);
				GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
				RenderUtils.bindIdentity();
				RenderUtils.loadTexture(ACClientProps.TEX_GUI_AD_SKILL);
				HudUtils.drawRect(0, 0, 291, v0, 110.5, 33.5, 221, 67);
				
				//Logo
				ResourceLocation logo = skill.getLogo();
				GenericUtils.assertObj(logo);
				RenderUtils.loadTexture(logo);
				HudUtils.drawRect(6, 8, 16.5, 16.5);
				
				//Skill Name
				String text = skill.getDisplayName();
				RenderUtils.bindColor(base.DEFAULT_COLOR);
				ACUtils.drawText(text, 30, 6, 6, 63);
				GL11.glColor4d(1, 1, 1, 1);
				
				if(learned) {
					//level
					RenderUtils.bindColor(base.DEFAULT_COLOR);
					text = String.format("Lv%d", level);
					ACUtils.drawText(text, 97.5, 5.5, 4.8);
					
					GL11.glColor4d(0.2, 0.2, 0.2, 0.7);
					HudUtils.drawModalRect(30, 15, 75, 1);
					
					GL11.glColor4d(0, 1, 0, 0.8);
					
					HudUtils.drawModalRect(30, 15, 75 * base.data.getSkillUpgradeProgress(skillID), 1);
				}
				
				if(!fullyLearned) {
					RenderUtils.bindColor(base.EXP_INDI_COLOR);
					ACUtils.drawText(String.valueOf(expectedExp), 37, 20, 5.5);
					
					RenderUtils.bindColor(base.EU_INDI_COLOR);
					ACUtils.drawText(String.valueOf(expectedEnergy), 80, 20, 5.5);
				} else {
					RenderUtils.bindColor(59, 177, 43);
					ACUtils.drawText(ACLangs.fullyLearned(), 30, 18.5, 5.5);
				}
			}
			
			@Override
			public void onMouseDown(double mx, double my) {
				if(!this.fullyLearned && this.canUpgrade)
					getGui().addWidget(new DiagActionConfirm(base, TileDeveloper.ID_SKILL_ACQUIRE, skillID));
			}
			
		}

		public SkillList() {
			super("list", 9.5, 9, 110.5, 101.5);
			this.setDragBar(bar);
		}
		
		@Override
		public void onAdded() {
			for(Integer sk : base.data.getCanLearnSkillList()) {
				if(sk != 0) {
					this.addWidget(new SkillElement(sk));
				}
			}
			
			super.onAdded();
		}
		
	}

	private class Bar extends DragBar {

		public Bar() {
			super(120.5, 13, 5.5, 93.5, 12);
			this.enableDragging = true;
		}
		
		@Override
		public void onAdded() {
			super.onAdded();
			this.setTexMapping(280, 0, 11, 24);
			this.addSetTexture(ACClientProps.TEX_GUI_AD_SKILL);
		}
		
		@Override
		public void onProgressChanged() {
			sl.setByRelativeProgress(this.getProgress());
		}
	}
	
	public PageSkills(GuiDeveloper parent) {
		super(parent, "page.adskill", ACClientProps.TEX_GUI_AD_SKILL);
	}
	
	@Override
	public void onAdded() {
		addWidget(bar = new Bar());
		addWidget(sl = new SkillList());
		getGui().addWidget(new HoverEffect());
		
		addWidgets(new Widget(120.5, 7, 5.5, 5.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				sl.progressLast();
				bar.setProgress(sl.getRelativeProgress());
			}
		},
		new Widget(120.5, 107, 5.5, 5.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				sl.progressNext();
				bar.setProgress(sl.getRelativeProgress());
			}
		},
		new RandBufProgressBar(7, 124.5, 122, 5.5, 14, 293, 244, 11) {
			{
				drawer.addHandlers(
					new AssignTexture(ACClientProps.TEX_GUI_AD_SKILL),
					new AssignColor());
				this.fluctRegion = 0;
			}
			@Override
			public double getProgress() {
				return base.dev.curEnergy / base.dev.getMaxEnergy();
			}
		});
	}
	
	@Override
	public void draw(double mx, double my, boolean hover) {
		super.draw(mx, my, hover);
		//sync rate
		String str = String.format("%s: %.2f%%", ACLangs.devSyncRate(), base.dev.getSyncRateForDisplay());
		RenderUtils.bindColor(base.DEFAULT_COLOR);
		ACUtils.drawText(str, 5, 135, 5.5);
		GL11.glColor4d(1, 1, 1, 1);
	}

}
