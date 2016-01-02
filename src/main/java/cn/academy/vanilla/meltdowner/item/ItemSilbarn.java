
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
package cn.academy.vanilla.meltdowner.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cn.academy.core.client.Resources;
import cn.academy.core.item.ACItem;
import cn.academy.vanilla.meltdowner.entity.EntitySilbarn;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegItem;
import cn.lambdalib.template.client.render.item.RenderModelItem;
import cn.lambdalib.util.deprecated.ItemModelCustom;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Registrant
public class ItemSilbarn extends ACItem {
    
    @SideOnly(Side.CLIENT)
    @RegItem.Render
    public static RenderSilbarn render;
    
    public ItemSilbarn() {
        super("silbarn");
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if(!world.isRemote) {
            world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
            world.spawnEntityInWorld(new EntitySilbarn(player));
        }
        if(!player.capabilities.isCreativeMode)
            --stack.stackSize;
        return stack;
    }
    
    public static class RenderSilbarn extends RenderModelItem {

        public RenderSilbarn() {
            super(new ItemModelCustom(Resources.getModel("silbarn")), Resources.getTexture("models/silbarn"));
            this.renderInventory = false;
            this.setStdRotation(90, 0, 0);
            this.setEquipRotation(0, 90, 0);
            this.setEquipOffset(.5, 0.1, -.2);
        }
        
    }
    
}