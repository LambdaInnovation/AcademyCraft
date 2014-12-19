/**
 * 
 */
package cn.academy.api.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import cn.academy.api.ability.Abilities;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.Level;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.AcademyCraftMod;

/**
 * @author WeathFolD
 *
 */
public class AbilityData implements IExtendedEntityProperties {
	
	public static final String IDENTIFIER = "ac_ability";
	
	private final EntityPlayer player;
	
	private int catID, level;
	private float currentCP;
	private float maxCP;
	private float skillExps[];
	private boolean skillOpens[];

	public AbilityData(EntityPlayer _player) {
		player = _player;
	}
	
	public static final void register(EntityPlayer player) {
	    player.registerExtendedProperties(AbilityData.IDENTIFIER, new AbilityData(player));
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
	
	public int getSkillCount() {
		Category cat = getCategory();
		return cat == null ? -1 : cat.getSkillCount();
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
			int ms = getSkillCount();
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
			int ms = getSkillCount();
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
		//this.player = (EntityPlayer) entity;
	}
	
	public final void sync() {
		AcademyCraftMod.netHandler.sendTo(new MsgSyncAbilityData(player), (EntityPlayerMP) player);
	}

}
