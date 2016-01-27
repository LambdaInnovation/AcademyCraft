/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.skilltree;

import cn.academy.ability.develop.DeveloperType;
import cn.lambdalib.cgui.gui.Widget;
import cn.lambdalib.cgui.gui.component.DrawTexture;
import cn.lambdalib.cgui.gui.component.ProgressBar;
import cn.lambdalib.util.helper.Color;
import net.minecraft.entity.player.EntityPlayer;

/**
 * @author WeAthFolD
 */
public class GuiSkillTreeApp extends GuiSkillTree {
    
    static Color
        COLOR_PROG_MONO0 = new Color().setColor4i(106, 106, 106, 255),
        COLOR_PROG_NONO1 = new Color().setColor4i(127, 127, 127, 255);

    public GuiSkillTreeApp(EntityPlayer _player) {
        super(_player, DeveloperType.PORTABLE, true);
        initAppPage();
    }
    
    private void initAppPage() {
        ProgressBar p = ProgressBar.get(windowMachine.getWidget("p_energy"));
        p.color = DrawTexture.get(windowMachine.getWidget("i_energy")).color 
            = COLOR_PROG_MONO0;
        p.fluctRegion = 0;
        
        p = ProgressBar.get(windowMachine.getWidget("p_syncrate"));
        p.color = DrawTexture.get(windowMachine.getWidget("i_syncrate")).color
            = COLOR_PROG_NONO1;
        p.fluctRegion = 0;
    }
    
    @Override
    protected Widget createDesc(SkillHandler handler) {
        WidgetSkillDesc ret = new WidgetSkillDesc(handler);
        ret.addWidget(new GuiSkillTree.SkillLevelDesc(handler.skill));
        ret.addWidget(new GuiSkillTree.SkillHint(ret));
        return ret;
    }

}
