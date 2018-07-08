package cn.academy.medicine;

import cn.academy.ability.api.cooldown.CooldownData;
import cn.academy.ability.api.data.CPData;
import cn.academy.core.LocalHelper;
import cn.academy.medicine.api.BuffData;
import cn.academy.medicine.buffs.*;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.Color;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class Properties {
    public abstract class Property {
        public abstract String stackDisplayHint();
        public abstract String internalID();
        public final String displayDesc(){
            return Properties.localProps.get(internalID());
        }
    }

    public abstract class Target extends Property {
        public abstract void apply(EntityPlayer player, MedicineApplyInfo data);

        public Color baseColor;
        public float medSensitiveRatio;
        String id;

        @Override
        public String stackDisplayHint()
        {
            return Properties.formatItemDesc("targ", EnumChatFormatting.GREEN, displayDesc());
        }
        @Override
        public String internalID()
        {
            return "targ_" + id;
        }

    }

    public abstract class Strength extends Property {

        public float baseValue;
        String id;
        @Override
        public String stackDisplayHint()
        {
            return Properties.formatItemDesc("str", EnumChatFormatting.RED, displayDesc());
        }

        @Override
        public String internalID()
        {
            return "str_" + id;
        }

    }

    public abstract class ApplyMethod extends Property {
        public boolean instant;
        public boolean incr;
        public float strength;

        String id;
        @Override
        public String stackDisplayHint()
        {
            return Properties.formatItemDesc("app", EnumChatFormatting.AQUA, displayDesc());
        }
        @Override
        public String internalID(){
            return "app_" + id;
        }
    }

    public abstract class Variation extends Property {
        String id;
        @Override
        public String internalID()
        {
            return "var_" + id;
        }
        @Override
        public String stackDisplayHint()
        {
            return Properties.formatItemDesc("var", EnumChatFormatting.DARK_PURPLE, displayDesc());
        }
    }

    int ContApplyTime = 15 * 20;

    public Target Targ_Life = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
                float amt = 5 * data.strengthModifier;
            if (data.method.instant) {
                if (data.method.incr) {
                    player.heal(amt);
                } else {
                    player.attackEntityFrom(DamageSource.causePlayerDamage(player), amt);
                }
            } else { // Continuous recovery
                BuffData buffData = BuffData.apply(player);
                int time = ContApplyTime;

                BuffHeal buff = new BuffHeal(amt);

                buffData.addBuff(buff, time);
            }
        }
        {
            id = "life";
            baseColor = new Color(0xffff0000);
            medSensitiveRatio = 0.05f;
        }
    };

    public Target Targ_CP = new Target() {
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
            CPData cpData = CPData.get(player);
            float baseValue = cpData.getMaxCP() * 0.1f * data.strengthModifier;

            if (data.method.instant) {
                cpData.setCP(cpData.getCP() + baseValue);
            }
            else {
                BuffData buffData = BuffData.apply(player);
                int time = ContApplyTime;
                float perTick = baseValue;
                buffData.addBuff(new BuffCPRecovery(perTick), time);
            }
        }
        {
            id = "cp";
            baseColor = new Color(0xff0000ff);
            medSensitiveRatio = 0.05f;
        }
    };

    public Target Targ_Overload = new Target() {
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
                CPData cpData = CPData.get(player);
                float amt = cpData.getMaxOverload() * 0.1f * data.strengthModifier;

        if (data.method.instant) {
            cpData.setOverload(cpData.getOverload() - amt);
        } else {
            BuffData.apply(player).addBuff(new BuffOverloadRecovery(amt), ContApplyTime);
        }
    }
        {
            id = "overload";
			baseColor =  new Color(0xffffff00);
			medSensitiveRatio = 0.05f;
		}
    };

    public Target Targ_Jump = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
                if(data.method == Apply_Continuous_Incr) {

                    int time = ContApplyTime;
                    PotionEffect eff = new PotionEffect(Potion.jump.id, time, strenghToLevel(data.strengthType));
                    player.addPotionEffect(eff);
                }
                else
                    throw new IllegalArgumentException("requirement failed");//convert from Scala code:require(data.method == Apply_Continuous_Incr)
        }

        {
			id = "jump";
			baseColor =  new Color(0xffffffff);
			medSensitiveRatio = 0.03f;
		}
    };

    public Target Targ_Cooldown = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
            float baseValue = 0.2f * data.strengthModifier;
            if (data.method.instant) {
                for (CooldownData.SkillCooldown cd : CooldownData.of(player).rawData().values()) {
                    cd.setTickLeft((int)(cd.getTickLeft() - baseValue * cd.getMaxTick()));
                }
            }
            else {
                BuffData.apply(player).addBuff(new BuffCooldownRecovery(baseValue), ContApplyTime);
            }
        }

        {
			id = "cooldown";
			baseColor =  new Color(0xff0000ff);
			medSensitiveRatio = 0.1f;
		}
    };

    public Target Targ_MoveSpeed = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
            if(!data.method.instant) {
                int time = ContApplyTime;
                Potion potion = (data.method.incr)? Potion.moveSpeed: Potion.moveSlowdown;
                player.addPotionEffect(new PotionEffect(potion.id, time, strenghToLevel(data.strengthType)));
            }
            else
                throw new IllegalArgumentException("requirement failed");
        }
    };

    public Target Targ_Disposed = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
            float test = RandUtils.rangef(0, 1);
            World world = player.worldObj;
            if(test<0.5){// No effect but adds sensitivity
                System.out.println("+Sensitivity");
            }
            else if (test<0.75){
                System.out.println("Debuff");
            }
            else{// Fake Explosion
                world.playSoundEffect(player.posX, player.posY, player.posZ, "random.explode",
                        4.0f, 1.0f);
                player.attackEntityFrom(DamageSource.causePlayerDamage(player), 10f);
            }
        }
        {
			id = "disposed";
			baseColor =  new Color(0xff000000);
			medSensitiveRatio = 0.5f;
		}
    };

    public Target Targ_Attack = new Target(){
        @Override
        public void apply(EntityPlayer player, MedicineApplyInfo data){
            if(!data.method.instant){
                int time = ContApplyTime;
                float boostRatio = 1 + (0.2f * data.strengthModifier);
                BuffData.apply(player).addBuff(new BuffAttackBoost(boostRatio, player.getCommandSenderName()), time);
            }
            else
                throw new IllegalArgumentException("requirement failed");
        }

        {
            id = "attack";
            medSensitiveRatio = 0;
			baseColor =  new Color(0xffff00ff);
		}
    };


    public Strength Str_Mild = new Strength() {
        {			
            baseValue = 0.3f;
            id = "mild";
		}
    };

    public Strength Str_Weak = new Strength(){
        {
			baseValue = 0.6f;
            id = "weak";
		}

    };

    public Strength Str_Normal = new Strength(){
        {
			baseValue = 0.9f;
            id = "normal";
		}

    };

    public Strength Str_Strong = new Strength(){
        {
           baseValue= 1.5f;
            id = "strong";
        }


    };

    public Strength Str_Infinity = new Strength(){
        {
            baseValue= 10000f;
            id = "infinity";
        }
    };




    public ApplyMethod Apply_Instant_Incr = new ApplyMethod() {
        {
            incr = true;
            instant = true;
            strength = 2f;
            id = "instant_incr";
        }
    };

    public ApplyMethod Apply_Instant_Decr = new ApplyMethod(){
        {
            incr = false;
            instant = true;
            strength = -1f;

            id = "instant_decr";
        }
    };

    public ApplyMethod Apply_Continuous_Incr = new ApplyMethod(){{
            incr = true;
            instant = false;
            strength = 0.01f;

            id = "cont_incr";
    }};

    public ApplyMethod Apply_Continuous_Decr = new ApplyMethod(){{
            incr = false;
            instant = false;
            strength = -0.005f;

            id = "cont_decr";
    }};



    public Variation Var_Infinity = new Variation(){
        {
            id = "infinity";
        }
    };

    public Variation Var_Neutralize = new Variation(){
        {
            id = "neutralize";
        }
    };

    public Variation Var_Desens = new Variation(){
        {
            id = "desens";
        }
    };

    public Variation Var_Fluct = new Variation(){
        {
            id = "fluct";
        }
    };

    public Variation Var_Stabilize = new Variation(){
        {
            id = "stabilize";
        }
    };

    // Misc

    private static LocalHelper local = LocalHelper.at("ac.medicine");
    private static LocalHelper localTypes = local.subPath("prop_type");
    private static LocalHelper localProps = local.subPath("props");
    private List<ApplyMethod> applyMethodMapping = Arrays.asList(Apply_Instant_Incr, Apply_Instant_Decr,
            Apply_Continuous_Incr, Apply_Continuous_Decr);

    public ApplyMethod findApplyMethod(boolean instant, boolean incr)
    {
        for(ApplyMethod method:applyMethodMapping)
        {
            if(method.incr==incr && method.instant == instant)
                return method;
        }
        return null;

    }

    public int strenghToLevel(Strength strength)
    {
        if(strength==Str_Mild)
            return 0;
        else if(strength==Str_Weak)
            return 1;
        else if(strength==Str_Normal)
            return 2;
        else if(strength==Str_Strong)
            return 3;
        else if(strength==Str_Infinity)
            return 4;
        return -1;//invalid
    }

    private static String formatItemDesc(String propType, EnumChatFormatting color, String name){
        return color + localTypes.get(propType) + ": " + EnumChatFormatting.RESET + name;
    }

    // --- storage & s11n


    // For cross-version compatibility, only append new properties at the end of lists.

    private final List<Target> allTargets = Arrays.asList(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump,
            Targ_Disposed, Targ_Attack, Targ_Cooldown);
    private final List<Strength> allStrengths = Arrays.asList(Str_Mild, Str_Weak, Str_Normal, Str_Strong, Str_Infinity);
    private final List<ApplyMethod> allMethods = Arrays.asList(Apply_Instant_Incr, Apply_Instant_Decr,
            Apply_Continuous_Decr, Apply_Continuous_Incr);
    private final List<Variation> allVariations = Arrays.asList(Var_Infinity, Var_Neutralize,
            Var_Desens, Var_Stabilize, Var_Fluct);

    public final List<Property> allProperties = Arrays.asList(Targ_Life, Targ_CP, Targ_Overload, Targ_Jump,
            Targ_Disposed, Targ_Attack, Targ_Cooldown,Str_Mild, Str_Weak, Str_Normal, Str_Strong, Str_Infinity,
            Apply_Instant_Incr, Apply_Instant_Decr, Apply_Continuous_Decr, Apply_Continuous_Incr, Var_Infinity,
            Var_Neutralize, Var_Desens, Var_Stabilize, Var_Fluct);

    public static Properties instance = new Properties();

    public int writeTarget(Target t)
    {
        return serialize(allTargets, t);
    }
    public Target readTarget(int i)
    {
        return deserialize(allTargets, i);
    }

    public int writeStrength(Strength s)
    {
        return serialize(allStrengths, s);
    }
    public Strength readStrength(int i)
    {
        return deserialize(allStrengths, i);
    }

    public int writeMethod(ApplyMethod m)
    {
        return serialize(allMethods, m);
    }
    public ApplyMethod readMethod(int i)
    {
        return deserialize(allMethods, i);
    }

    public int writeVariation(Variation m)
    {
        return serialize(allVariations, m);
    }
    public Variation readVariation(int i)
    {
        return deserialize(allVariations, i);
    }

    Property find(String name)
    {
        for(Property p :allProperties)
        {
            if(p.internalID().equals(name)){
                return p;
            }
        }
        return null;
    }

    private <T> int serialize(List<T> seq, T value){
        int idx = seq.indexOf(value);
        if (idx == -1) {
            throw new IllegalArgumentException("Can't serialize " + value);
        }
        return idx;
    }

    private <T> T deserialize(List<T> seq, int idx)
    {
        return seq.get(idx);
    }

}

