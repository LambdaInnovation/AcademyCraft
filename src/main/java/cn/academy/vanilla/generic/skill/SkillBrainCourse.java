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
package cn.academy.vanilla.generic.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.event.SkillExpAddedEvent;
import cn.academy.core.util.SubscribePipeline;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

/**
 * Generic skill: Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourse extends Skill {

    public SkillBrainCourse() {
        super("brain_course", 4);
        this.canControl = false;
        this.isGeneric = true;
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribePipeline("ability.maxcp")
    public float addMaxCP(float cp, EntityPlayer player) {
        if(AbilityData.get(player).isSkillLearned(this))
            cp += 1000;
        return cp;
    }
    
    @SubscribeEvent
    public void onExpAdded(SkillExpAddedEvent event) {
        AbilityData aData = event.getAbilityData();
        if(event.skill.canControl() && aData.isSkillLearned(this)) {
            aData.addSkillExp(this, event.amount * this.getFloat("incr_rate"));
        }
    }

}
