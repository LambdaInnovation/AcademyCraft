package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.learning.LearningCondition;
import cn.academy.ability.api.learning.RootLearningCondition;
import cn.academy.core.client.Resources;

import com.google.common.collect.ImmutableList;

/**
 * Skill is the basic control unit of an ESPer. A skill is learned through Ability Developer
 * and can be activated/controlled via the Preset system. <br/>
 * A skill must be added into a Category, otherwise its instance is meaningless. <br/>
 * You should provide the SkillInstance for the skill via {@link Skill#createSkillInstance(EntityPlayer)}
 * method so that the skill control will take effect.
 * @author WeAthFolD
 */
public abstract class Skill extends Controllable {
	
	private Category category;
	
	private final List<LearningCondition> learningConditions = new ArrayList();
	
	/**
	 * The parent skill of the skill. This is the upper level skill in the Skill Tree UI. If not specified, this skill is the root skill of the type.
	 */
	private Skill parent;
	private int id;
	
	private final String name;
	private ResourceLocation icon;
	
	private final int level;
	
	public Skill(String _name, int atLevel) {
		name = _name;
		level = atLevel;
	}
	
	final void addedSkill(Category _category, int id) {
		category = _category;
		this.id = id;
		
		icon = Resources.getTexture("abilities/" + category.getName() + "/skills/" + name);
		
		addLearningCondition(new RootLearningCondition());
		
		initSkill();
	}
	
	/**
	 * Called AFTER the skill is added into the category.
	 */
	protected void initSkill() {}
	
	public int getID() {
		return id;
	}
	
	public int getLevel() {
		return level;
	}
	
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
	
	//--- Learning
	public void setParent(Skill skill) {
		parent = skill;
	}
	
	public Skill getParent() {
		return parent;
	}
	
	public boolean isRoot() {
		return parent == null;
	}
	
	protected void addLearningCondition(LearningCondition cond) {
		learningConditions.add(cond);
	}
	
	/**
	 * Returns an immutable list of learning conditions of this skill.
	 */
	public List<LearningCondition> getLearningConditions() {
		return ImmutableList.copyOf(learningConditions);
	}
	
	//
	
    public static Skill testSkill = new Skill("test", 1) {

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
