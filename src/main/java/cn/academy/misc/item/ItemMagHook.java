/**
 * 
 */
package cn.academy.misc.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.academy.misc.entity.EntityMagHook;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cn.liutils.api.render.model.ItemModelCustom;
import cn.liutils.template.client.render.item.RenderModelItem;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class ItemMagHook extends Item {
	
	@RegItem.Render
	public static HookRender renderer;

	public ItemMagHook() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ac_maghook");
		setTextureName("academy:maghook");
		this.setFull3D();
	}
	
    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    	if(!world.isRemote) {
    		world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
    		world.spawnEntityInWorld(new EntityMagHook(player));
    		if(!player.capabilities.isCreativeMode)
    			stack.damageItem(1, player);
    	}
        return stack;
    }
    
    public static class HookRender extends RenderModelItem {

		public HookRender() {
			super(new ItemModelCustom(ACModels.MDL_MAGHOOK), ACClientProps.TEX_MDL_MAGHOOK);
			this.setScale(0.15d);
			this.setStdRotation(0, -90, 90);
			this.setOffset(0, 0.0, -3);
			this.setEquipOffset(1, 0, 0);
		}
		
		@Override
		protected void renderAtStdPosition(float i) {
			this.setOffset(0, 0, 1);
			this.setEquipOffset(0.5, 0.1, 0);
			super.renderAtStdPosition(i);
		}
    	
    }

}
