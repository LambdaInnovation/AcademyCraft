package cn.academy.item;

import cn.academy.Resources;
import cn.academy.event.MatterUnitHarvestEvent;
import cn.academy.client.render.item.RendererMatterUnit;
import cn.lambdalib2.util.mc.PlayerUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The matter unit class. Have a simple material system for registration.
 * @author WeAthFolD
 */
public class ItemMatterUnit extends Item {
    
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
                } else {
                    if (!player.canPlayerEdit(i, j, k, mop.sideHit, stack)) {
                        return stack;
                    }
                    Block b = world.getBlock(i, j, k);
                    if(b.isReplaceable(world, i, j, k)) {
                        world.setBlock(i, j, k, getMaterial(stack).block);
                    } else {
                        switch(mop.sideHit) {
                            case 0:
                                j--;
                                break;
                            case 1:
                                j++;
                                break;
                            case 2:
                                k--;
                                break;
                            case 3:
                                k++;
                                break;
                            case 4:
                                i--;
                                break;
                            case 5:
                                i++;
                                break;
                        }
                        world.setBlock(i, j, k, this.getMaterial(stack).block);
                    }
                    ItemStack newStack = new ItemStack(this);
                    this.setMaterial(newStack, NONE);
                    int left = PlayerUtils.mergeStackable(player.inventory, newStack);
                    if(left > 0 && !world.isRemote) {
                        newStack.stackSize = left;
                        player.dropPlayerItemWithRandomChoice(newStack, false);
                    }
                    if(!player.capabilities.isCreativeMode) {
                        stack.stackSize--;
                    }
                    MinecraftForge.EVENT_BUS.post(new MatterUnitHarvestEvent(player, NONE));
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