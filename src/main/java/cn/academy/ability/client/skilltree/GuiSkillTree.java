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
package cn.academy.ability.client.skilltree;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.developer.LearningHelper;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;

/**
 * @author WeAthFolD
 */
public class GuiSkillTree extends LIGuiScreen {
	
	static LIGui loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/skill_tree.xml"));
	static final Color
		CRL_SKILL_ENABLED = new Color().setColor4i(74, 74, 74, 255),
		CRL_SKILL_DISABLED = new Color().setColor4i(48, 48, 48, 48),
		CRL_LVL_ACQUIRED = new Color().setColor4i(196, 196, 196, 0),
		CRL_LVL_CANREACH = new Color().setColor4i(196, 196, 196, 0),
		CRL_LVL_UNREACHED = new Color().setColor4i(65, 65, 65, 0);
	
	static final ResourceLocation
		TEX_EXPPROG_BACK = tex("expprog_back"),
		TEX_EXPPROG_GLOW = tex("expprog_glow"),
		TEX_LVL_GLOW = Resources.getTexture("guis/mark/mark_ball_highlighted");
		
	private static ResourceLocation tex(String loc) {
		return Resources.getTexture("guis/skill_tree/" + loc);
	}
	
	final EntityPlayer player;
	final AbilityData aData;
	final CPData cpData;
	
	Widget window;
	Widget treeArea;
	
	// Skill tree selection info
	String[] types;
	String currentType;
	
	public GuiSkillTree(EntityPlayer _player) {
		player = _player;
		aData = AbilityData.get(player);
		cpData = CPData.get(player);
		initPages();
	}
	
	protected void onSkillWidgetCreated(Widget w) {}
	
	private void initPages() {
		window = loaded.getWidget("window").copy();
		treeArea = window.getWidget("tree_area");
		
		window.addWidget(loaded.getWidget("widgets/window_esper").copy());
		window.addWidget(loaded.getWidget("widgets/window_machine").copy());
		
		for(int i = 1; i <= 5; ++i) {
			Widget ball = treeArea.getWidget("ball" + i);
			ball.addComponent(new LevelHandler(i));
		}
		
		if(aData.isLearned()) {
			initSkillTree();
		} else {
			window.addWidget(loaded.getWidget("widgets/not_acquired").copy());
		}
		
		gui.addWidget(window);
	}
	
	private void initSkillTree() {
		Category cat = aData.getCategory();
		types = cat.getTypes();
		setCurrentType(types[0]);
	}
	
	private void setCurrentType(String type) {
		currentType = type;
		List<Skill> skills = aData.getCategory().getSkillsOfType(currentType);
		for(Skill s : skills) {
			treeArea.addWidget(createSkillWidget(s));
		}
	}
	
	private Widget createSkillWidget(Skill skill) {
		Widget ret = loaded.getWidget("widgets/single_skill").copy();
		DrawTexture.get(ret).color = aData.isSkillLearned(skill) ? CRL_SKILL_ENABLED : CRL_SKILL_DISABLED;
		DrawTexture.get(ret.getWidget("skill_icon")).setTex(skill.getHintIcon());
		ret.regEventHandler(new FrameEventHandler() {

			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				// Draw the progress bar
				GL11.glPushMatrix();
				GL11.glDisable(GL11.GL_CULL_FACE);
				GL11.glTranslated(w.transform.width / 2, w.transform.height / 2, 0);
				final float size = 70;
				GL11.glColor4d(1, 1, 1, event.hovering ? 1 : 0.8);
				GL11.glScalef(size, size, 1);
				ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_BACK, 1);
				ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_GLOW, aData.getSkillExp(skill));
				GL11.glEnable(GL11.GL_CULL_FACE);
				GL11.glPopMatrix();
				GL11.glColor4d(1, 1, 1, 1);
			}
			
		});
		ret.transform.setPos(skill.guiPosition.x, skill.guiPosition.y);
		return ret;
	}
	
	public class LevelHandler extends Component {
		
		final int level;
	 	
	 	public LevelHandler(int _level) {
	 		super("LevelHandler");
	 		level = _level;
	 		this.addEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					if(event.hovering) {
						Font.font.draw(StatCollector.translateToLocal("ac.ability.level" + level),
							-10, -10, 40, 0xd0ffffff, Align.RIGHT);
					}
				}
	 			
	 		});
	 	}

	 	public void onAdded() {
	 		Tint tint = new Tint();
	 		tint.affectTexture = true;
	 		
	 		Color color;
	 		if(level == aData.getLevel() + 1 && LearningHelper.canLevelUp(aData)) {
	 			double original = widget.transform.width, now = 60;
	 			widget.transform.setSize(now, now);
	 			double offset = -(now - original) / 2;
	 			widget.transform.x += offset;
	 			widget.transform.y += offset;
	 			color = CRL_LVL_CANREACH;
	 			DrawTexture.get(widget).setTex(TEX_LVL_GLOW);
	 		} else if(level <= aData.getLevel()) {
	 			color = CRL_LVL_ACQUIRED;
	 		} else {
	 			color = CRL_LVL_UNREACHED;
	 		}
	 		
	 		tint.idleColor = color.copy();
	 		tint.hoverColor = color.copy();
	 		tint.idleColor.a = 0.8;
	 		tint.hoverColor.a = 1;
	 		
	 		widget.addComponent(tint);
	 	}
	 	
	 }

	
}
