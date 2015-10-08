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
package cn.academy.ability.client.render;

import cn.academy.core.client.Resources;
import cn.liutils.api.render.model.IItemModel;
import cn.liutils.api.render.model.ItemModelCustom;
import cn.liutils.template.client.render.item.RenderModelItem;
import cn.liutils.util.generic.VecUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

/**
 * @author WeAthFolD
 */
public class RenderDeveloperPortable extends RenderModelItem {

	public RenderDeveloperPortable() {
		super(new ItemModelCustom(Resources.getModel("developer_portable")), 
			Resources.getTexture("models/developer_portable"));
		renderInventory = false;
		this.scale = 6;
		this.equipRotation = VecUtils.vec(0, -10, -5);
		this.equipOffset = VecUtils.vec(0.6, 0, -.2);
		this.entityItemOffset.zCoord = 0.2;
		this.entityItemRotation.xCoord = 15;
		this.thirdPersonScale = 0.6;
		this.thirdPersonOffset = VecUtils.vec(0.1, 0.05, 0.2);
	}

}
