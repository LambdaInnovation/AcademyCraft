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
import cn.academy.api.ctrl.pattern.PatternHold;
import cn.academy.api.ctrl.pattern.PatternHold.State;
import cn.academy.core.proxy.ACClientProps;

/**
 * 一般电弧攻击
 * @author WeathFolD
 *
 */
public class SkillElecArc extends SkillBase {

	public SkillElecArc() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
	}
	
	public String getInternalName() {
		return "em_arc";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_ARC;
	}

}
