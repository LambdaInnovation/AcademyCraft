package cn.academy.misc.media;

import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.SerializeIncluded;
import cn.lambdalib.s11n.nbt.NBTS11n;
import cn.lambdalib.util.datapart.DataPart;
import cn.lambdalib.util.datapart.EntityData;
import cn.lambdalib.util.datapart.RegDataPart;
import com.google.common.base.Preconditions;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

import java.util.BitSet;

@Registrant
@RegDataPart(EntityPlayer.class)
public class MediaData extends DataPart<EntityPlayer> {

    public static MediaData of(EntityPlayer player) {
        return EntityData.get(player).getPart(MediaData.class);
    }

    {
        setNBTStorage();
        setClientNeedSync();
    }

    /**
     * This list corresponds to only internal medias.
     */
    @SerializeIncluded
    private final BitSet installed = new BitSet();

    public void install(ACMedia media) {
        checkSide(Side.SERVER);
        Preconditions.checkArgument(!media.isExternal());

        installed.set(index(media));

        sync();
    }

    public boolean isInstalled(ACMedia media) {
        return media.isExternal() || installed.get(index(media));
    }

    private int index(ACMedia media) {
        return MediaManager.internalMedias().indexOf(media);
    }

    @Override
    public void fromNBT(NBTTagCompound tag) {
        NBTS11n.read(tag, this);
    }

    @Override
    public void toNBT(NBTTagCompound tag) {
        NBTS11n.write(tag, this);
    }
}
