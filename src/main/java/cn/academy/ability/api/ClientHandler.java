package cn.academy.ability.api;

import net.minecraft.client.Minecraft;

import org.lwjgl.input.Keyboard;

import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.api.key.IKeyHandler;
import cn.liutils.api.key.LIKeyProcess;
import cn.liutils.util.ClientUtils;

/**
 * Key event listener for skill events.
 * @author acaly
 *
 */
@Registrant
@RegSubmoduleInit
public final class ClientHandler {
    
    public static void init() {
        
        LIKeyProcess.instance.addKey("Skill 1", Keyboard.KEY_R, false, new IKeyHandler() {
            
            SkillInstance down;
            
            @Override
            public void onKeyDown(int keyCode, boolean tickEnd) {
                if(tickEnd || !ClientUtils.isPlayerInGame()) return;
                //TODO check preset ready and skill enabled
                down = Skill.testSkill.createSkillInstance(Minecraft.getMinecraft().thePlayer);
            }

            @Override
            public void onKeyUp(int keyCode, boolean tickEnd) {
                if (down != null) {
                    down.onClientKeyUp();
                    down = null;
                }
            }

            @Override
            public void onKeyTick(int keyCode, boolean tickEnd) {
            }
            
        });
        
    }
}
