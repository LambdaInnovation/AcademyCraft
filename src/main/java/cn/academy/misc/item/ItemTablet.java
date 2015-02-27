/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.item;

import cn.academy.core.AcademyCraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Dev Tablet
 * TODO: Not added in β
 * @author KSkun
 */
@Deprecated
public class ItemTablet extends Item {
	
	private static final String[] unames = {"", "ability_tablet1", "ability_tablet2", "ability_tablet3"};

	int tabletID;

	public ItemTablet(int subID) {
		setCreativeTab(AcademyCraft.cct);
		tabletID = subID;
		this.setUnlocalizedName(unames[subID]);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister) {
		this.itemIcon = par1IconRegister.registerIcon("academy:tablet" + tabletID);
	}
	
}
