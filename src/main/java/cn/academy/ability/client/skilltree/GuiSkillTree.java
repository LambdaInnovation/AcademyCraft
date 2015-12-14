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

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glColor4d;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTranslated;
import static org.lwjgl.opengl.GL20.glUseProgram;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2d;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.cgui.gui.CGui;
import cn.lambdalib.cgui.gui.CGuiScreen;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.Component;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.TextBox;
import cn.lambdalib.cgui.gui.component.Tint;
import cn.lambdalib.cgui.gui.event.FrameEvent;
import cn.lambdalib.cgui.gui.event.IGuiEventHandler;
import cn.lambdalib.cgui.gui.event.LeftClickEvent;
import cn.lambdalib.cgui.loader.xml.CGUIDocLoader;
import cn.lambdalib.util.client.HudUtils;
import cn.lambdalib.util.client.RenderUtils;
import cn.lambdalib.util.client.shader.ShaderMono;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Color;
import cn.lambdalib.util.helper.Font;
import cn.lambdalib.util.helper.GameTimer;
import cn.lambdalib.util.helper.Font.Align;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 */
@Registrant
public abstract class GuiSkillTree extends CGuiScreen {
	
	static ShaderMono shader;
	static CGui loaded = CGUIDocLoader.load(new ResourceLocation("academy:guis/skill_tree.xml"));
	static final Color
		CRL_SKILL_ENABLED = new Color().setColor4i(74, 74, 74, 255),
		CRL_SKILL_DISABLED = new Color().setColor4i(48, 48, 48, 48),
		CRL_LVL_ACQUIRED = new Color().setColor4i(196, 196, 196, 0),
		CRL_LVL_CANREACH = new Color().setColor4i(255, 255, 255, 0),
		CRL_LVL_UNREACHED = new Color().setColor4i(65, 65, 65, 0),
		CRL_LINE = new Color().setColor4i(178, 178, 178, 255),
		CRL_LINE_DISABLED = new Color().setColor4i(132, 132, 132, 255),
		CRL_SDESC_LEARNED = new Color(),
		CRL_SDESC_NOTLEARNED = new Color().setColor4i(117, 117, 117, 255),
		CRL_WARNING = new Color().setColor4i(240, 51, 51, 255),
		CRL_SKILL_DESC_1 = new Color().setColor4i(222, 222, 222, 255),
		CRL_BACKGRND = new Color().setColor4i(42, 42, 42, 180);
	
	public static final ResourceLocation
		TEX_EXPPROG_BACK = tex("expprog_back"),
		TEX_EXPPROG_GLOW = tex("expprog_glow"),
		TEX_LVL_GLOW = Resources.getTexture("guis/mark/mark_ball_highlighted"),
		TEX_UNKNOWN_CAT = tex("cat_not_found");

    @RegInitCallback
	public static void init() {
		shader = ShaderMono.instance();
	}
	
	private static ResourceLocation tex(String loc) {
		return Resources.getTexture("guis/skill_tree/" + loc);
	}
	
	final EntityPlayer player;
	final AbilityData aData;
	final CPData cpData;
	final DeveloperType type;
	final boolean isApp;
	
	protected Widget window;
	protected Widget treeArea;
	
	Widget windowEsper, windowMachine;
	
	List<Widget> skillWidgets;
	List<int[]> connections;
	
	public GuiSkillTree(EntityPlayer _player, DeveloperType _type, boolean _isApp) {
		player = _player;
		type = _type;
		isApp = _isApp;
		aData = AbilityData.get(player);
		cpData = CPData.get(player);
		initPages();
	}
	
	protected abstract Widget createDesc(SkillHandler handler);
	
	private void initPages() {
		window = loaded.getWidget("window").copy();
		treeArea = window.getWidget("tree_area");
		
		window.addWidget("window_esper", windowEsper = loaded.getWidget("widgets/window_esper").copy());
		window.addWidget("window_machine", windowMachine = loaded.getWidget("widgets/window_machine").copy());
		
		TextBox.get(window.getWidget("title")).content = "ac.skill_tree.title" + (isApp ? 0 : 1);
		
		for(int i = 1; i <= 5; ++i) {
			Widget ball = treeArea.getWidget("ball" + i);
			ball.addComponent(new LevelHandler(i));
		}
		
		if(aData.isLearned()) {
			initSkillTree();
		} else {
			window.addWidget(loaded.getWidget("widgets/not_acquired").copy());
		}
		
		initPlayerInfo();
		TextBox.get(windowMachine.getWidget("dev_type")).content = SkillTreeLocal.machineType(type);

		window.getWidget("tree_area").listen(FrameEvent.class, (w, e) -> {
			if(connections == null)
				return;
			final double zLevel = 0.5;
			for(int[] c : connections) {
				Widget a = skillWidgets.get(c[0]), b = skillWidgets.get(c[1]);
				Skill me = ((SkillHandler) a.getComponent("SkillHandler")).skill;
				if(aData.isSkillLearned(me))
					CRL_LINE.bind();
				else
					CRL_LINE_DISABLED.bind();
				glPushMatrix();
				glTranslated(0, 0, zLevel);
				glDisable(GL_TEXTURE_2D);
				glDisable(GL_DEPTH_TEST);

				double x0 = a.transform.x, y0 = a.transform.y, x1 = b.transform.x, y1 = b.transform.y;
				double dy = y1 - y0, dx = x1 - x0;
				double len = Math.sqrt(dx * dx + dy * dy);
				final double move = 45;
				dx *= move / len; dy *= move / len;

				x0 += dx * a.transform.scale; y0 += dy * a.transform.scale;
				x1 -= dx * b.transform.scale; y1 -= dy * b.transform.scale;
				float alpha = ((SkillHandler) a.getComponent("SkillHandler")).getAlpha();
				x0 = x1 + (x0 - x1) * alpha; y0 = y1 + (y0 - y1) * alpha;

				ACRenderingHelper.lineSegment(x0, y0, x1, y1, 2.2f);

				glEnable(GL_DEPTH_TEST);
				glEnable(GL_TEXTURE_2D);
				glPopMatrix();
			}
		});

		gui.addWidget("window", window);
	}
	
	private void initSkillTree() {
		Category cat = aData.getCategory();
		
		List<Skill> list = cat.getSkillList();
		List<Skill> list2 = new ArrayList();
		
		skillWidgets = new ArrayList();
		
		int i = 0;
		for(Skill s : list) {
			if(aData.isSkillLearned(s) || LearningHelper.canBePotentiallyLearned(aData, s)) {
				Widget w = createSkillWidget(s, i++);
				treeArea.addWidget(w);
				skillWidgets.add(w);
				list2.add(s);
			}
		}
		
		connections = new ArrayList();
		for(i = 0; i < list2.size(); ++i) {
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
	
	private void initPlayerInfo() {
		TextBox.get(windowEsper.getWidget("text_user")).content = player.getDisplayName();
		TextBox.get(windowEsper.getWidget("text_level")).content = SkillTreeLocal.levelDesc(aData.getLevel());
		TextBox.get(windowEsper.getWidget("text_prg")).content = SkillTreeLocal.levelPrg(player);
		
		Category cat = aData.getCategory();
		TextBox.get(windowEsper.getWidget("text_cat")).content = cat == null ? SkillTreeLocal.unknown() : cat.getDisplayName();
		DrawTexture.get(windowEsper.getWidget("icon_cat")).texture = cat == null ? TEX_UNKNOWN_CAT : cat.getIcon();
	}
	
	@Override
    public boolean doesGuiPauseGame() {
        return false;
    }
	
	private Widget createSkillWidget(Skill skill, int i) {
		Widget ret = loaded.getWidget("widgets/single_skill").copy();
		DrawTexture.get(ret.getWidget("back")).color = aData.isSkillLearned(skill) ? CRL_SKILL_ENABLED : CRL_SKILL_DISABLED;
		DrawTexture.get(ret.getWidget("skill_icon")).setTex(skill.getHintIcon());
		ret.addComponent(new SkillHandler(skill, i));
		ret.transform.setPos(skill.guiX, skill.guiY);
		return ret;
	}
	
	enum ScaleState { IN, OUT }
	
	public class SkillHandler extends Component {
		
		final Skill skill;
		
		final int scaleTime = 100;
		final double maxScale = 1.2;
		
		private boolean active = false, lastHovering = false;
		
		private ScaleState current, queued;
		private long lastEvent, create;
		private int timeOffset;

		public SkillHandler(Skill _skill, int i) {
			super("SkillHandler");
			skill = _skill;
			create = GameTimer.getTime();
			timeOffset = i * 100;
		}
		
		void active() {
			active = true;
			startScale(ScaleState.IN);
		} 
		
		void deactive() {
			active = false;
			startScale(ScaleState.OUT);
		}
		
		void startScale(ScaleState state) {
			if(current == null) {
				current = state;
				lastEvent = GameTimer.getTime();
			} else {
				if(state != current) {
					queued = state;
				}
			}
		}
		
		float getAlpha() {
			long dt = GameTimer.getTime() - create;
			return MathUtils.clampf(0, 1, (float)(dt - timeOffset) / 200);
		}
		
		@Override
		public void onAdded() {
			Widget back = widget.getWidget("back");
			
			back.listen(LeftClickEvent.class, (w, event) -> 
			{
				Widget menu = createDesc(SkillHandler.this);
				if(menu != null) {
					active();
					menu.transform.x += widget.transform.x;
					menu.transform.y += widget.transform.y;
					
					// To make the draw order correct, we used a bit of hack to re-add the current skill
					// widget and menu widget to the last of the draw list.
					treeArea.forceRemoveWidget(widget);
                    widget.disposed = false;
					treeArea.addWidget(menu);
					treeArea.addWidget(widget);
				}
			});
			
			if(!aData.isSkillLearned(skill)) {
				Widget icon = widget.getWidget("skill_icon");
				icon.removeComponent("DrawTexture");
				icon.listen(FrameEvent.class, (w, event) -> {
					glColor4f(1, 1, 1, getAlpha());
					shader.useProgram();
					glDepthMask(active);
					RenderUtils.loadTexture(skill.getHintIcon());
					HudUtils.pushZLevel();
					HudUtils.zLevel = active ? 11 : 1;
					HudUtils.rect(0, 0, w.transform.width, w.transform.height);
					HudUtils.popZLevel();
					glDepthMask(true);
					glUseProgram(0);
				});
			}
			
			back.listen(FrameEvent.class, (w, event) -> 
			{
				double zLevel = active ? 11 : 1;
				float alpha = getAlpha();
				
				DrawTexture dt1 = DrawTexture.get(widget.getWidget("skill_icon"));
				if(dt1 != null) {
					dt1.zLevel = zLevel;
					dt1.writeDepth = active;
					dt1.color.a = alpha;
				}
				dt1 = DrawTexture.get(w);
				dt1.zLevel = zLevel;
				dt1.writeDepth = active;
				dt1.color.a = alpha;
				
				// Size update
				if(current != null) {
					long dt = GameTimer.getTime() - lastEvent;
					double prog = Math.min(1.0, (double) dt / scaleTime);
					if(current == ScaleState.IN) {
						widget.transform.scale = MathUtils.lerp(1, maxScale, prog);
					} else {
						widget.transform.scale = MathUtils.lerp(maxScale, 1, prog);
					}
					if(prog >= 1.0)
						current = null;
					widget.dirty = true;
				} else {
					if(queued != null) {
						startScale(queued);
						queued = null;
					}
				}
				
				if(!active && (event.hovering ^ lastHovering)) {
					startScale(event.hovering ? ScaleState.IN : ScaleState.OUT);
				}
				
				lastHovering = event.hovering;
				
				// Draw the progress bar
				glPushMatrix();
				glDepthMask(false);
				glDisable(GL_CULL_FACE);
				glTranslated(w.transform.width / 2, w.transform.height / 2, zLevel);
				final float size = 70;
				glColor4d(1, 1, 1, alpha * (event.hovering ? 1 : 0.8));
				glScalef(size, size, 1);
				ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_BACK, 1);
				ACRenderingHelper.drawCircularProgbar(TEX_EXPPROG_GLOW, aData.getSkillExp(skill));
				glEnable(GL_CULL_FACE);
				glDepthMask(true);
				glPopMatrix();
				glColor4d(1, 1, 1, 1);
				
				// Draw the skill name
				if(event.hovering && !active && alpha == 1.0f) {
					glPushMatrix();
					glTranslated(0, 0, 10);
					Font.font.draw(skill.getDisplayName(), 20, -5, 40, 0xbbbbbb, Align.RIGHT);
					glPopMatrix();
				}
			});
		}
		
	}
	
	public class LevelHandler extends Component {
		
		final int level;
	 	
	 	public LevelHandler(int _level) {
	 		super("LevelHandler");
	 		level = _level;
	 		
	 		listen(FrameEvent.class, (w, event) -> {
	 			glPushMatrix();
				glTranslated(0, 0, 15);
				if(event.hovering) {
					if(level <= aData.getLevel())
						Font.font.draw(SkillTreeLocal.levelDesc(level),
								-10, -10, 37, 0xb0ffffff, Align.RIGHT);
				}
				if(LearningHelper.canLevelUp(DeveloperType.ADVANCED, aData) && level == aData.getLevel() + 1) {
					Font.font.draw(level == 1 ? SkillTreeLocal.acquire() : SkillTreeLocal.upgradeTo(level),
							-10, -10, 37, event.hovering ? 0xf0ffffff : 0xa0ffffff, Align.RIGHT);
				}
				glPopMatrix();
	 		});
	 		
	 		listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {
	 			
	 			boolean checked = false;

				@Override
				public void handleEvent(Widget w, FrameEvent event) {
					if(!checked && level == aData.getLevel() + 1 && LearningHelper.canLevelUp(DeveloperType.ADVANCED, aData)) {
						checked = true;
						
						double original = widget.transform.width, now = 60;
			 			widget.transform.setSize(now, now);
			 			double offset = -(now - original) / 2;
			 			widget.transform.x += offset;
			 			widget.transform.y += offset;
			 			widget.dirty = true;
			 			
			 			Tint tint = Tint.get(w);
			 			Color color = CRL_LVL_CANREACH;
			 			tint.idleColor = color.copy();
			 			tint.hoverColor = color.copy();
			 			
			 			tint.idleColor.a = 0.8;
				 		tint.hoverColor.a = 1;
			 			
			 			DrawTexture dt = DrawTexture.get(widget);
			 			dt.setTex(TEX_LVL_GLOW);
			 			
			 			widget.listen(FrameEvent.class, (ww, e) -> {
			 				dt.color.a = (event.hovering ? 1.0 : 0.8) * (0.4 + 0.3 * (1 + Math.sin(GameTimer.getTime() / 300.0)));
			 			});
					}
				}
	 			
	 		});
	 	}

	 	@Override
		public void onAdded() {
	 		super.onAdded();
	 		
	 		Tint tint = new Tint();
	 		tint.affectTexture = true;
	 		Color color;
	 		if(level <= aData.getLevel()) {
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

	public class SkillLevelDesc extends Widget {
		
		final Skill skill;
		final Color color;
		
		public SkillLevelDesc(Skill _skill) {
			skill = _skill;
			boolean learned = aData.isSkillLearned(skill);
			color = learned ? CRL_SDESC_LEARNED : CRL_SDESC_NOTLEARNED;
			double width1 = Font.font.strLen(skill.getDisplayName(), 50);
			transform.setSize(Math.max(180 + width1, 450), 115);
			transform.doesListenKey = false;
			{
				Long time = GameTimer.getTime();
				listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {

					@Override
					public void handleEvent(Widget w, FrameEvent event) {
						glPushMatrix();
						glTranslated(0, 0, 10);
						color.a = blendfac(300);
						Font.font.draw(skill.getDisplayName(), 120, -6, 50, color.asHexColor());
						color.a = blendfac(400);
						Font.font.draw("Lv" + skill.getLevel(), 
							w.getWidgetParent().transform.width - 50, -1, 44, color.asHexColor(), Align.RIGHT);
						
						Color color2 = learned ? CRL_SKILL_DESC_1 : CRL_WARNING;
						color2.a = blendfac(500);
						Font.font.draw(learned ? SkillTreeLocal.acquiredProg(aData.getSkillExp(skill)) : SkillTreeLocal.notAcquired(), 
								122, 50, 36, color2.asHexColor());
						glPopMatrix();
					}
					
					private double blendfac(int offset) {
						return .1 + .9 * MathUtils.clampd(0, 1, (GameTimer.getTime() - time - offset) / 200.0d);
					}
					
				});
			}
		}
		
	}
	
	public class SkillHint extends Widget {
		
		static final double FONT_SIZE = 38;
		final String text;
		long ct = GameTimer.getTime();
		double width = 420;
		
		public SkillHint(WidgetSkillDesc _skill) {
			Skill skill = _skill.handler.skill;
			text = aData.isSkillLearned(skill) ? skill.getDescription() : SkillTreeLocal.unknownSkill();
			Vector2d vec = Font.font.simDrawWrapped(text, FONT_SIZE, 
					width = Math.max(width, _skill.width - 30));
			transform.setSize(vec.x, vec.y);
			transform.doesListenKey = false;
			
			listen(FrameEvent.class, (w, e) -> 
			{
				glPushMatrix();
				glTranslated(0, 0, 10);
				double a = .1 + .9 * MathUtils.clampd(0, 1, (GameTimer.getTime() - ct - 700.0) / 200.0);
				CRL_SKILL_DESC_1.a = a;
				Font.font.drawWrapped(text, 0, 0, FONT_SIZE, CRL_SKILL_DESC_1.asHexColor(), width);
				glPopMatrix();
			});
		}
		
	}
	
}
