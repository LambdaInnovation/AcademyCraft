/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.util.ResourceLocation;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.core.proxy.ACClientProps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 强电弧（电弧束）攻击
 * @author WeathFolD
 */
public class SkillStrongArc extends SkillBase {

	public SkillStrongArc() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
	}
	
	public String getInternalName() {
		return "em_arc";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_ARC_STRONG;
	}

}
