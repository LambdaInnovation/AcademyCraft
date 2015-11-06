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
package cn.academy.crafting.item;

import java.util.ArrayList;
import java.util.List;

import cn.academy.core.client.Resources;
import cn.academy.core.item.ACItem;
import cn.academy.crafting.api.event.MatterUnitHarvestEvent;
import cn.academy.crafting.client.render.item.RendererMatterUnit;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.util.mc.PlayerUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * The matter unit class. Have a simple material system for registration.
 * @author WeAthFolD
 */
public class ItemMatterUnit extends ACItem {
	
	@RegItem.Render
	@SideOnly(Side.CLIENT)
	public static RendererMatterUnit renderer;
	
	public static class MatterMaterial {
		
		public final String name;
		public final ResourceLocation texture;
		public final Block block;
		private int id;
		
		public MatterMaterial(String _name, Block block) {
			this(_name, block, Resources.getTexture("items/matter_unit/" + _name + "_mat"));
		}
		
		public MatterMaterial(String _name, Block _block, ResourceLocation tex) {
			name = _name;
			texture = tex;
			block = _block;
		}
		
	}
	
	private static List<MatterMaterial> materials = new ArrayList();
	
	public static final MatterMaterial NONE = new MatterMaterial("none", Blocks.air);
	static {
		addMatterMaterial(NONE);
	}
	
	public static void addMatterMaterial(MatterMaterial mat) {
		for(MatterMaterial prev : materials) {
			if(prev.name.equals(mat.name))
				throw new RuntimeException("Duplicate MatterMaterial Key " + mat.name);
		}
		mat.id = materials.size();
		materials.add(mat);
	}
	
	public static MatterMaterial getMatterMaterial(String name) {
		for(MatterMaterial mat : materials) {
			if(mat.name.equals(name))
				return mat;
		}
		return null;
	}

	//------
	
	
	public ItemMatterUnit() {
		super("matter_unit");
		setMaxStackSize(16);
		hasSubtypes = true;
	}
	
	public MatterMaterial getMaterial(ItemStack stack) {
		if(stack.getItem() != this || stack.getItemDamage() >= materials.size())
			return null;
		MatterMaterial mat = materials.get(stack.getItemDamage());
		if(mat == null) {
			setMaterial(stack, NONE);
			return NONE;
		}
		return mat;
	}

	public void setMaterial(ItemStack stack, MatterMaterial mat) {
		stack.setItemDamage(mat.id);
	}
	
	public void setMaterial(ItemStack stack, String name) {
		setMaterial(stack, getMatterMaterial(name));
	}
	
	public ItemStack create(String name) {
		return create(getMatterMaterial(name));
	}
	
	public ItemStack create(MatterMaterial mat) {
		ItemStack ret = new ItemStack(this);
		setMaterial(ret, mat);
		return ret;
	}
	
	@Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        boolean isNone = getMaterial(stack) == NONE;
        MovingObjectPosition mop = 
        	this.getMovingObjectPositionFromPlayer(world, player, true);

        if (mop == null) {
            return stack;
        } else {

            if (mop.typeOfHit == MovingObjectType.BLOCK) {
                int i = mop.blockX;
                int j = mop.blockY;
                int k = mop.blockZ;

                if (!world.canMineBlock(player, i, j, k)) {
                    return stack;
                }

                if (isNone) {
                    if (!player.canPlayerEdit(i, j, k, mop.sideHit, stack)) {
                        return stack;
                    }
                    
                    Block b = world.getBlock(i, j, k);
                    for(MatterMaterial m : materials) {
                    	if(m.block == b) {
                    		// Match, merge the stack.
                    		ItemStack newStack = new ItemStack(this);
                    		this.setMaterial(newStack, m);
                    		int left = PlayerUtils.mergeStackable(player.inventory, newStack);
                    		if(left > 0 && !world.isRemote) {
                    			newStack.stackSize = left;
                    			player.dropPlayerItemWithRandomChoice(newStack, false);
                    		}
                    		// --stackSize
                    		if(!player.capabilities.isCreativeMode) {
                    			stack.stackSize--;
                    		}
                    		// Clear block
                    		world.setBlockToAir(i, j, k);
                    		MinecraftForge.EVENT_BUS.post(new MatterUnitHarvestEvent(player, m));
                    		break;
                    	}
                    }
                }
            }

            return stack;
        }
    }
	
	@Override
    public String getUnlocalizedName(ItemStack stack) {
        return getUnlocalizedName() + "_" + getMaterial(stack).name;
    }
	
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item instance, CreativeTabs cct, List list) {
        for(MatterMaterial mat : materials) {
        	ItemStack stack = new ItemStack(this);
        	setMaterial(stack, mat);
        	list.add(stack);
        }
    }
	
}
