package cn.academy.ability.api.data;

import cn.academy.core.AcademyCraft;
import cn.academy.core.util.SubscribePipeline;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInit;
import net.minecraft.entity.player.EntityPlayer;

@Registrant
@RegInit
public class DataHooks {
    
    public static void init() {
        AcademyCraft.pipeline.register(new DataHooks());
    }
    
    // TODO Dynamicify this pattern. That is, allow range and other parameters to go like this without sucking up the code

    @SubscribePipeline("?.?.$damage")
    public float modifyDamage(float value, EntityPlayer player) {
        AbilityData data = AbilityData.get(player);
        return value * data.getCategory().getDamageRate(data);
    }
    
    @SubscribePipeline("?.?.$consumption")
    public float modifyConsumption(float value, EntityPlayer player) {
        AbilityData data = AbilityData.get(player);
        return value * data.getCategory().getConsumptionRate(data);
    }
    
    @SubscribePipeline("?.?.$overload")
    public float modifyOverload(float value, EntityPlayer player) {
        AbilityData data = AbilityData.get(player);
        return value * data.getCategory().getOverloadRate(data);
    }
}
