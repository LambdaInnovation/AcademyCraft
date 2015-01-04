/**
 * 
 */
package cn.academy.core.client.gui.dev;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;
import cn.liutils.api.gui.Widget;
import cn.liutils.api.gui.widget.ListVertical;
import cn.liutils.api.gui.widget.ProgressBar;
import cn.liutils.util.HudUtils;
import cn.liutils.util.RenderUtils;
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
			
			final SkillBase skill;
			final boolean learned;

			public SkillElement(int id) {
				super("skill_" + id, SkillList.this, 0, 0, 110.5, 33.5);
				this.setTexMapping(291, 0, 221, 67);
				this.setTexture(ACClientProps.TEX_GUI_AD_SKILL, 512, 512);
				skill = dev.data.getSkill(id);
				learned = dev.data.isSkillLearned(id);
			}
			
			@Override
			public void draw(double mx, double my, boolean mouseHovering) {
				if(learned) {
					area.v = 139;
				} else if(mouseHovering) {
					area.v = 1;
				} else {
					area.v = 70;
				}
				//System.out.println(skill);
				super.draw(mx, my, mouseHovering);
				
				//Logo
				ResourceLocation logo = skill.getLogo();
				RenderUtils.loadTexture(logo);
				HudUtils.drawRect(6, 8, 16.5, 16.5);
				
				//Skill Name
				String text = skill.getDisplayName();
				RenderUtils.bindColor(dev.DEFAULT_COLOR);
				TextUtils.drawText(TextUtils.FONT_CONSOLAS_64, text, 30, 5, 11);
				GL11.glColor4d(1, 1, 1, 1);
			}
			
			public void onMouseDown(double mx, double my) {
				System.out.println("learn " + skill.getDisplayName());
			}
			
		}

		public SkillList() {
			super("list", PageSkills.this, 9.5, 9, 110.5, 101.5);
			int max = dev.data.getSkillCount();
			for(int i = 1; i < max; ++i) {
				new SkillElement(i);
			}
		}
		
	}

	private class Bar extends ProgressBar {

		public Bar() {
			super("bar", PageSkills.this, 120.5, 13, 5.5, 93.5, 12);
			this.enableDragging = true;
			this.setTexture(ACClientProps.TEX_GUI_AD_SKILL, 512, 512);
			this.setTexMapping(280, 0, 11, 24);
		}
		
		@Override
		public void onProgressChanged() {
			sl.setByRelativeProgress(this.getProgress());
		}
	}
	
	public PageSkills(PageMainBase parent) {
		super(parent, "page.adskill", ACClientProps.TEX_GUI_AD_SKILL);
		sl = new SkillList();
		bar = new Bar();
		new Widget("up", this, 120.5, 7, 5.5, 5.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				sl.progressLast();
				bar.setProgress(sl.getRelativeProgress());
			}
		};
		new Widget("down", this, 120.5, 107, 5.5, 5.5) {
			@Override
			public void onMouseDown(double mx, double my) {
				sl.progressNext();
				bar.setProgress(sl.getRelativeProgress());
			}
		};
	}

}
