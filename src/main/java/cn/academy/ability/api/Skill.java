package cn.academy.ability.api;

import net.minecraft.entity.player.EntityPlayer;

public abstract class Skill {
    public static Skill testSkill = new Skill() {};
    
    //TODO remove after test
    public static class SimpleSkillInstance extends SkillInstance {

        public SimpleSkillInstance(EntityPlayer player) {
            super(player);
        }
        
        protected void onKeyUp() {
            this.normalEnd();
        }
        
    }
    
    //TODO change to abstract after test
    public SkillInstance createSkillInstance(EntityPlayer player) {
        SkillInstance si = new SimpleSkillInstance(player);
        si.startSync();
        return si;
    }
    
}
