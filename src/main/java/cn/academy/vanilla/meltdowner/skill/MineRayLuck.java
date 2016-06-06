/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.core.Resources;
import cn.academy.vanilla.meltdowner.entity.EntityMineRayLuck;
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
public class MineRayLuck extends MineRaysBase {

    public static final MineRayLuck instance = new MineRayLuck();

    private MineRayLuck() {
        super("luck", 5);
        this.particleTexture = Resources.getTexture("effects/md_particle_luck");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new LuckRayAction());
    }

    public static class LuckRayAction extends MRAction {
        public LuckRayAction() {
            super(instance);

            setRange(20);
            setHarvestLevel(5);
            setSpeed(.5f, 1);
            setConsumption(50, 30);
            setOverload(320, 250);
            setCooldown(60, 30);
            setExpIncr(0.0003f);
        }

        @Override
        protected void onBlockBreak(World world, int x, int y, int z, Block block) {
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, block.stepSound.getBreakSound(), .5f, 1f);
            block.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z), 1.0f, 3);
            world.setBlock(x, y, z, Blocks.air);
        }

        @SideOnly(Side.CLIENT)
        @Override
        protected Entity createRay() {
            return new EntityMineRayLuck(player);
        }
    }

}
