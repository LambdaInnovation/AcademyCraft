package cn.academy.analyticUtil;

import cn.academy.ability.Skill;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class AnalyticEvent extends Event {
    private String skillName;
    private String userName;

    public AnalyticEvent(EntityPlayer p, Skill s){
        userName = p.getName();
        skillName = s.getName();
    }


    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
