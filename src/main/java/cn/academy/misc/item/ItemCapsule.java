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
package cn.academy.misc.item;

import cn.academy.core.AcademyCraft;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;

/**
 * Dev Capsule
 * TODO: Not added in β
 * @author KSkun
 */
@Deprecated
public class ItemCapsule extends Item {
	
	private static String[] uname = {"", "ability_capsule1", "ability_capsule2", "ability_capsule3"};
	
	int capsuleID;
	
	public ItemCapsule(int subID) {
		setCreativeTab(AcademyCraft.cct);
		capsuleID = subID;
		this.setUnlocalizedName(uname[subID]);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister par1IconRegister) {
		this.itemIcon = par1IconRegister.registerIcon("academy:capsule" + capsuleID);
	}
	
}
