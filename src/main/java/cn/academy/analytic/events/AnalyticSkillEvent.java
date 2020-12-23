package cn.academy.analytic.events;

import cn.academy.ability.Skill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AnalyticSkillEvent extends Event {
    private String skillName;
    private EntityPlayer player;

    public AnalyticSkillEvent(EntityPlayer p, Skill s){
        skillName = s.getName();
        player = p;
    }


    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public EntityPlayer getPlayer() {
        return player;
    }

    public void setPlayer(EntityPlayer player) {
        this.player = player;
    }
}
