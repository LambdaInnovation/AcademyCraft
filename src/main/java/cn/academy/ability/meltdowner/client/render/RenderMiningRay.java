/**
 * 
 */
package cn.academy.ability.meltdowner.client.render;

import cn.academy.ability.meltdowner.entity.EntityMdRayBase;
import cn.academy.ability.meltdowner.entity.EntityMiningRay;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@SideOnly(Side.CLIENT)
public class RenderMiningRay extends RenderMdRayBase<EntityMiningRay> {
	{
		this.setWidthFp(0.3);
		this.setWidthTp(0.5);
	}
}
