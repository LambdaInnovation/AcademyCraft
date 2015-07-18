package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.api.learning.LearningCondition;
import cn.academy.ability.api.learning.RootLearningCondition;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.liutils.ripple.ScriptFunction;
import cn.liutils.ripple.ScriptNamespace;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Skill is the basic control unit of an ESPer. A skill is learned through Ability Developer
 * and can be activated/controlled via the Preset system. <br/>
 * A skill must be added into a Category, otherwise its instance is meaningless. <br/>
 * 
 * A skill can be specified to not appear in Preset Edit screen. This kind of skills usually serve as 'passive' skills and provide
 *  pipeline functions inside to affect the skill damage or other values. <br/>
 * 
 * You should provide the SkillInstance for the skill via {@link Skill#createSkillInstance(EntityPlayer)}
 * method so that the skill control will take effect.
 * 
 * @see cn.academy.core.util.ValuePipeline
 * @author WeAthFolD
 */
public abstract class Skill extends Controllable {
	
	private Category category;
	
	private final List<LearningCondition> learningConditions = new ArrayList();
	
	private String fullName;
	
	/**
	 * The parent skill of the skill. This is the upper level skill in the Skill Tree UI. If not specified, this skill is the root skill of the type.
	 */
	private Skill parent;
	private int id;
	
	private final String name;
	private ResourceLocation icon;
	
	private final int level;
	
	private ScriptNamespace script;
	
	/**
	 * Whether this skill can be controlled (i.e. appear in preset edit ui).
	 */
	protected boolean canControl = true;
	
	/**
	 * Whether this skill is a generic skill (Skill used across many categories). 
	 * If set to true, the logo lookup path and the name lookup path will be changed. (CategoryName="generic")
	 */
	protected boolean isGeneric = false;
	
	/**
	 * @param _name Skill internal name
	 * @param atLevel The level at which this skill is in
	 */
	public Skill(String _name, int atLevel) {
		name = _name;
		level = atLevel;
		fullName = "<unassigned>." + name;
		
		AcademyCraft.pipeline.register(this);
	}
	
	final void addedSkill(Category _category, int id) {
		category = _category;
		this.id = id;
		
		icon = Resources.getTexture("abilities/" + category.getName() + "/skills/" + name);
		
		addLearningCondition(new RootLearningCondition());
		
		fullName = (isGeneric ? "generic" : category.getName()) + "." + name;
		
		script = AcademyCraft.script.at("ac." + fullName);
		
		initSkill();
	}
	
	/**
	 * Called AFTER the skill is added into the category.
	 */
	protected void initSkill() {}
	
	/**
	 * Get the id of the skill in the Category.
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Get the level id that this skill is in.
	 */
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
		return fullName;
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
	
	public boolean canControl() {
		return canControl;
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
	
	//--- Ctrl
	@Override
	@SideOnly(Side.CLIENT)
    public SkillInstance createSkillInstance(EntityPlayer player) {
		return null;
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
	
	/**
	 * @return The stimulation in the developer required in order to learn this skill
	 */
	public int getLearningStims() {
		return AcademyCraft.getFunction("ability.learning_cost").callInteger(level);
	}
	
	/**
	 * @return The minimum developer type that this skill will appear on
	 */
	public DeveloperType getMinimumDeveloperType() {
		if(level <= 1) // Level 1 and 2
			return DeveloperType.PORTABLE;
		if(level <= 2) // Level 3
			return DeveloperType.NORMAL;
		else // Level 4 and 5
			return DeveloperType.ADVANCED;
	}
    
    @Override
    public String toString() {
    	return getFullName();
    }
    
    //---Script integration
    // TODO: Unify the pipeline call
    
    protected ScriptFunction getFunc(String name) {
    	return script.getFunction(name);
    }
    
    /**
     * The most commonly used script operation. Pass the skill exp of this skill as argument into the function [name].
     */
    protected float callFloatWithExp(String name, AbilityData data) {
    	return getFunc(name).callFloat(data.getSkillExp(this));
    }
    
    protected int callIntWithExp(String name, AbilityData data) {
    	return getFunc(name).callInteger(data.getSkillExp(this));
    }
    
    protected float getFloat(String name) {
    	return script.getFloat(name);
    }
    
    protected double getDouble(String name) {
    	return script.getDouble(name);
    }
    
    //---Pipeline integration
    protected float pipeFloat(String key, float value, Object ...params) {
    	return AcademyCraft.pipeline.pipeFloat(getFullName() + "." + key, value, params);
    }
    
    protected double pipeDouble(String key, double value, Object ...params) {
    	return AcademyCraft.pipeline.pipeDouble(getFullName() + "." + key, value, params);
    }
    
    protected int pipeInt(String key, int value, Object ...params) {
    	return AcademyCraft.pipeline.pipeInt(getFullName() + "." + key, value, params);
    }
    
    // Subclass sandbox
    protected float getConsumption(AbilityData data) {
    	return pipeFloat("cp", callFloatWithExp("consumption", data), data.getPlayer());
    }
    
    protected float getOverload(AbilityData data) {
    	return pipeFloat("overload", callFloatWithExp("overload", data), data.getPlayer());
    }
    
    protected int getCooldown(AbilityData data) {
    	return pipeInt("cooldown", callIntWithExp("cooldown", data), data.getPlayer());
    }
    
}
