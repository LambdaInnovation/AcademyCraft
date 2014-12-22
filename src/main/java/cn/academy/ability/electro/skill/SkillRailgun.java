/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.api.ctrl.pattern.PatternDown;
import cn.academy.core.proxy.ACClientProps;

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
				return false;
			}
			
		});
	}
	
	public String getInternalName() {
		return "em_railgun";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_RAILGUN;
	}

}
