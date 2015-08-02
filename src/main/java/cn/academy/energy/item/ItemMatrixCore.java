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
package cn.academy.energy.item;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cn.academy.core.item.ACItem;
import cn.annoreg.core.Registrant;
import cn.liutils.util.generic.MathUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
public class ItemMatrixCore extends ACItem {
	
	int LEVELS = 3;
	
	IIcon icons[] = new IIcon[LEVELS];

	public ItemMatrixCore() {
		super("matrix_core");
		this.setHasSubtypes(true);
	}
	
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + "_" + stack.getItemDamage();
    }
	
    @SideOnly(Side.CLIENT)
    @Override
    public void registerIcons(IIconRegister ir) {
    	for(int i = 0; i < LEVELS; ++i) {
    		icons[i] = ir.registerIcon("academy:matrix_core_" + i);
    	}
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIconFromDamage(int meta) {
        return icons[MathUtils.wrapi(0, icons.length, meta)];
    }
    
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs cct, List list) {
    	for(int i = 0; i < LEVELS; ++i)
    		list.add(new ItemStack(this, 1, i));
    }

}
