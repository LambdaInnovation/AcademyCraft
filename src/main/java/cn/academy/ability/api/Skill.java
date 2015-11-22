package cn.academy.ability.api;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.ability.develop.condition.DevConditionDep;
import cn.academy.ability.develop.condition.DevConditionDeveloperType;
import cn.academy.ability.develop.condition.DevConditionLevel;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.condition.IDevCondition;
import cn.academy.core.AcademyCraft;
import cn.academy.core.client.Resources;
import cn.academy.misc.achievements.ModuleAchievements;
import cn.lambdalib.ripple.ScriptFunction;
import cn.lambdalib.ripple.ScriptNamespace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**
 * Skill is the basic control unit of an ESPer. A skill is learned through Ability Developer
 * and can be activated/controlled via the Preset system. <br/>
 * A skill must be added into a Category, otherwise its presence is meaningless. <br/>
 * 
 * A skill can be specified to not appear in Preset Edit screen. This kind of skills usually serve as 'passive' skills and provide
 *  pipeline functions inside to affect the skill damage or other values. <br/>
 * 
 * You should provide the SkillInstance for the skill via {@link Skill#createSkillInstance(EntityPlayer)}
 * method so that the skill control will take effect.
 * 
 * @see cn.academy.core.util.ValuePipeline
 * @see cn.academy.ability.api.ctrl.SkillInstance
 * @see cn.academy.ability.api.ctrl.SyncAction
 * @author WeAthFolD
 */
public abstract class Skill extends Controllable {
	
	private Category category;
	
	private final List<IDevCondition> learningConditions = new ArrayList();
	
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
	 * The place this skill is at in the Skill Tree UI. This field is automatically loaded from script from field
	 * "x" and "y"
	 */
	public double guiX, guiY;
	
	/**
	 * Whether this skill has customized experience definition.
	 * If this is set to true, getSkillExp() will be called whenever
	 * 	querying experience of skill.
	 */
	public boolean expCustomized = false;
	
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
		
		addDevCondition(new DevConditionLevel());
	}
	
	final void addedSkill(Category _category, int id) {
		category = _category;
		this.id = id;
		
		icon = initIcon();
		fullName = initFullName();
		script = initScript();
		
		try {
			float x = script.getFloat("x"), y = script.getFloat("y");
			guiX = x;
			guiY = y;
		} catch(Exception e) {
			AcademyCraft.log.error("Failed to load gui position of skill " + fullName);
		}
		
		this.addDevCondition(new DevConditionDeveloperType(getMinimumDeveloperType()));
		
		initSkill();
	}
	
	/**
	 * Callback that is called AFTER the skill is added into the category.
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
	
	//--- Path init
	protected String getCategoryLocation() {
		return (isGeneric ? "generic" : category.getName());
	}
	
	/**
	 * @return The init full name. Is guaranteed to be called AFTER the Category is assigned.
	 */
	protected String initFullName() {
		return getCategoryLocation() + "." + name;
	}
	
	/**
	 * @return The icon of this skill. Is guaranteed to be called AFTER the Category is assigned.
	 */
	protected ResourceLocation initIcon() {
		return icon = Resources.getTexture("abilities/" + getCategoryLocation() + "/skills/" + name);
	}
	
	/**
	 * @return The ScriptNamespace of this skill. 
	 * 	Is guaranteed to be called AFTER the full name of the skill is assigned.
	 */
	protected ScriptNamespace initScript() {
		return AcademyCraft.getScript().at("ac." + fullName);
	}
	
	//--- Hooks
	/**
	 * Get called when set expCustomize=true, to query the experience of the skill.
	 * @param data
	 * @return exp value in [0, 1]
	 */
	public float getSkillExp(AbilityData data) {
		return 0.0f;
	}
	
	//--- Ctrl
	@Override
	@SideOnly(Side.CLIENT)
    public SkillInstance createSkillInstance(EntityPlayer player) {
		return null;
	}
	
	//--- Learning
	public void setParent(Skill skill) {
		setParent(skill, 0.0f);
	}
	
	public void setParent(Skill skill, float requiredExp) {
		if(parent != null)
			throw new IllegalStateException("You can't set the parent twice!");
		parent = skill;
		this.addDevCondition(new DevConditionDep(parent, requiredExp));
	}
	
	public Skill getParent() {
		return parent;
	}
	
	public boolean isRoot() {
		return parent == null;
	}
	
	public void addDevCondition(IDevCondition cond) {
		learningConditions.add(cond);
	}
	
	public void addSkillDep(Skill skill, float exp) {
		addDevCondition(new DevConditionDep(skill, exp));
	}
	
	/**
	 * Returns an immutable list of learning conditions of this skill.
	 */
	public List<IDevCondition> getDevConditions() {
		return ImmutableList.copyOf(learningConditions);
	}
	
	/**
	 * @return The stimulation in the developer required in order to learn this skill
	 */
	public int getLearningStims() {
		return AcademyCraft.getFunction("ability.learning.learning_cost")
				.callInteger(level);
	}
	
	/**
	 * @return The minimum developer type that this skill will appear on
	 */
	public DeveloperType getMinimumDeveloperType() {
		if(level <= 2) // Level 1 and 2
			return DeveloperType.PORTABLE;
		if(level <= 3) // Level 3
			return DeveloperType.NORMAL;
		else // Level 4 and 5
			return DeveloperType.ADVANCED;
	}
    
    @Override
    public String toString() {
    	return getFullName();
    }
    
    //---Script integration
    
    protected ScriptFunction getFunc(String name) {
    	return script.getFunction(name);
    }
    
    /**
     * Fetch a float value from a function call in script and pipeline it.
     * @param args the arguments passed into the SCRIPT FUNCTION
     */
    protected float getFloatPiped(String key, EntityPlayer player, Object... args) {
    	return AcademyCraft.pipeline.pipeFloat(getFullName() + "." + key, 
    		getFunc(key).callFloat(args), player);
    }
    
    protected int getIntPiped(String key, EntityPlayer player, Object... args) {
    	return AcademyCraft.pipeline.pipeInt(getFullName() + "." + key, 
    		getFunc(key).callInteger(args), player);
    }
    
    /**
     * The most commonly used script operation. Pass the skill exp of this skill as argument into the function [name].
     */
    protected float callFloatWithExp(String name, AbilityData data) {
    	return getFloatPiped(name, data.getEntity(), data.getSkillExp(this));
    }
    
    protected int callIntWithExp(String name, AbilityData data) {
    	return getIntPiped(name, data.getEntity(), data.getSkillExp(this));
    }
    
    protected float getFloat(String name) {
    	return script.getFloat(name);
    }
    
    protected int getInt(String name) {
    	return script.getInteger(name);
    }
    
    //---Pipeline integration
    protected float pipeFloat(String key, float value, Object ...params) {
    	return AcademyCraft.pipeline.pipeFloat(getFullName() + "." + key, value, params);
    }
    
    protected int pipeInt(String key, int value, Object ...params) {
    	return AcademyCraft.pipeline.pipeInt(getFullName() + "." + key, value, params);
    }
    
    // Subclass sandbox
    protected float getConsumption(AbilityData data) {
    	return pipeFloat("cp", callFloatWithExp("consumption", data), data.getEntity());
    }
    
    protected float getOverload(AbilityData data) {
    	return pipeFloat("overload", callFloatWithExp("overload", data), data.getEntity());
    }
    
    protected int getCooldown(AbilityData data) {
    	return pipeInt("cooldown", callIntWithExp("cooldown", data), data.getEntity());
    }
    
    /**
     * Trigger the achievement in vanilla achievement page, if any.
     * @param player
     */
    protected void triggerAchievement(EntityPlayer player) {
    	ModuleAchievements.trigger(player, getFullName());
    }
    
}
