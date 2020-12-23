package cn.academy.ability.vanilla.generic.skill;

import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.CalcEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Generic skill: Advanced Brain Course.
 * @author WeAthFolD
 */
public class SkillBrainCourseAdvanced extends Skill {

    public SkillBrainCourseAdvanced() {
        super("brain_course_advanced", 4);
        this.canControl = false;
        this.isGeneric = true;
        
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void recalcMaxCP(CalcEvent.MaxCP event) {
        if (learned(event.player)) {
            event.value += 1500;
        }
    }

    @SubscribeEvent
    public void recalcMaxOverload(CalcEvent.MaxOverload event) {
        if (learned(event.player)) {
            event.value += 100;
        }
    }

    private boolean learned(EntityPlayer player) {
        return AbilityData.get(player).isSkillLearned(this);
    }
}