package cn.academy.client.auxgui;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.util.ACKeyManager;
import cn.academy.Resources;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * The overall debug console. Use BACKSPACE to switch between states.
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class DebugConsole extends AuxGui {
    
    private static class Text {
        final String text;
        final FontOption option;
        
        public Text(String _text, float _size, int _color) {
            text = _text;
            option = new FontOption(_size, Colors.fromRGBA32(_color));
        }
        
        public Text(String _text, float _size) {
            this(_text, _size, 0xffffffff);
        }
        
        public Text(String _text) {
            this(_text, 10);
        }
    }
    
    static DebugConsole INSTANCE;

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        AuxGuiHandler.register(INSTANCE = new DebugConsole());
        ACKeyManager.instance.addKeyHandler("debug_console", Keyboard.KEY_F4, new KeyHandler() {
            @Override
            public void onKeyDown() {
                State[] states = State.values();
                INSTANCE.state = states[(INSTANCE.state.ordinal() + 1) % states.length];
            }
        });
    }

    enum State { NONE, NORMAL, SHOW_EXP }

    State state = State.NONE;
    
    private DebugConsole() {
        foreground = false;
    }
    
    @Override
    public void draw(ScaledResolution sr) {
        if(state == State.NONE)
            return;
        
        List<Text> texts = new ArrayList<>();
        texts.add(new Text("AcademyCraft developer info"));
        EntityPlayer player = Minecraft.getMinecraft().player;
        
        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);

        switch (state) {
        case NORMAL:
            if (!aData.hasCategory()) {
                texts.add(new Text("Ability not acquired"));
            } else {
                texts.add(new Text(aData.getCategory().getName()));

                texts.add(new Text("Level " + aData.getLevel()));
                texts.add(new Text(String.format("CP:       %.0f/%.0f(%.1f+%.1f)", cpData.getCP(), cpData.getMaxCP(), cpData.getRawMaxCP(), cpData.getAddMaxCP())));
                texts.add(new Text(String.format("Overload: %.0f/%.0f(%.1f+%.1f)", cpData.getOverload(), cpData.getMaxOverload(), cpData.getRawMaxOverload(), cpData.getAddMaxOverload())));
                texts.add(new Text("CPData.canUseAbility: " + cpData.canUseAbility()));
                texts.add(new Text("CPData.activated: " + cpData.isActivated()));
                texts.add(new Text("CPData.addMaxCP: " + cpData.getAddMaxCP()));
                texts.add(new Text("CPData.interfering: " + cpData.isInterfering()));
                texts.add(new Text(String.format(" AData.levelProgress: %.2f%%", aData.getLevelProgress() * 100)));
            }
            break;
        case SHOW_EXP:
            texts.add(new Text("Skill status"));
            if (aData.hasCategory()) {
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
            }
            break;
        }
        
        iter(texts, 10.5f, 10.5f, 0.2);
        iter(texts, 10, 10, 1);
    }

    private void iter(List<Text> texts, float x, float y, double lumMul) {
        IFont font = Resources.font();
        for(Text text : texts) {
            Color crl = text.option.color;
            Color prev = new Color(crl);

            crl.setRed((int) (crl.getRed() * lumMul));
            crl.setGreen((int) (crl.getGreen() * lumMul));
            crl.setBlue((int) (crl.getBlue() * lumMul));

            font.draw(text.text, x, y, text.option);

            crl.setColor(prev);

            y += text.option.fontSize;
        }
    }

}