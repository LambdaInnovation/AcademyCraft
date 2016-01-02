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
package cn.academy.core.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.util.client.font.IFont;
import cn.lambdalib.util.client.font.IFont.FontOption;
import cn.lambdalib.util.helper.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.Category;
import cn.academy.ability.api.Controllable;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.Cooldown;
import cn.academy.ability.api.ctrl.Cooldown.CooldownData;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.AcademyCraft;
import cn.academy.core.ModuleCoreClient;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import cn.lambdalib.util.client.auxgui.AuxGui;
import cn.lambdalib.util.client.auxgui.AuxGuiHandler;
import cn.lambdalib.util.helper.Font;
import cn.lambdalib.util.key.KeyHandler;

/**
 * The overall debug console. use NUMPAD keys to switch between different states.
 * @author WeAthFolD
 */
@Registrant
public class DebugConsole extends AuxGui {
    
    private static class Text {
        final String text;
        final FontOption option;
        
        public Text(String _text, float _size, int _color) {
            text = _text;
            option = new FontOption(_size, new Color(_color));
        }
        
        public Text(String _text, float _size) {
            this(_text, _size, 0xffffffff);
        }
        
        public Text(String _text) {
            this(_text, 10);
        }
    }
    
    static DebugConsole INSTANCE;

    @RegInitCallback
    public static void init() {
        AuxGuiHandler.register(INSTANCE = new DebugConsole());
        ModuleCoreClient.keyManager.addKeyHandler("debug_console", Keyboard.KEY_F4, new KeyHandler() {
            @Override
            public void onKeyDown() {
                INSTANCE.enabled = !INSTANCE.enabled;
            }
        });
    }
    
    boolean enabled = AcademyCraft.DEBUG_MODE;
    
    private DebugConsole() {
        
    }
    
    @Override
    public boolean isForeground() {
        return false;
    }

    @Override
    public void draw(ScaledResolution sr) {
        if(!enabled)
            return;
        
        List<Text> texts = new ArrayList<>();
        texts.add(new Text("AcademyCraft developer info"));
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        
        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);
        if(!aData.isLearned()) {
            texts.add(new Text("Ability not acquired"));
        } else if(Keyboard.isKeyDown(Keyboard.KEY_BACK)) {
            texts.add(new Text("Skill status"));
            Category cat = aData.getCategory();
            for(Skill s : cat.getSkillList()) {
                StringBuilder sb = new StringBuilder(s.getName());
                for(int i = 0; i < 30 - s.getName().length(); ++i)
                    sb.append(' ');
                if(aData.isSkillLearned(s)) {
                    sb.append(String.format("%.1f", aData.getSkillExp(s) * 100)).append('%');
                } else {
                    sb.append("[not learned]");
                }
                
                texts.add(new Text(sb.toString()));
            }
            
        } else {
            texts.add(new Text(aData.getCategory().getName()));
            
            texts.add(new Text("Level " + aData.getLevel()));
            texts.add(new Text(String.format("CP:       %.0f/%.0f(%.1f+%.1f)", cpData.getCP(), cpData.getMaxCP(), cpData.getRawMaxCP(), cpData.getAddMaxCP())));
            texts.add(new Text(String.format("Overload: %.0f/%.0f(%.1f+%.1f)", cpData.getOverload(), cpData.getMaxOverload(), cpData.getRawMaxOverload(), cpData.getAddMaxOverload())));
            texts.add(new Text("CPData.canUseAbility: " + cpData.canUseAbility()));
            texts.add(new Text("CPData.activated: " + cpData.isActivated()));
            texts.add(new Text("CPData.addMaxCP: " + cpData.getAddMaxCP()));
            texts.add(new Text("CPData.interfering: " + cpData.isInterfering()));
            
            if(Cooldown.cooldown.size() != 0) {
                texts.add(new Text("Cooldown: "));
                for(Entry<Controllable, CooldownData> entry : Cooldown.cooldown.entrySet()) {
                    Controllable c = entry.getKey();
                    CooldownData data = entry.getValue();
                    String name = c.getHintText();
                    StringBuilder sb = new StringBuilder(name);
                    
                    for(int i = 0; i < 30 - name.length(); ++i)
                        sb.append(' ');
                    sb.append(String.format("%d/%dtick (%.1f/%.1fs)", 
                        data.getTickLeft(), data.getMaxTick(),
                        data.getTickLeft() / 20f, data.getMaxTick() / 20f));
                    
                    texts.add(new Text(sb.toString()));
                }
            }
        }
        
        texts.add(new Text(""));
        texts.add(new Text("[BACKSPACE]: Skill info", 10, 0x95a6ff));
        
        double x = 10, y = 10;
        IFont font = Resources.font();
        for(Text text : texts) {
            font.draw(text.text, x, y, text.option);
            
            y += text.option.fontSize;
        }
    }

}
