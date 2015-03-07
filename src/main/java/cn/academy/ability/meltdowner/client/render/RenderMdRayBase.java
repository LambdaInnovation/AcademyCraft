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
package cn.academy.ability.meltdowner.client.render;

import net.minecraft.util.ResourceLocation;
import cn.academy.ability.meltdowner.entity.EntityMdRayBase;
import cn.academy.misc.client.render.RendererRayBlended;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderMdRayBase <T extends EntityMdRayBase> extends RendererRayBlended<T> {

	public RenderMdRayBase() {
		super(null,
			  null,
			 1);
	}
	
	@Override
	protected void drawAtOrigin(T ent, double len, boolean firstPerson) {
		ResourceLocation[] texData = ent.getTexData();
		int i = ent.ticksExisted % (texData.length - 1);
		this.tex = texData[i + 1];
		this.blendTex = texData[0];
		super.drawAtOrigin(ent, len, firstPerson);
	}
	
}