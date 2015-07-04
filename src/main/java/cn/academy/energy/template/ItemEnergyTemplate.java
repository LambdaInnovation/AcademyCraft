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
package cn.academy.energy.template;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.item.ImagEnergyItem;
import cn.liutils.loading.Loader.ObjectNamespace;
import cn.liutils.loading.item.ItemLoadRule;
import cn.liutils.loading.item.ItemLoadRuleProvider;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
public class ItemEnergyTemplate extends Item implements ImagEnergyItem, ItemLoadRuleProvider {
	
	protected static IFItemManager itemManager = IFItemManager.instance;
	
	public String name;
	
	public double maxEnergy;
	public double latency;
	
	IIcon iconEmpty, iconHalf, iconFull;
	
	public ItemEnergyTemplate() {
		setMaxDamage(13);
	}
	
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister ir) {
    	iconEmpty = ir.registerIcon("academy:" + name + "_empty");
    	iconHalf = ir.registerIcon("academy:" + name + "_half");
    	iconFull = ir.registerIcon("academy:" + name + "_full");
    }

    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
    	if(damage < 3) {
    		return iconFull;
    	}
    	if(damage > 10) {
    		return iconEmpty;
    	}
    	return iconHalf;
    }
    
	@Override
	public double getMaxEnergy() {
		return maxEnergy;
	}

	@Override
	public double getLatency() {
		return latency;
	}
	
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs cct, List list) {
    	ItemStack is = new ItemStack(this);
    	list.add(is);
    	itemManager.charge(is, 0, true);
    	
    	is = new ItemStack(this);
    	itemManager.charge(is, Double.MAX_VALUE, true);
    	list.add(is);
    }
    
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean wtf) {
    	list.add(itemManager.getDescription(stack));
    }

	@Override
	public ItemLoadRule[] getRules() {
		return new ItemLoadRule[] {
			new ItemLoadRule() {
				@Override
				public void load(Item item, ObjectNamespace ns, String name)
						throws Exception {
					maxEnergy = ns.getDouble("maxEnergy");
					latency = ns.getDouble("latency");
					
					ItemEnergyTemplate.this.name = name;
				}
			}
		};
	}

}
