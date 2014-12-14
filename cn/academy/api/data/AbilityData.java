/**
 * 
 */
package cn.academy.api.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;

/**
 * @author WeathFolD
 *
 */
public class AbilityData implements IExtendedEntityProperties {
	
	public static final String IDENTIFIER = "ac_ability";
	
	private EntityPlayer player;
	
	protected int catID, level;
	protected float currentCP;
	protected float maxCP;
	protected float skillExps[];
	protected boolean skillOpens[];

	public AbilityData(EntityPlayer _player) {
		player = _player;
	}
	
	public Category getCategory() {
		return Abilities.getCategory(catID);
	}
	
	public int getCategoryID() {
		return catID;
	}
	
	public Level getLevel() {
		return getCategory().getLevel(level);
	}
	
	public int getLevelID() {
		return level;
	}
	
	public SkillBase getSkill(int sid) {
		return getCategory().getSkill(sid);
	}
	
	public int getMaxSkills() {
		Category cat = getCategory();
		return cat == null ? -1 : cat.getMaxSkills();
	}
	
	public float getSkillExp(int sid) {
		return skillExps == null ? -1 : skillExps[sid];
	}
	
	public boolean isSkillLearned(int sid) {
		return skillOpens == null ? false : skillOpens[sid];
	}
	
	public float getCurrentCP() {
		return currentCP;
	}
	
	public float getMaxCP() {
		return maxCP;
	}
	
	public EntityPlayer getPlayer() {
		return player;
	}

	@Override
	public void saveNBTData(NBTTagCompound nbt) {
		nbt.setInteger("catid", catID);
		nbt.setInteger("level", level);
		if(getCategory() != null) {
			//Store exps and activate stats
			nbt.setFloat("ccp", currentCP);
			nbt.setFloat("mcp", maxCP);
			int ms = getMaxSkills();
			for(int i = 0; i < ms; ++i) {
				nbt.setFloat("exp_" + i, skillExps[i]);
				nbt.setBoolean("open_" + i, skillOpens[i]);
			}
		}
	}

	@Override
	public void loadNBTData(NBTTagCompound nbt) {
		catID = nbt.getInteger("catid");
		level = nbt.getInteger("level");
		if(getCategory() != null) {
			currentCP = nbt.getFloat("ccp");
			maxCP = nbt.getFloat("mcp");
			int ms = getMaxSkills();
			skillExps = new float[ms];
			skillOpens = new boolean[ms];
			for(int i = 0; i < ms; ++i) {
				skillExps[i] = nbt.getFloat("exp_" + i);
				skillOpens[i] = nbt.getBoolean("open_" + i);
			}
		}
	}

	@Override
	public void init(Entity entity, World world) {
		this.player = (EntityPlayer) entity;
	}

}
