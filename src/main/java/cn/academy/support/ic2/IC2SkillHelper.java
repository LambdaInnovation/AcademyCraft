/**
 * Copyright (c) Lambda Innovation, 2013-2016
 * This file is part of the AcademyCraft mod.
 * https://github.com/LambdaInnovation/AcademyCraft
 * Licensed under GPLv3, see project root for more information.
 */
package cn.academy.support.ic2;

import cn.academy.ability.api.event.AbilityActivateEvent;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc;
import cn.lambdalib.util.helper.BlockPos;
import cn.lambdalib.util.mc.IBlockSelector;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ic2.core.block.wiring.BlockCable;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

/**
 * @author KSkun
 */
public class IC2SkillHelper {

    private static final IBlockSelector blockSelector = new IBlockSelector() {
        @Override
        public boolean accepts(World world, int x, int y, int z, Block block) {
            if(block instanceof BlockCable) {
                return true;
            } else {
                return false;
            }
        }
    };

    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onAbilityActive(AbilityActivateEvent event) {
        spawnArc(event.player);
    }

    @SideOnly(Side.CLIENT)
    public void spawnArc(EntityPlayer player) {
        List<BlockPos> blockList = WorldUtils.getBlocksWithin(player, 5, 100, blockSelector);
        World world = player.worldObj;
        for(BlockPos bp : blockList) {
            world.spawnEntityInWorld(new EntitySurroundArc(world, bp.x, bp.y, bp.z, 1, 1));
        }
    }

}
