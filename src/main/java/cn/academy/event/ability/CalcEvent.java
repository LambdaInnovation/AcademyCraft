package cn.academy.event.ability;

import cn.academy.ability.Skill;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

public class CalcEvent<T> extends Event {

    /**
     * Post the specified event to the event bus and returns the result.
     *  A fast and common routine to perform calculation.
     */
    public static <T> T calc(CalcEvent<T> evt) {
        MinecraftForge.EVENT_BUS.post(evt);
        return evt.value;
    }

    public T value;

    public CalcEvent(T initial) {
        value = initial;
    }

    public static class PlayerCalcEvent<T> extends CalcEvent<T> {

        public final EntityPlayer player;

        public PlayerCalcEvent(EntityPlayer _player, T initial) {
            super(initial);
            player = _player;
        }

    }

    public static class MaxCP extends PlayerCalcEvent<Float> {
        public MaxCP(EntityPlayer player, float initial) {
            super(player, initial);
        }
    }

    public static class CPRecoverSpeed extends PlayerCalcEvent<Float> {
        public CPRecoverSpeed(EntityPlayer player, float initial) {
            super(player, initial);
        }
    }

    public static class OverloadRecoverSpeed extends PlayerCalcEvent<Float> {
        public OverloadRecoverSpeed(EntityPlayer player, float initial) {
            super(player, initial);
        }
    }

    public static class MaxOverload extends PlayerCalcEvent<Float> {
        public MaxOverload(EntityPlayer player, float initial) {
            super(player, initial);
        }
    }

    public static class SkillAttack extends PlayerCalcEvent<Float> {

        public final Skill skill;
        public final Entity target;

        public SkillAttack(EntityPlayer player, Skill _skill, Entity _target, float initial) {
            super(player, initial);
            skill = _skill;
            target = _target;
        }

    }

    public static class SkillPerform extends AbilityEvent {

        public float cp;
        public float overload;

        public SkillPerform(EntityPlayer player, float _overload, float _cp) {
            super(player);

            cp = _cp;
            overload = _overload;
        }

    }

}