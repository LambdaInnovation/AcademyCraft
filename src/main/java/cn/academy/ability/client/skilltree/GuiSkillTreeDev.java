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

import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTranslated;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.DevelopTypeSkill;
import cn.academy.ability.developer.Developer;
import cn.academy.ability.developer.Developer.DevState;
import cn.academy.ability.developer.IDevCondition;
import cn.academy.ability.developer.IDevelopType;
import cn.academy.ability.developer.LearningHelper;
import cn.academy.core.client.component.Glow;
import cn.academy.core.client.glsl.ShaderMono;
import cn.annoreg.mc.network.Future;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.annotations.GuiCallback;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.ProgressBar;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.component.Transform.HeightAlign;
import cn.liutils.cgui.gui.component.Transform.WidthAlign;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.cgui.loader.EventLoader;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Font;


/**
 * @author WeAthFolD
 */
public class GuiSkillTreeDev extends GuiSkillTree {
	
	interface ICallback {
		void invoke();
	}
	
	final Developer developer;
	
	Overlay overlay;

	public GuiSkillTreeDev(EntityPlayer _player, Developer _developer) {
		super(_player, _developer.type);
		developer = _developer;
		
		EventLoader.load(gui, this);
	}
	
	@GuiCallback("window/window_machine/p_energy")
	public void updateEnergy(Widget w, FrameEvent event) {
		ProgressBar bar = ProgressBar.get(w);
		bar.progress = developer.getEnergy() / developer.getMaxEnergy();
		
		if(event.hovering) {
			RenderingHelper.drawTextBox(
				String.format("%.0f/%.0fIF", developer.getEnergy(), developer.getMaxEnergy()), 
				event.mx, event.my, 40);
		}
	}
	
	@Override
	protected Widget createDesc(SkillHandler handler) {
		Skill skill = handler.skill;
		WidgetSkillDesc ret = new WidgetSkillDesc(handler);
		ret.addWidget(new GuiSkillTree.SkillLevelDesc(skill));
		
		if(aData.isSkillLearned(skill)) {
			ret.addWidget(new GuiSkillTree.SkillHint(ret));
		} else {
			List<IDevCondition> devConditions = skill.getDevConditions();
			if(!devConditions.isEmpty()){ // Dep display
				Widget area = new Widget();
				area.transform.setSize(450, 50);
				area.transform.doesListenKey = false;
				
				Widget wtext = new Widget();
				wtext.transform.alignHeight = HeightAlign.BOTTOM;
				
				TextBox text = new TextBox();
				text.allowEdit = false;
				text.content = SkillTreeLocal.required();
				text.heightAlign = HeightAlign.CENTER;
				text.size = 38;
				wtext.addComponent(text);
				
				area.addWidget(wtext);
				
				double x = Font.font.strLen(text.content, text.size) + 10;
				for(IDevCondition cond : skill.getDevConditions()) {
					ResourceLocation tex = cond.getIcon();
					if(tex != null) {
						Widget dep = new Widget();
						dep.transform.x = x;
						dep.transform.setSize(50, 50);
						dep.transform.alignHeight = HeightAlign.BOTTOM;
						
						DrawTexture dt = new DrawTexture();
						if(!cond.accepts(aData, skill))
							dt.setShaderId(ShaderMono.instance().getProgramID());
						dt.setTex(tex);
						dep.addComponent(dt);
						
						dep.regEventHandler(new FrameEventHandler() {

							final String text = cond.getHintText();
							
							@Override
							public void handleEvent(Widget w, FrameEvent event) {
								if(event.hovering) {
									glPushMatrix();
									final float fsize = 35;
									double flen = Font.font.strLen(text, fsize);
									
									glTranslated(-flen / 2, -30, 11);
									
									CRL_BACKGRND.bind();
									HudUtils.colorRect(0, 0, flen + 20, fsize + 20);
									
									glColor4f(1, 1, 1, 1);
									Font.font.draw(text, 10, 10, fsize, 0xffffffff);
									glPopMatrix();
								}
							}
							
						});
						
						area.addWidget(dep);
						
						x += 60;
					}
				}
				
				ret.addWidget(area);
			}
			
			{
				Widget area = new Widget();
				area.transform.setSize(450, 90);
				area.transform.doesListenKey = false;
				
				LearnButton button = new LearnButton();
				
				boolean can = LearningHelper.canLearn(aData, skill);
				if(can) {
					button.regEventHandler(new MouseDownHandler() {
	
						@Override
						public void handleEvent(Widget w, MouseDownEvent event) {
							overlay = new Overlay();
							window.addWidget(overlay);
							window.addWidget(createConfirmWidget(new DevelopTypeSkill(skill), 
								() -> {
									developer.reset();
									Syncs.startLearningSkill(developer, skill);
								}));
						}
						
					});
				} else {
					button.setDisabled();
				}
				area.addWidget(button);
				
				ret.addWidget(area);
			}
		}
		
		return ret;
	}
	
	Widget createConfirmWidget(IDevelopType type, ICallback callback) {
		Widget ret = loaded.getWidget("widgets/window_confirm").copy();
		
		double estmCons = developer.getEstmCons(type);
		boolean can = developer.getEstmCons(type) <= developer.getEnergy();
		
		Widget startButton = ret.getWidget("button_start");
		if(!can) {
			startButton.transform.doesListenKey = false;
			TextBox.get(startButton).color.setColor4d(.6, .6, .6, 1);
			TextBox.get(ret.getWidget("text_cons")).color.setColor4i(255, 66, 58, 255);
		} else {
			startButton.regEventHandler(new MouseDownHandler() {

				@Override
				public void handleEvent(Widget w, MouseDownEvent event) {
					ret.dispose();
					
					callback.invoke();
					window.addWidget(createProgressTracker(type));
				}
				
			});
		}
		
		TextBox.get(ret.getWidget("text_cons")).setContent(SkillTreeLocal.estmCons(estmCons));
		TextBox.get(ret.getWidget("text_content")).setContent(type.getName(player));
		
		ret.getWidget("button_cancel").regEventHandler(new MouseDownHandler() {

			@Override
			public void handleEvent(Widget w, MouseDownEvent event) {
				overlay.dispose();
				overlay = null;
				ret.dispose();
			}
			
		});
		
		return ret;
	}
	
	/**
	 * Will NOT start the action at SERVER side. Do it on your own.
	 */
	Widget createProgressTracker(IDevelopType type) {
		Widget ret = loaded.getWidget("widgets/window_tracker").copy();
		DrawTexture.get(ret.getWidget("icon_track")).setTex(type.getIcon(player));
		TextBox.get(ret.getWidget("text_content")).setContent(type.getName(player));
		
		Widget wProgText = ret.getWidget("text_progress");
		TextBox progText = TextBox.get(wProgText);
		progText.setContent(SkillTreeLocal.progress(0));
		
		ret.regEventHandler(new FrameEventHandler() {
			
			DevState state = DevState.IDLE;
			
			@Override
			public void handleEvent(Widget w, FrameEvent event) {
				progText.setContent(developer.getState().toString());
			}
			
		});
		
		return ret;
	}
	
	class LearnButton extends Widget {
		
		TextBox text;
		Glow glow;
		
		public LearnButton() {
			transform.setSize(142, 50);
			transform.alignWidth = WidthAlign.CENTER;
			transform.alignHeight = HeightAlign.BOTTOM;
			
			glow = new Glow();
			glow.color.setColor4i(255, 255, 255, 128);
			glow.glowSize = 12;
			
			Tint tint = new Tint();
			tint.idleColor.setColor4i(40, 40, 40, 255);
			tint.hoverColor.setColor4i(80, 80, 80, 255);
			
			text = new TextBox();
			text.heightAlign = HeightAlign.CENTER;
			text.widthAlign = WidthAlign.CENTER;
			text.size = 40;
			text.content = SkillTreeLocal.local("learn_skill");
			
			addComponent(glow);
			addComponent(tint);
			addComponent(text);
		}
		
		void setDisabled() {
			transform.doesListenKey = false;
			text.color.setColor4d(.6, .6, .6, 1);
			glow.color.setColor4d(1, 1, 1, 0.2);
		}
		
	}
	
	class Overlay extends Widget {
		
		Overlay() {
			transform.setSize(2000, 2000);
			transform.setCenteredAlign();
			
			DrawTexture dt = new DrawTexture();
			dt.texture = new ResourceLocation("<null>");
			dt.color.setColor4d(0, 0, 0, 0.3);
			
			addComponent(dt);
		}
		
	}

}
