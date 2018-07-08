package cn.academy.medicine;

import cn.academy.medicine.blocks.TileMedSynthesizer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkS11n;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class MSNetEvents {

    public static MSNetEvents instance = new MSNetEvents();
    @RegInitCallback
    public static void init(){
        NetworkS11n.addDirectInstance(instance);
    }

    public static final String MSG_BEGIN_SYNTH = "begin";

    @NetworkMessage.Listener(channel=MSG_BEGIN_SYNTH, side=Side.SERVER)
    public void hBegin(TileMedSynthesizer tile){
        tile.beginSynth();
    }

}
