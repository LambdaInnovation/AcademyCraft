package cn.academy.entity;

import cn.academy.client.render.entity.RenderDiamondShield;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityDiamondShield extends EntityAdvanced
{

    public static final float SIZE = 1.8f;
    
    final EntityPlayer player;

    public EntityDiamondShield(EntityPlayer _player) {
        super(_player.getEntityWorld());
        player = _player;
        this.setSize(SIZE, SIZE);

        setPosition(player.posX, player.posY, player.posZ);
        ignoreFrustumCheck = true;
        updatePos();
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        updatePos();
    }

    private void updatePos() {
        Vec3d pos =
            VecUtils.add(player.getPositionVector().add(0, 1.1, 0), VecUtils.multiply(player.getLookVec(), 1));

        this.posX = pos.x;
        this.posY = pos.y;
        this.posZ = pos.z;
        this.rotationYaw = player.rotationYawHead;
        this.rotationPitch = player.rotationPitch;
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

}