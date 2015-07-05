/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.electromaster.entity;

import net.minecraft.entity.player.EntityPlayer;
import cn.academy.vanilla.electromaster.client.renderer.SubArc;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.entityx.EntityCallback;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;

/**
 * @author WeAthFolD
 */
@Registrant
@RegEntity
public class EntityIntensifyEffect extends EntitySurroundArc {

	public EntityIntensifyEffect(EntityPlayer player) {
		super(player);
		
		this.setArcType(ArcType.THIN);
		initEvents();
	}
	
	private void initEvents() {
		genAtHt(2, 0);
		genAtHt(1.8, 1);
		genAtHt(1.5, 3);
		genAtHt(1, 4);
		genAtHt(0.5, 6);
		genAtHt(0, 7);
		genAtHt(-0.1, 8);
		
		this.life = 15;
	}
	
	// Disable the original generation
	@Override
	protected void doGenerate() {}
	
	private void genAtHt(double ht, int after) {
		this.executeAfter(new EntityCallback<EntityIntensifyEffect>() {

			@Override
			public void execute(EntityIntensifyEffect target) {
				//arcHandler.clear();
				int gen = RandUtils.rangei(3, 4);
				while(gen-- > 0) {
					double phi = RandUtils.ranged(0.5, 0.6);
					double theta = RandUtils.ranged(0, Math.PI * 2);
					SubArc arc = arcHandler.generateAt(
						VecUtils.vec(phi * Math.sin(theta), ht, phi * Math.cos(theta)));
					arc.life = 3;
				}
			}
			
		}, after);
	}

}
