package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.AbilityContext;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.KeyDelegate;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.data.PresetData;
import cn.academy.core.util.RangedRayDamage;
import cn.academy.vanilla.electromaster.client.effect.RailgunHandEffect;
import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import cn.academy.vanilla.electromaster.entity.EntityRailgunFX;
import cn.academy.vanilla.electromaster.event.CoinThrowEvent;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.client.renderhook.DummyRenderData;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.Raytrace;
import cn.lambdalib.util.mc.SideHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static cn.lambdalib.util.generic.MathUtils.lerpf;

public class Railgun extends Skill {

    public static final Railgun instance = new Railgun();

    private static final String
        MSG_CHARGE_EFFECT = "charge_eff",
        MSG_PERFORM       = "perform",
        MSG_REFLECT       = "reflect",
        MSG_COIN_PERFORM  = "coin_perform",
        MSG_ITEM_PERFORM  = "item_perform";

    private static final double
        REFLECT_DISTANCE = 15;

    private Set<Item> acceptedItems = new HashSet<>();
    {
        acceptedItems.add(Items.iron_ingot);
        acceptedItems.add(Item.getItemFromBlock(Blocks.iron_block));
    }

    private Railgun() {
        super("railgun", 4);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private boolean isAccepted(ItemStack stack) {
        return stack != null && acceptedItems.contains(stack.getItem());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new Delegate());
    }

    @SubscribeEvent
    public void onThrowCoin(CoinThrowEvent evt) {
        CPData cpData = CPData.get(evt.entityPlayer);
        PresetData pData = PresetData.get(evt.entityPlayer);

        boolean spawn = cpData.canUseAbility() &&
                pData.getCurrentPreset().hasControllable(this);

        if (spawn) {
            if (SideHelper.isClient()) {
                spawnClientEffect(evt.entityPlayer);

                informDelegate(evt.coin);
            } else {
                NetworkMessage.sendToAllAround(
                        TargetPoints.convert(evt.entityPlayer, 30),
                        instance,
                        MSG_CHARGE_EFFECT,
                        evt.entityPlayer
                );
            }
        }
    }

    private void informDelegate(EntityCoinThrowing coin) {
        ClientRuntime rt = ClientRuntime.instance();
        Collection<KeyDelegate> delegates = rt.getDelegates(ClientRuntime.DEFAULT_GROUP);
        if (!delegates.isEmpty()) {
            for (Iterator<KeyDelegate> i = delegates.iterator(); i.hasNext(); ) {
                KeyDelegate dele = i.next();
                if (dele instanceof Delegate) {
                    ((Delegate) dele).informThrowCoin(coin);
                    return;
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel=MSG_CHARGE_EFFECT, side= Side.CLIENT)
    private void hSpawnClientEffect(EntityPlayer target) {
        spawnClientEffect(target);
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel=MSG_REFLECT, side=Side.CLIENT)
    private void hReflectClient(EntityPlayer player, Entity reflector) {
        EntityRailgunFX eff = new EntityRailgunFX(player, REFLECT_DISTANCE);

        double dist = player.getDistanceToEntity(reflector);
        Motion3D mo = new Motion3D(player, true).move(dist);

        eff.setPosition(mo.px, mo.py, mo.pz);
        eff.rotationYaw = reflector.getRotationYawHead();
        eff.rotationPitch = reflector.rotationPitch;

        player.worldObj.spawnEntityInWorld(eff);
    }

    private void reflectServer(EntityPlayer player, Entity reflector) {
        MovingObjectPosition result = Raytrace.traceLiving(reflector, REFLECT_DISTANCE);
        if (result != null && result.typeOfHit == MovingObjectType.ENTITY) {
            AbilityContext.of(player, Railgun.instance).attack(result.entityHit, 14);
        }

        NetworkMessage.sendToAllAround(TargetPoints.convert(player, 20),
                instance, MSG_REFLECT, player, reflector);
    }

    private void spawnClientEffect(EntityPlayer target) {
        DummyRenderData.get(target).addRenderHook(new RailgunHandEffect());
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
    private void performClient(EntityPlayer player, double length) {
        player.worldObj.spawnEntityInWorld(new EntityRailgunFX(player, length));
    }

    private void performServer(EntityPlayer player) {
        AbilityContext ctx = AbilityContext.of(player, this);

        final float exp = ctx.getSkillExp();

        float cp     = lerpf(340, 455, exp);
        float overload = lerpf(160, 110, exp);
        if (ctx.consume(overload, cp)) {
            float dmg = lerpf(40, 100, exp);
            float energy = lerpf(900, 2000, exp);

            double[] length = new double[] { 45 };
            RangedRayDamage damage = new RangedRayDamage.Reflectible(ctx, 2, energy, reflector -> {
                reflectServer(player, reflector);
                length[0] = Math.min(length[0], reflector.getDistanceToEntity(player));
                NetworkMessage.sendToServer(instance, MSG_REFLECT, player, reflector);
            });
            damage.startDamage = dmg;
            damage.perform();
            instance.triggerAchievement(player);

            ctx.setCooldown((int) lerpf(300, 160, exp));
            NetworkMessage.sendToAllAround(
                    TargetPoints.convert(player, 20),
                    instance, MSG_PERFORM,
                    player, length[0]);
        }
    }

    @Listener(channel=MSG_COIN_PERFORM, side=Side.SERVER)
    private void consumeCoinAtServer(EntityPlayer player, EntityCoinThrowing coin) {
        coin.setDead();
        performServer(player);
    }

    @Listener(channel=MSG_ITEM_PERFORM, side=Side.SERVER)
    private void consumeItemAtServer(EntityPlayer player) {
        ItemStack equipped = player.getCurrentEquippedItem();
        if (isAccepted(equipped)) {
            equipped.stackSize--;
            if (equipped.stackSize == 0) {
                player.setCurrentItemOrArmor(0, null);
            }

            performServer(player);
        }
    }

    private static class Delegate extends KeyDelegate {

        EntityCoinThrowing coin;

        int chargeTicks = -1;

        void informThrowCoin(EntityCoinThrowing coin) {
            if (this.coin == null || this.coin.isDead) {
                this.coin = coin;
                onKeyAbort();
            }
        }

        @Override
        public void onKeyDown() {
            if (coin == null) {
                if (instance.isAccepted(getPlayer().getCurrentEquippedItem())) {
                    instance.spawnClientEffect(getPlayer());
                    chargeTicks = 20;
                }
            } else {
                if (coin.getProgress() > 0.7) {
                    NetworkMessage.sendToServer(instance,
                            MSG_COIN_PERFORM, getPlayer(), coin);
                }

                coin = null; // Prevent second QTE judgement
            }
        }

        @Override
        public void onKeyTick() {
            if (chargeTicks != -1) {
                if (--chargeTicks == 0) {
                    NetworkMessage.sendToServer(instance,
                            MSG_ITEM_PERFORM, getPlayer());
                }
            }
        }

        @Override
        public void onKeyUp() {
            chargeTicks = -1;
        }

        @Override
        public void onKeyAbort() {
            chargeTicks = -1;
        }

        public DelegateState getState() {
            if (coin != null && !coin.isDead) {
                return coin.getProgress() < 0.6 ? DelegateState.CHARGE : DelegateState.ACTIVE;
            } else {
                return chargeTicks == -1 ? DelegateState.IDLE : DelegateState.CHARGE;
            }
        }

        @Override
        public ResourceLocation getIcon() {
            return instance.getHintIcon();
        }

        @Override
        public int createID() {
            return 0;
        }

        @Override
        public Skill getSkill() {
            return instance;
        }
    }
}
