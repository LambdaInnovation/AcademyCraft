package cn.academy.ability.api.data;

import cn.academy.core.AcademyCraft;
import cn.academy.core.util.SubscribePipeline;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import net.minecraft.entity.player.EntityPlayer;

@Registrant
public class DataHooks {

    @RegInitCallback
    public static void init() {
        AcademyCraft.pipeline.register(new DataHooks());
    }

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
