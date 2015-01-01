/**
 * 
 */
package cn.academy.core.client.gui.dev;

import java.util.Set;

import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.liutils.api.client.gui.part.LIGuiButton;
import cn.liutils.api.client.gui.part.LIGuiPart;
import cn.liutils.api.client.gui.part.LIGuiScrollerHorizontal;
import cn.liutils.api.client.gui.part.LIGuiScrollerHorizontal.ScrollerEntry;
import cn.liutils.api.client.util.HudUtils;

/**
 * @author WeathFolD
 *
 */
public class PageMainOrdinary extends PageMainBase {
	
	private class Scroller extends LIGuiScrollerHorizontal {
		

		public Scroller() {
			super("scroller", 9.5F, 9, 110.5F, 101.5F, 34);
		}
		
	}
	
	private class Entry extends ScrollerEntry {
		
		SkillBase skill;
		boolean hasLearned;

		public Entry(int sid) {
			super();
			skill = dev.data.getSkill(sid);
			hasLearned = dev.data.isSkillLearned(sid);
		}

		@Override
		public void draw(boolean hover) {
			double v = hover ? 0 : (hasLearned ? 138 : 69);
			HudUtils.drawTexturedModalRect(0, 0, 291, v, 101.5, 34, 221, 68);
		}

		@Override
		public void addElements(Set<LIGuiPart> set) {}

		@Override
		public void onPartClicked(LIGuiPart part, float subX, float subY) {}

		@Override
		public void onEntryClicked() {
			//Open confirm window
		}
		
	}

	public PageMainOrdinary(GuiDeveloper gd) {
		super(gd);
	}
	
	@Override
	public void addElements(Set<LIGuiPart> set) {
		set.add(new LIGuiButton("lpg", 89, 6.5F, 8, 8) {
			@Override
			public boolean onPartClicked(float x, float y) {
				int next = Math.min(dev.pageID + 1, dev.subs.size() - 1);
				if(next == dev.subs.size() - 1) {
					//switch to non-ordinary
				}
				dev.pageID = next;
				return true;
			}
		});
		
		set.add(new LIGuiButton("rpg", 215.5F, 6.5F, 8, 8) {
			@Override
			public boolean onPartClicked(float x, float y) {
				int next = Math.max(0, (dev.pageID - 1));
				dev.pageID = next;
				return true;
			}
		});
	}

}
