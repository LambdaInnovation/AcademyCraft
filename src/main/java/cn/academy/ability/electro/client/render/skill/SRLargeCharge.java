/**
 * 
 */
package cn.academy.ability.electro.client.render.skill;

import cn.academy.core.proxy.ACClientProps;

/**
 * @author WeathFolD
 *
 */
public class SRLargeCharge extends SRSmallCharge {

	{
		this.setTex(ACClientProps.ANIM_ARC_W);
	}
	
	public SRLargeCharge(int iten, double size) {
		super(iten, size);
	}

	public SRLargeCharge(int iten, double size, double sx, double sy, double sz) {
		super(iten, size, sx, sy, sz);
	}

}
