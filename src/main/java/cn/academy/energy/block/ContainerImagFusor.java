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
package cn.academy.energy.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import cn.liutils.cgui.gui.Widget;
import cn.liutils.cgui.gui.component.DrawTexture;
import cn.liutils.cgui.gui.event.FrameEvent;
import cn.liutils.cgui.gui.event.FrameEvent.FrameEventHandler;
import cn.liutils.util.helper.Color;

/**
 * @author WeAthFolD
 *
 */
public class ContainerImagFusor extends Container {

	public final TileImagFusor tile;
	public final EntityPlayer player;
	
	public ContainerImagFusor(TileImagFusor _tile, EntityPlayer _player) {
		tile = _tile;
		player = _player;
		
		initInventory(player.inventory);
	}
	
    private void initInventory(InventoryPlayer inv) {
    	this.addSlotToContainer(new Slot(tile, 0, 15, 31));
    	this.addSlotToContainer(new Slot(tile, 1, 79, 31));
        this.addSlotToContainer(new Slot(tile, 2, 32, 72));
        this.addSlotToContainer(new SlotIFItem(tile, 3, 67, 72));
        
        int STEP = 18;
        
        for(int i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(inv, i, 8 + i * STEP, 153));
        }
        
        for(int i = 1; i < 4; ++i) {
            for(int j = 0; j < 9; ++j) {
                int slot = (4 - i) * 9 + j;
                addSlotToContainer(new Slot(inv, slot, 8 + j * STEP, 149 - i * STEP));
            }
        }
    }
	
	@Override
	public boolean canInteractWith(EntityPlayer p_75145_1_) {
		return player.getDistanceSq(tile.xCoord + .5, tile.yCoord + .5, tile.zCoord + .5) < 64;
	}

}
