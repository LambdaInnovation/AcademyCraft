package cn.academy.item;

import cn.academy.ACBlocks;
import cn.academy.Resources;
import cn.academy.event.MatterUnitHarvestEvent;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.PlayerUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The matter unit class. Have a simple material system for registration.
 * @author WeAthFolD
 */
public class ItemMatterUnit extends Item {
    
//    @RegItem.Render
//    @SideOnly(Side.CLIENT)
//    public static RendererMatterUnit renderer;
    
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
    
    private static List<MatterMaterial> materials = new ArrayList<>();
    
    public static final MatterMaterial MAT_NONE = new MatterMaterial("none", Blocks.AIR);
    public static final MatterMaterial MAT_PHASE_LIQUID = new MatterMaterial("phase_liquid", ACBlocks.imag_phase);

    static {
        // !!! Order is important here, only append at the end
        addMatterMaterial(MAT_NONE);
        addMatterMaterial(MAT_PHASE_LIQUID);
    }
    
    private static void addMatterMaterial(MatterMaterial mat) {
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
        setMaxStackSize(16);
        hasSubtypes = true;
        addPropertyOverride(new ResourceLocation("frame"), (stack, worldIn, entityIn) -> (int) (GameTimer.getTime() * 4) % 4);
    }
    
    public MatterMaterial getMaterial(ItemStack stack) {
        if(stack.getItem() != this || stack.getItemDamage() >= materials.size())
            return null;
        MatterMaterial mat = materials.get(stack.getItemDamage());
        if(mat == null) {
            setMaterial(stack, MAT_NONE);
            return MAT_NONE;
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
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        boolean isNone = getMaterial(stack) == MAT_NONE;
        RayTraceResult rayRes = rayTrace(world, player, isNone);

        if (rayRes == null || rayRes.typeOfHit== RayTraceResult.Type.MISS) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        } else {
            if (rayRes.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos pos = rayRes.getBlockPos();

                if (!world.canMineBlockBody(player, pos)) {
                    return new ActionResult<>(EnumActionResult.PASS, stack);
                }

                if (isNone) {
                    if (!player.canPlayerEdit(pos, rayRes.sideHit, stack)) {
                        return new ActionResult<>(EnumActionResult.PASS, stack);
                    }
                    
                    Block b = world.getBlockState(pos).getBlock();
                    for(MatterMaterial m : materials) {
                        if(m.block == b) {
                            // Match, merge the stack.
                            ItemStack newStack = new ItemStack(this);
                            this.setMaterial(newStack, m);
                            int left = PlayerUtils.mergeStackable(player.inventory, newStack);
                            if(left > 0 && !world.isRemote) {
                                newStack.setCount(left);
                                player.dropItem(newStack, false);
                            }
                            // --stackSize
                            if(!player.capabilities.isCreativeMode) {
                                stack.shrink(1);
                            }
                            // Clear block
                            world.setBlockToAir(pos);
                            MinecraftForge.EVENT_BUS.post(new MatterUnitHarvestEvent(player, m));
                            break;
                        }
                    }
                } else {
                    if (!player.canPlayerEdit(pos, rayRes.sideHit, stack)) {
                        return new ActionResult<>(EnumActionResult.PASS, stack);
                    }
                    Block b = world.getBlockState(pos).getBlock();
                    if(b.isReplaceable(world, pos)) {
                        world.setBlockState(pos, getMaterial(stack).block.getBlockState().getBaseState());
                    } else {
                        BlockPos npos = pos.offset(rayRes.sideHit);
                        world.setBlockState(npos, getMaterial(stack).block.getBlockState().getBaseState());
                    }
                    ItemStack newStack = new ItemStack(this);
                    this.setMaterial(newStack, MAT_NONE);
                    int left = PlayerUtils.mergeStackable(player.inventory, newStack);
                    if(left > 0 && !world.isRemote) {
                        newStack.setCount(left);
                        player.dropItem(newStack, true);
                    }
                    if(!player.capabilities.isCreativeMode) {
                        stack.shrink(1);
                    }
                    MinecraftForge.EVENT_BUS.post(new MatterUnitHarvestEvent(player, MAT_NONE));
                }
            }

            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
    }
    
    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey() + "_" + getMaterial(stack).name;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab))
            return;
        for(MatterMaterial mat : materials) {
            ItemStack stack = new ItemStack(this);
            setMaterial(stack, mat);
            items.add(stack);
        }
    }
    
}