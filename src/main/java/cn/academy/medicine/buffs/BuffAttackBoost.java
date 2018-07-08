package cn.academy.medicine.buffs;


import cn.academy.medicine.api.Buff;
import cn.academy.medicine.api.BuffApplyData;
import cn.academy.medicine.api.RegBuff;
import cn.lambdalib.annoreg.core.Registrant;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

@Registrant
@RegBuff
public class BuffAttackBoost extends Buff {

    private String playerName;
    private float ratio;

    public BuffAttackBoost(float ratio, String playerName){
        super("attack_boost");
        this.ratio = ratio;
        this.playerName = playerName;
    }

    @Override
    public void onBegin(EntityPlayer player){
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onTick(EntityPlayer player, BuffApplyData applyData) {

    }

    @Override
    public void onEnd(EntityPlayer player){
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    private void  onLivingHurt(LivingHurtEvent evt){
        if(evt.source.getEntity() instanceof EntityPlayer){
            if (evt.source.getEntity().getCommandSenderName().equals(playerName))
                evt.ammount *= ratio;
        }
    }


    @Override
    public void load(NBTTagCompound tag ){
        playerName = tag.getString("name");
        ratio = tag.getFloat("ratio");
    }

    @Override
    public void store(NBTTagCompound tag ){
        tag.setString("name", playerName);
        tag.setFloat("ratio", ratio);
    }

}