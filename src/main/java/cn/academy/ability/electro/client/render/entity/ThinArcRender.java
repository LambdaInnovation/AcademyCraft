/**
 * 
 */
package cn.academy.ability.electro.client.render.entity;

import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
public class ThinArcRender extends RenderElecArc {

	{
		widthFp = 0.3;
		widthTp = 0.6;
	}

}
