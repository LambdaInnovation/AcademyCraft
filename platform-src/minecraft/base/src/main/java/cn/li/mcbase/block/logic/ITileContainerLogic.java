package cn.li.mcbase.block.logic;

import cn.li.mcbase.block.entity.IScriptedBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ITileContainerLogic {
    int getSize(IScriptedBlockEntity be);

    ItemStack getItem(IScriptedBlockEntity be, int slot);

    void setItem(IScriptedBlockEntity be, int slot, ItemStack stack);

    ItemStack removeItem(IScriptedBlockEntity be, int slot, int amount);

    ItemStack removeItemNoUpdate(IScriptedBlockEntity be, int slot);

    void clearContent(IScriptedBlockEntity be);

    boolean stillValid(IScriptedBlockEntity be, Player player);

    int[] getSlotsForFace(IScriptedBlockEntity be, Direction side);

    boolean canPlaceItemThroughFace(IScriptedBlockEntity be, int slot, ItemStack stack, Direction side);

    boolean canTakeItemThroughFace(IScriptedBlockEntity be, int slot, ItemStack stack, Direction side);
}
