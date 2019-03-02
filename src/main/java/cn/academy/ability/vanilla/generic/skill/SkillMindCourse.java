package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Generic passive skill: Mind Training Course
 * @author WeAthFolD
 */
public class SkillMindCourse extends Skill {

    public SkillMindCourse() {
        super("mind_course", 5);
        this.canControl = false;
        this.isGeneric = true;
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcCPRecover(CalcEvent.CPRecoverSpeed evt) {
        if (AbilityData.get(evt.player).isSkillLearned(this)) {
            evt.value *= 1.2f;
        }
    }

}