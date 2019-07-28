package cn.academy.entity;

import cn.academy.datapart.AbilityData;
import cn.academy.client.render.entity.MarkRender;
import cn.academy.client.render.misc.TPParticleFactory;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Spawn a position mark indicating where the player would be teleport to. You
 * should spawn it in CLIENT ONLY.
 * 
 * @author WeathFolD
 */
@SideOnly(Side.CLIENT)
public class EntityTPMarking extends EntityAdvanced {

//    static TPParticleFactory particleFac = TPParticleFactory.instance;

    final AbilityData data;
    protected final EntityPlayer player;

    public boolean available = true;

    public EntityTPMarking(EntityPlayer player) {
        super(player.getEntityWorld());
        data = AbilityData.get(player);
        this.player = player;
        setPosition(player.posX, player.posY, player.posZ);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        rotationPitch = player.rotationPitch;
        rotationYaw = player.rotationYaw;

        if (available && rand.nextDouble() < 0.4) {
            TPParticleFactory.instance.setPosition(posX + RandUtils.ranged(-1, 1), posY + RandUtils.ranged(0.2, 1.6) - 1.6,
                    posZ + RandUtils.ranged(-1, 1));
            TPParticleFactory.instance.setVelocity(RandUtils.ranged(-.03, .03), RandUtils.ranged(0, 0.05),
                    RandUtils.ranged(-.03, .03));

            world.spawnEntity(TPParticleFactory.instance.next(world));
        }
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    public double getDist() {
        return this.getDistanceSq(player);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound p_70037_1_) {
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {
    }

}