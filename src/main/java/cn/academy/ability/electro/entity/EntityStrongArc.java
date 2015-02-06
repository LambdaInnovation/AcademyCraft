/**
 * 
 */
package cn.academy.ability.electro.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.RenderElecArc;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.data.AbilityData;
import cn.academy.api.data.AbilityDataMain;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity
@RegEntity.HasRender
public class EntityStrongArc extends EntityWeakArc {
	
	public static class OffSync extends EntityWeakArc.OffSync {

		public OffSync(EntityPlayer creator, SkillBase sag) {
			super(creator, sag);
		}
		
		@Override
		@SideOnly(Side.CLIENT)
		public ResourceLocation[] getTexs() {
			return ACClientProps.ANIM_ELEC_ARC_STRONG;
		}
		
	}
	
	@RegEntity.Render
	@SideOnly(Side.CLIENT)
	public static StrongRender renderer;

	public EntityStrongArc(EntityPlayer creator, SkillBase sag) {
		super(creator, sag);
		
		AbilityData data = AbilityDataMain.getData(creator);
		int skillID = data.getSkillID(sag);
		dmg = 12 + data.getSkillLevel(skillID) * 1.2F + data.getLevelID();
		igniteProb = 0.25 + 0.06 * data.getSkillLevel(skillID) + data.getLevelID() * 0.06;
		aoeRange = 7 + data.getSkillLevel(skillID) * 1;
		
	}

	@SideOnly(Side.CLIENT)
	public EntityStrongArc(World world) {
		super(world);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public ResourceLocation[] getTexs() {
		return ACClientProps.ANIM_ELEC_ARC_STRONG;
	}
	
	public static class StrongRender extends RenderElecArc {
		public StrongRender() {
			this.ratio = 2.0;
			this.width = 0.8;
		}
	}

}
