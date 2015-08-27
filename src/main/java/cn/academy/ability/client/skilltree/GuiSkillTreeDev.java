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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.DevelopTypeLevel;
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
import cn.liutils.cgui.gui.component.Component;
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
	
	// A bit of hack. When reloading gui doesn't call developer#onGuiClosed callback
	boolean closeLock;
	
	final Developer developer;
	
	Overlay overlay;

	public GuiSkillTreeDev(EntityPlayer _player, Developer _developer) {
		super(_player, _developer.type);
		developer = _developer;
		
		EventLoader.load(gui, this);
		
		for(int i = 1; i <= 5; ++i) {
			final int j = i;
			treeArea.getWidget("ball" + i).regEventHandler(
					new MouseDownHandler() {

						@Override
						public void handleEvent(Widget w, MouseDownEvent event) {
							if(j == aData.getLevel() + 1 && LearningHelper.canLevelUp(type, aData)) {
								overlay = new Overlay();
								window.addWidget(overlay);
								window.addWidget(createConfirmWidget(new DevelopTypeLevel(), 
									() -> {
										developer.reset();
										Syncs.startUpgradingLevel(developer);
									}));
							}
						}
						
					});
		}
		
		ProgressBar.get(window.getWidget("window_machine/p_syncrate")).progress = developer.type.syncRate;
	}
	
	@Override
	public void onGuiClosed() {
		if(!closeLock)
			developer.onGuiClosed();
	}
	
	@GuiCallback("window/window_machine/p_energy")
	public void updateEnergy(Widget w, FrameEvent event) {
		ProgressBar bar = ProgressBar.get(w);
		bar.progress = developer.getEnergy() / developer.getMaxEnergy();
	}
	
	@GuiCallback("window/window_machine/t_energy")
	public void updateEnergyText(Widget w, FrameEvent event) {
		TextBox text = TextBox.get(w);
		text.setContent(SkillTreeLocal.energyDesc(
			developer.getEnergy(), developer.getMaxEnergy()));
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
			
			boolean show = false;
			for(IDevCondition c : devConditions) {
				if(c.getIcon() != null) {
					show = true;
					break;
				}
			}
			
			if(show){ // Dep display
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
				
				BlendIn blend = new BlendIn();
				blend.timeOffset = -550;
				wtext.addComponent(blend);
				
				area.addWidget(wtext);
				
				double x = Font.font.strLen(text.content, text.size) + 10;
				
				int i = 0;
				for(IDevCondition cond : devConditions) {
					ResourceLocation tex = cond.getIcon();
					if(tex != null) {
						Widget dep = new Widget();
						dep.transform.x = x;
						dep.transform.setSize(50, 50);
						dep.transform.alignHeight = HeightAlign.BOTTOM;
						
						DrawTexture dt = new DrawTexture();
						if(!cond.accepts(aData, developer, skill))
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
						
						BlendIn bi = new BlendIn();
						bi.timeOffset = -600 - i * 200;
						dep.addComponent(bi);
						
						area.addWidget(dep);
						
						x += 60;
						
						i++;
					}
				}
				
				ret.addWidget(area);
			}
			
			{
				Widget area = new Widget();
				area.transform.setSize(450, 90);
				area.transform.doesListenKey = false;
				
				boolean can = LearningHelper.canLearn(aData, developer, skill);
				LearnButton button = new LearnButton(!can);
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
				System.out.println("CANCELED");
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
		
		Widget button = ret.getWidget("button");
		TextBox buttonText = TextBox.get(button);
		
		ProgressBar progbar = ProgressBar.get(ret.getWidget("prog"));
		
		ret.addComponent(
		new Component("Tracker") {
			
			int state = 0;
			
			{
				addEventHandler(new FrameEventHandler() {
					@Override
					public void handleEvent(Widget w, FrameEvent event) {
						DevState dstate = developer.getState();
						if(dstate == DevState.FAILED && state != 2) {
							state = 2;
							buttonText.setContent(SkillTreeLocal.ok());
							progText.setContent(SkillTreeLocal.aborted());
							progbar.color.setColor4i(244, 40, 40, 255);
						} else {
							if(state == 0 && dstate == DevState.DEVELOPING) {
								state = 1;
							}
							if(state == 1 && dstate == DevState.DEVELOPING) {
								progText.setContent(
									SkillTreeLocal.progress((double) developer.stim / developer.maxStim));
							}
							if(state == 1 && dstate == DevState.IDLE) {
								state = 3;
								progText.setContent(SkillTreeLocal.successful());
								buttonText.setContent(SkillTreeLocal.ok());
							}
						}
					}
					
				});
				
				button.regEventHandler(new MouseDownHandler() {

					@Override
					public void handleEvent(Widget w, MouseDownEvent event) {
						if(state == 2) {
							overlay.dispose();
							overlay = null;
							ret.dispose();
						} else if(state == 3) {
							// Dispose this gui completely and open a new one!
							closeLock = true;
							Minecraft.getMinecraft().displayGuiScreen(
									new GuiSkillTreeDev(player, developer));
						} else {
							Syncs.abort(developer);
						}
					}
					
				});
				
				progbar.widget.regEventHandler(new FrameEventHandler() {

					@Override
					public void handleEvent(Widget w, FrameEvent event) {
						if(state == 1) {
							progbar.progress = (double) developer.stim / developer.maxStim;
						} else if(state == 3) {
							progbar.progress = 1;
						}
					}
					
				});
			}
		});
		
		return ret;
	}
	
	class LearnButton extends Widget {
		
		TextBox text;
		Glow glow;
		
		public LearnButton(boolean disabled) {
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
			
			BlendIn blend = new BlendIn();
			blend.timeOffset = -700;
			
			addComponent(glow);
			addComponent(tint);
			addComponent(text);
			
			if(disabled)
				setDisabled();
			
			addComponent(blend);
		}
		
		private void setDisabled() {
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
