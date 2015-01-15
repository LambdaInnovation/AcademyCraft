/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cn.academy.ability.electro.entity.EntityRailgun;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.internal.PatternDown;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 传说中的超电磁炮~~！
 * TODO 施工中
 * @author WeathFolD
 */
public class SkillRailgun extends SkillBase {

	public SkillRailgun() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		reh.addPattern(new PatternDown() {

			@Override
			public boolean onKeyDown(EntityPlayer player) {
				if(!player.worldObj.isRemote) {
					System.out.println("Entity spawned");
					player.worldObj.spawnEntityInWorld(new EntityRailgun(player, 10));
				}
				return true;
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_railgun";
	}
	
	public int getMaxSkillLevel() {
		return 200;
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}

}
