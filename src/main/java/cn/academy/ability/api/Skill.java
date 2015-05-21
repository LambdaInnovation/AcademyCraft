package cn.academy.ability.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.ctrl.Controllable;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.generic.client.Resources;

/**
 * Skill is the basic control unit of an ESPer. A skill is learned through Ability Developer
 * and can be activated/controlled via the Preset system. <br/>
 * A skill must be added into a Category, otherwise its instance is meaningless. <br/>
 * You should provide the SkillInstance for the skill via {@link Skill#createSkillInstance(EntityPlayer)}
 * method so that the skill control will take effect.
 * @author WeAthFolD
 */
public abstract class Skill implements Controllable {
	
	private Category category;
	
	private final String name;
	private ResourceLocation icon;
	
	public Skill(String _name) {
		name = _name;
	}
	
	final void addedIntoCategory(Category _category) {
		category = _category;
		
		icon = Resources.getTexture("abilities/" + category.getName() + "/skills/" + name);
		
		initSkill();
	}
	
	protected void initSkill() {}
	
	/**
	 * Get the direct name of the skill.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Get the full name of the skill, in format [category].[name].
	 */
	public String getFullName() {
		return category.getName() + "." + getName();
	}
	
	/**
	 * Get the display name of the skill.
	 */
	public String getDisplayName() {
		return getLocalized("name");
	}
	
	/**
	 * Get the detailed description for the skill, shown in Ability Developer.
	 */
	public String getDescription() {
		return getLocalized("desc");
	}
	
	@Override
	public ResourceLocation getHintIcon() {
		return icon;
	}

	@Override
	public String getHintText() {
		return getDisplayName();
	}
	
	protected String getLocalized(String key) {
		return StatCollector.translateToLocal("ac.ability." + getFullName() + "." + key);
	}
	
    public static Skill testSkill = new Skill("test") {

		@Override
		protected void initSkill() {
			// TODO Auto-generated method stub
			
		}};
    
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
    
    @Override
    public String toString() {
    	return getFullName();
    }
    
}
