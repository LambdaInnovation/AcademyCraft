/**
 * 
 */
package cn.academy.ability.electro.skill;

import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.SkillBase;
import cn.academy.api.ctrl.RawEventHandler;
import cn.academy.core.proxy.ACClientProps;

/**
 * 吸引金属类方块而移动自身的能力，在原作中用来进行紧急回避，由于自伤太大所以较少使用……
 * TODO 施工中
 * TODO 关于自伤的考量？
 * @author WeathFolD
 */
public class SkillMagneticMovement extends SkillBase {

	public SkillMagneticMovement() {
	}
	
	@Override
	public void initPattern(RawEventHandler reh) {
		
	}
	
	public String getInternalName() {
		return "em_move";
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_MOVE;
	}

}
