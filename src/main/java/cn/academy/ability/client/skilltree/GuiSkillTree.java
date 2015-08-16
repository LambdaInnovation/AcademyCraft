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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.developer.LearningHelper;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.academy.core.client.glsl.ShaderMono;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegInit;
import cn.liutils.cgui.gui.LIGui;
import cn.liutils.cgui.gui.LIGuiScreen;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.Component;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.cgui.loader.xml.CGUIDocLoader;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.client.RenderUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Font;
import cn.liutils.util.helper.Font.Align;

/**
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class GuiSkillTree extends LIGuiScreen {
	
	static ShaderMono shader;
	static LIGui loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/skill_tree.xml"));
	static final Color
		CRL_SKILL_ENABLED = new Color().setColor4i(74, 74, 74, 255),
		CRL_SKILL_DISABLED = new Color().setColor4i(48, 48, 48, 48),
		CRL_LVL_ACQUIRED = new Color().setColor4i(196, 196, 196, 0),
		CRL_LVL_CANREACH = new Color().setColor4i(255, 255, 255, 0),
		CRL_LVL_UNREACHED = new Color().setColor4i(65, 65, 65, 0),
		CRL_LINE = new Color().setColor4i(178, 178, 178, 255),
		CRL_LINE_DISABLED = new Color().setColor4i(132, 132, 132, 255);
	
	static final ResourceLocation
		TEX_EXPPROG_BACK = tex("expprog_back"),
		TEX_EXPPROG_GLOW = tex("expprog_glow"),
		TEX_LVL_GLOW = Resources.getTexture("guis/mark/mark_ball_highlighted");
	
	public static void init() {
		shader = ShaderMono.instance();
	}
	
	private static ResourceLocation tex(String loc) {
		return Resources.getTexture("guis/skill_tree/" + loc);
	}
	
	final EntityPlayer player;
	final AbilityData aData;
	final CPData cpData;
	
	Widget window;
	Widget treeArea;
	
	List<Widget> skillWidgets;
	List<int[]> connections;
	
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
		
		System.out.println("load events");
		gui.addWidget("window", window);
		EventLoader.load(gui, this);
	}
	
	private void initSkillTree() {
		Category cat = aData.getCategory();
		
		List<Skill> list = cat.getSkillList();
		List<Skill> list2 = new ArrayList();
		
		skillWidgets = new ArrayList();
		for(Skill s : list) {
			if(aData.isSkillLearned(s) || LearningHelper.canBePotentiallyLearned(aData, s)) {
				Widget w = createSkillWidget(s);
				onSkillWidgetCreated(w);
				treeArea.addWidget(w);
				skillWidgets.add(w);
				list2.add(s);
			}
		}
		
		System.out.println("HuH");
		connections = new ArrayList();
		for(int i = 0; i < list2.size(); ++i) {
			Skill s = list2.get(i);
			Skill parent = s.getParent();
			if(parent != null) {
				for(int j = 0; j < list2.size(); ++j) {
					if(list2.get(j) == parent) {
						connections.add(new int[] { i, j });
						break;
					}
				}
			}
		}
	}
	
	@GuiCallback("window/tree_area")
	public void drawConnections(Widget w, FrameEvent event) {
		//System.out.println("DC" + connections.size());
		final double zLevel = 0.5;
		for(int[] c : connections) {
			Widget a = skillWidgets.get(c[0]), b = skillWidgets.get(c[1]);
			Skill parent = ((SkillHandler) b.getComponent("SkillHandler")).skill;
			if(aData.isSkillLearned(parent))
				CRL_LINE.bind();
			else
				CRL_LINE_DISABLED.bind();
			GL11.glLineWidth(2f);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glBegin(GL11.GL_LINES);
			
			double x0 = a.transform.x, y0 = a.transform.y, x1 = b.transform.x, y1 = b.transform.y;
			double dy = y1 - y0, dx = x1 - x0;
			double len = Math.sqrt(dx * dx + dy * dy);
			final double move = 45;
			dx *= move / len; dy *= move / len;
			
			GL11.glVertex3d(a.transform.x + dx, a.transform.y + dy, zLevel);
			GL11.glVertex3d(b.transform.x - dx, b.transform.y - dy, zLevel);
			GL11.glEnd();
			GL11.glEnable(GL11.GL_TEXTURE_2D);
		}
		
	}
	
	private Widget createSkillWidget(Skill skill) {
		Widget ret = loaded.getWidget("widgets/single_skill").copy();
		DrawTexture.get(ret.getWidget("back")).color = aData.isSkillLearned(skill) ? CRL_SKILL_ENABLED : CRL_SKILL_DISABLED;
		DrawTexture.get(ret.getWidget("skill_icon")).setTex(skill.getHintIcon());
		ret.addComponent(new SkillHandler(skill));
		ret.transform.setPos(skill.guiPosition.x, skill.guiPosition.y);
		return ret;
	}
	
	public class SkillHandler extends Component {
		
		final Skill skill;
		long lastHover = -1;

		public SkillHandler(Skill _skill) {
			super("SkillHandler");
			skill = _skill;
		}
		
		@Override
		public void onAdded() {
			if(!aData.isSkillLearned(skill)) {
				Widget icon = widget.getWidget("skill_icon");
				icon.removeComponent("DrawTexture");
				icon.regEventHandler(new FrameEventHandler() {

					@Override
					public void handleEvent(Widget w, FrameEvent event) {
						GL11.glDisable(GL11.GL_DEPTH_TEST);
						GL11.glColor4d(1, 1, 1, 1);
						shader.useProgram();
						RenderUtils.loadTexture(skill.getHintIcon());
						HudUtils.rect(0, 0, w.transform.width, w.transform.height);
						GL20.glUseProgram(0);
						GL11.glEnable(GL11.GL_DEPTH_TEST);
					}
					
				});
			}
			
			widget.getWidget("back").regEventHandler(new FrameEventHandler() {

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					// Draw the progress bar
					GL11.glPushMatrix();
					GL11.glDepthMask(false);
					GL11.glDisable(GL11.GL_CULL_FACE);
					GL11.glTranslated(w.transform.width / 2, w.transform.height / 2, 1);
					final float size = 70;
					GL11.glColor4d(1, 1, 1, event.hovering ? 1 : 0.8);
					GL11.glScalef(size, size, 1);
					ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_BACK, 1);
					ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_GLOW, aData.getSkillExp(skill));
					GL11.glEnable(GL11.GL_CULL_FACE);
					GL11.glDepthMask(true);
					GL11.glPopMatrix();
					GL11.glColor4d(1, 1, 1, 1);
					
					// Draw the skill name
					if(event.hovering) {
						GL11.glPushMatrix();
						GL11.glTranslated(0, 0, 10);
						Font.font.draw(skill.getDisplayName(), 20, -5, 40, 0xbbbbbb, Align.RIGHT);
						GL11.glPopMatrix();
					}
				}
				
			});
		}
		
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
	 		System.out.println(level);
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
