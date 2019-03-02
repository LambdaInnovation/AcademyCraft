package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.AcademyCraft;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.advancements.ACAdvancements;
import cn.academy.advancements.ACAdvancements;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityArc;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.BlockSelectors;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.entityx.handlers.Life;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * @author WeAthFolD, KSkun
 */
public class ArcGen extends Skill
{
    public static final Skill instance = new ArcGen();
    public ArcGen()
    {
        super("arc_gen", 1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyid){
        activateSingleKey2(rt, keyid, ArcGenContext::new);
    }

    public static class ArcGenContext extends Context
    {
        static final String MSG_EFFECT = "effect";
        static final String MSG_PERFORM = "perform";

        final IBlockSelector blockFilter = (world, x, y, z, block) -> block == Blocks.WATER || block == Blocks.FLOWING_WATER ||
                BlockSelectors.filNormal.accepts(world, x, y, z, block);

        public ArcGenContext(EntityPlayer player)
        {
            super(player, ArcGen.instance);
        }

        private float damage = lerpf(5, 9, ctx.getSkillExp());
        private float igniteProb = lerpf(0, 0.6f, ctx.getSkillExp());
        private double fishProb = ctx.getSkillExp() > 0.5f? 0.1 : 0;
        private boolean canStunEnemy = ctx.getSkillExp() >= 1.0f;
        private float range = lerpf(6, 15, ctx.getSkillExp());
        private float cp = lerpf(30, 70, ctx.getSkillExp());

        private boolean consume()
        {
            float overload = lerpf(18, 11, ctx.getSkillExp());
            return ctx.consume(overload, cp);
        }

        @Listener(channel=MSG_KEYDOWN, side=Side.CLIENT)
        private void l_keydown()
        {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        private void s_perform(){
            if(consume()) {
                World world = player.world;
                // Perform ray trace
                RayTraceResult result = Raytrace.traceLiving(player, range, null, blockFilter);

                sendToClient(MSG_EFFECT, range);

                if (result != null)
                {
                    float expincr = 0f;
                    if (result.typeOfHit == RayTraceResult.Type.ENTITY)
                    {
                        EMDamageHelper.attack(ctx, result.entityHit, damage);
                        expincr = getExpIncr(true);
                    }
                    else
                    {
                        //BLOCK
                        BlockPos pos = result.getBlockPos();
                        IBlockState ibs = player.world.getBlockState(pos);
                        Block block = ibs.getBlock();
                        if (block == Blocks.WATER)
                        {
                            if (RandUtils.ranged(0, 1) < fishProb)
                            {
                                world.spawnEntity(new EntityItem(
                                        world,
                                        result.hitVec.x,
                                        result.hitVec.y,
                                        result.hitVec.z,
                                        new ItemStack(Items.COOKED_FISH)));
//                                ACAdvancements.trigger(player, ACAdvancements.ac_milestone.ID);
                            }
                        }
                        else
                        {
                            if (RandUtils.ranged(0, 1) < igniteProb)
                            {
                                pos = pos.add(0, 1, 0);
                                if (world.isAirBlock(pos))
                                {
                                    world.setBlockState(pos, Blocks.FIRE.getDefaultState());
//                                    ACAdvancements.trigger(player, ACAdvancements.ac_milestone.ID);
                                }
                            }
                        }
                        expincr = getExpIncr(false);
                    }
                    ctx.addSkillExp(expincr);
                }
                ctx.setCooldown((int)lerpf(15, 5, ctx.getSkillExp()));
            }
            terminate();
        }

        private float getExpIncr(boolean effectiveHit)
        {
            if (effectiveHit) {
                return lerpf(0.0048f, 0.0072f, ctx.getSkillExp());
            } else {
                return lerpf(0.0018f, 0.0027f, ctx.getSkillExp());
            }
        }
    }


    @SideOnly(Side.CLIENT)
    @RegClientContext(ArcGenContext.class)
    public static class ArcGenContextC extends ClientContext
    {
        public ArcGenContextC(ArcGenContext par)
        {
            super(par);
        }

        @Listener(channel=ArcGenContext.MSG_EFFECT, side=Side.CLIENT)
        private void c_spawnEffects(float range)
        {
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.addMotionHandler(new Life(10));
            arc.lengthFixed = false;
            arc.length = range;

            player.world.spawnEntity(arc);
            ACSounds.playClient(player, "em.arc_weak", SoundCategory.AMBIENT, 0.5f);
        }

    }
}
