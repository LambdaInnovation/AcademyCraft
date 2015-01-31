/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.core.block.dev.TileDeveloper;
import cn.academy.core.client.ACLangs;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.draw.prop.AssignColor;
import cn.liutils.api.draw.prop.AssignTexture;
import cn.liutils.api.draw.tess.GUIRect;
import cn.liutils.api.draw.tess.RectMapping;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.DragBar;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.api.gui.widget.RandBufProgressBar;
import cn.liutils.util.GenericUtils;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
import cn.liutils.util.misc.Pair;
import cn.liutils.util.render.TextUtils;

/**
 * @author WeathFolD
 *
 */
public class PageSkills extends DevSubpage {
	
	SkillList sl;
	Bar bar;
	
	private class SkillList extends ListVertical {
		
		private class SkillElement extends Widget {
			
			final int skillID;
			final SkillBase skill;
			
			//Inferred data
			final boolean learned;
			final boolean fullyLearned;
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
				Pair<Integer, Double> exp = 
					base.dev.getExpectation(base.dev.getAction(TileDeveloper.ID_SKILL_ACQUIRE, id), base.data);
				expectedExp = exp.first;
				expectedEnergy = exp.second.intValue();
				this.doesListenKey = !fullyLearned;
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				HudUtils.setTextureResolution(512, 512);
				
				double v0;
				if(fullyLearned) {
					v0 = 139;
				} else if(mouseHovering) {
					v0 = 1;
				} else if(!learned){
					v0 = 70;
				} else {
					v0 = 208;
				}
				
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
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 30, 5.5, 10);
				GL11.glColor4d(1, 1, 1, 1);
				
				if(learned) {
					//level
					RenderUtils.bindColor(base.DEFAULT_COLOR);
					text = String.format("Lv%d", level);
					base.drawText(text, 97.5, 5.5, 7);
				}
				
				if(!fullyLearned) {
					RenderUtils.bindColor(base.EXP_INDI_COLOR);
					base.drawText(String.valueOf(expectedExp), 37, 19, 8);
					
					RenderUtils.bindColor(base.EU_INDI_COLOR);
					base.drawText(String.valueOf(expectedEnergy), 80, 19, 8);
				} else {
					RenderUtils.bindColor(59, 177, 43);
					base.drawText(ACLangs.fullyLearned(), 30, 17.5, 8);
				}
			}
			
			public void onMouseDown(double mx, double my) {
				new DiagActionConfirm(base, TileDeveloper.ID_SKILL_ACQUIRE, skillID);
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
					System.out.println("Adding " + sk);
					this.addWidget(new SkillElement(sk));
				}
			}
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
		TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, str, 5, 135, 8);
		GL11.glColor4d(1, 1, 1, 1);
	}

}
