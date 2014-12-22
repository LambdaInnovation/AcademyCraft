/**
 * 
 */
package cn.academy.ability.electro;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cn.academy.api.ability.Category;
import cn.academy.api.ability.SkillBase;
import cn.academy.core.proxy.ACClientProps;

/**
 * @author WeathFolD
 *
 */
public class CatElectro extends Category {
	
	private static List<SkillBase> skills = new ArrayList<SkillBase>();
	static {
		
	}

	/**
	 * @param skills
	 */
	public CatElectro() {
		super(skills);
	}
	
	@SideOnly(Side.CLIENT)
	public ResourceLocation getLogo() {
		return ACClientProps.ELEC_LOGO;
	}

}
