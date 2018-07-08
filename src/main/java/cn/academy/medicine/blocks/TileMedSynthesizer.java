package cn.academy.medicine.blocks;

import cn.academy.core.block.TileReceiverBase;
import cn.academy.medicine.MedicineApplyInfo;
import cn.academy.medicine.Properties;
import cn.academy.medicine.items.ItemMedicineBottle;
import cn.academy.medicine.items.ItemPowder;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.TickScheduler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;


@Registrant
@RegTileEntity
public class TileMedSynthesizer extends TileReceiverBase {

    private float progress_ = 0.0f;
    private boolean synthesizing_ = false;

    static float ProgPerTick = 0.04f;
    static double ConsumePerSynth = 2000;
    static double ConsumePerTick = ConsumePerSynth * ProgPerTick;
    static int bottleSlot = 4;

    TickScheduler scheduler = new TickScheduler();
    public TileMedSynthesizer() {
        super("medicine_synthesizer", 6, 10000, 100);
        scheduler.every(5).atOnly(Side.SERVER).run(this::sync);
    }


    @Override
    public void updateEntity(){
        World world = getWorldObj();

        if (synthesizing_) {
            progress_ = Math.min(1, progress_ + ProgPerTick);
            boolean consEnergy = pullEnergy(ConsumePerTick) == ConsumePerTick;

            if (!world.isRemote) {

                if (progress_ == 1.0f) {
                    doSynth();
                    synthesizing_ = false;
                    progress_ = 0.0f;
                } else if (!consEnergy || !canSynth()) {
                    synthesizing_ = false;
                    progress_ = 0.0f;
                }

            }
        }

        super.updateEntity();
        scheduler.runTick();
    }


    public void beginSynth(){
        if(!getWorldObj().isRemote) {
            if (!synthesizing_ && canSynth()) {
                progress_ = 0.0f;
                synthesizing_ = true;
                sync();
            }
        }
        else
            throw new IllegalArgumentException("requirement failed");
    }

    private void sync(){
        NetworkMessage.sendToAllAround(TargetPoints.convert(this, 8), this, "synth_sync",
                Boolean.valueOf(synthesizing_), Float.valueOf(progress_));
    }

    private boolean canSynth(){
        return (inventory[0]!=null ||inventory[1]!=null ||inventory[2]!=null ||inventory[3]!=null )
                && inventory[bottleSlot] != null ;
    }

    public static MedicineApplyInfo synth(List<Properties.Property> mats){
        MedicineApplyInfo info=synthDirect(mats);
        if(info !=null)
            return info;
        else
        {
            return new MedicineApplyInfo(Properties.instance.Targ_Disposed, Properties.instance.Str_Strong,
                    1.5f, Properties.instance.Apply_Instant_Decr,
                    Properties.instance.Targ_Disposed.medSensitiveRatio * RandUtils.rangef(0.8f, 1.2f));
        }
    }

    private static <T extends Properties.Property> T findOne(List<Properties.Property> mats, Class<T> tag){
        List<Properties.Property> ret=new ArrayList<>();
        for(Properties.Property p:mats)
        {
            if(tag.isInstance(p))
                ret.add(p);
        }
        if (ret.size() == 1)
            return (T) ret.get(0);
        return null;
    }



    public static MedicineApplyInfo synthDirect(List<Properties.Property> mats){

        Properties.Target targ = findOne(mats,Properties.Target.class);
        Properties.Strength str = findOne(mats, Properties.Strength.class);
        Properties.ApplyMethod method = findOne(mats, Properties.ApplyMethod.class);
        Properties.Variation vars = findOne(mats, Properties.Variation.class);

        if(targ!=null && str!=null && method!=null){
            float variationMin = 0.8f, variationMax= 1.2f;
            float strValue = str.baseValue * method.strength;
            float medSensValue = targ.medSensitiveRatio;
            Properties.Strength rStrength = str;
            Properties.ApplyMethod rMethod = method;
            // Process variation
            if(vars==Properties.instance.Var_Infinity) {
                strValue = 10000;
                medSensValue = 0.99f;
                rStrength = Properties.instance.Str_Infinity;
            }
            else if (vars==Properties.instance.Var_Desens) {
                medSensValue = -1.2f * medSensValue;
                boolean incr = RandUtils.nextFloat() < 0.05f;
                rMethod = Properties.instance.findApplyMethod(method.instant, incr);
            }
            else if (vars==Properties.instance.Var_Fluct) {
                variationMin = 0.6f;
                variationMax = 1.5f;
            }
            else if (vars==Properties.instance.Var_Stabilize) {
                variationMin = 0.9f;
                variationMax = 1.1f;
            }
            else if (vars==Properties.instance.Var_Neutralize) {
                //TODO
            }

            return new MedicineApplyInfo(targ, rStrength,
                    strValue * RandUtils.rangef(variationMin, variationMax),
                    rMethod, medSensValue * RandUtils.rangef(variationMin, variationMax));
        }
        return null;
    }

    public static boolean isInputSlot(int slotID)
    {
        return slotID >= 0 && slotID < 4;
    }

    public static int outputSlot = 5;

    private void doSynth(){
        if(!getWorldObj().isRemote) {
            List<Properties.Property> ps = new ArrayList<>();
            for(int i=0;i<4;i++){
                if(inventory[i]!=null){
                    ps.add(ItemPowder.getProperty(inventory[i]));
                }
            }
            MedicineApplyInfo result = synth(ps);
            ItemStack resultStack = ItemMedicineBottle.create(result);

            for(int i=0;i<4;i++){
                inventory[i]=null;
            }
            setInventorySlotContents(outputSlot, resultStack);

            inventory[bottleSlot].stackSize -= 1;
            if (inventory[bottleSlot].stackSize == 0) {
                inventory[bottleSlot]= null;
            }
        }
        else
            throw new IllegalArgumentException("requirement failed");

    }

    public float synthProgress(){
        return progress_;
    }
    public boolean synthesizing(){
        return synthesizing_;
    }

    @NetworkMessage.Listener(channel="synth_sync", side=Side.CLIENT)
    private void hSyncSynth(Boolean ss, Float pr){
        synthesizing_ = ss;
        progress_ = pr;
    }

}
