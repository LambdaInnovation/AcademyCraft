package cn.academy.terminal.app;

import cn.academy.ability.client.ui.SkillTreeAppUI;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.RegApp;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class AppSkillTree extends App {
    
    @RegApp
    public static AppSkillTree instance = new AppSkillTree();

    public AppSkillTree() {
        super("skill_tree");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @SideOnly(Side.CLIENT)
            @Override
            public void onStart() {
                Minecraft.getMinecraft().displayGuiScreen(SkillTreeAppUI.apply());
            }
        };
    }

}