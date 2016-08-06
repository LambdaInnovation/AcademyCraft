/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.vanilla.meltdowner.entity.EntityMineRayBasic;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * @author WeAthFolD
 */
public class MineRayBasic extends MineRaysBase {

    public static final MineRayBasic instance = new MineRayBasic();

    private MineRayBasic() {
        super("basic", 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new BasicRayAction());
    }

    public static class BasicRayAction extends MRAction {
        public BasicRayAction() {
            super(instance);

            setRange(10);
            setHarvestLevel(2);
            setSpeed(0.2f, 0.4f);
            setConsumption(15.0f, 8.0f);
            setOverload(200f, 120f);
            setCooldown(40f, 20f);
            setExpIncr(0.0005f);
        }

        @Override
        protected void onBlockBreak(World world, int x, int y, int z, Block block) {
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound(), .5f, 1f);
            block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 0);
            world.setBlock(x, y, z, Blocks.air);
        }

        @SideOnly(Side.CLIENT)
        @Override
        protected Entity createRay() {
            return new EntityMineRayBasic(player);
        }

    }

}
