package cn.academy.ability.api;

import net.minecraft.entity.player.EntityPlayer;

public abstract class Skill {
    public static Skill testSkill = new Skill() {};
    
    //TODO remove after test
    public static class SimpleSkillInstance extends SkillInstance {

        public SimpleSkillInstance(EntityPlayer player) {
            super(player);
        }
        
        @Override
        protected void onKeyUp() {
            //this.normalEnd();
        }
        
        @Override
        protected void onActionStarted() {
            this.addSubAction("entity", new cn.academy.ability.api.action.ClientEntityAction(player) {

                @Override
                protected net.minecraft.entity.Entity createEntity() {
                    net.minecraft.entity.Entity ret = new net.minecraft.entity.passive.EntityHorse(player.worldObj);
                    ret.setPosition(player.posX, player.posY, player.posZ);
                    return ret;
                }
                
            });
            this.schedule(20, new Runnable() {

                @Override
                public void run() {
                    normalEndNonSync();
                }
                
            });
        }
    }
    
    //TODO change to abstract after test
    public SkillInstance createSkillInstance(EntityPlayer player) {
        SkillInstance si = new SimpleSkillInstance(player);
        si.startSync();
        return si;
    }
    
}
