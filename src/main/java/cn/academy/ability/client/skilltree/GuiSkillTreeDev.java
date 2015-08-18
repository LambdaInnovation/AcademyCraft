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

import static org.lwjgl.opengl.GL11.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.Developer;
import cn.academy.ability.developer.IDevCondition;
import cn.academy.core.client.component.Glow;
import cn.academy.core.client.glsl.ShaderMono;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.component.TextBox;
import cn.liutils.cgui.gui.component.Tint;
import cn.liutils.cgui.gui.component.Transform.HeightAlign;
import cn.liutils.cgui.gui.component.Transform.WidthAlign;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.cgui.gui.event.MouseDownEvent;
import cn.liutils.cgui.gui.event.MouseDownEvent.MouseDownHandler;
import cn.liutils.util.client.HudUtils;
import cn.liutils.util.helper.Font;

/**
 * @author WeAthFolD
 */
public class GuiSkillTreeDev extends GuiSkillTree {
	
	final Developer developer;

	public GuiSkillTreeDev(EntityPlayer _player, Developer _developer) {
		super(_player, _developer.type);
		developer = _developer;
	}
	
	@Override
	protected Widget createDesc(SkillHandler handler) {
		Skill skill = handler.skill;
		WidgetSkillDesc ret = new WidgetSkillDesc(handler);
		ret.addWidget(new GuiSkillTree.SkillLevelDesc(skill));
		
		if(aData.isSkillLearned(skill)) {
			ret.addWidget(new GuiSkillTree.SkillHint(ret));
		} else {
			{ // Dep display
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
				button.regEventHandler(new MouseDownHandler() {

					@Override
					public void handleEvent(Widget w, MouseDownEvent event) {
						// TODO
					}
					
				});
				area.addWidget(button);
				
				ret.addWidget(area);
			}
		}
		
		return ret;
	}
	
	class LearnButton extends Widget {
		
		public LearnButton() {
			transform.setSize(142, 50);
			transform.alignWidth = WidthAlign.CENTER;
			transform.alignHeight = HeightAlign.BOTTOM;
			
			Glow glow = new Glow();
			glow.color.setColor4i(255, 255, 255, 128);
			glow.glowSize = 12;
			
			Tint tint = new Tint();
			tint.idleColor.setColor4i(57, 57, 57, 255);
			tint.hoverColor.setColor4i(80, 80, 80, 255);
			
			TextBox text = new TextBox();
			text.heightAlign = HeightAlign.CENTER;
			text.widthAlign = WidthAlign.CENTER;
			text.size = 40;
			text.content = SkillTreeLocal.local("learn_skill");
			
			addComponent(glow);
			addComponent(tint);
			addComponent(text);
		}
		
	}

}
