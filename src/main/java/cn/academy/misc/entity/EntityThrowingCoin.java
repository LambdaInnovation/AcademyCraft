/**
 * 
 */
package cn.academy.misc.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import cn.academy.core.register.ACItems;
import cn.academy.misc.client.render.RendererCoin;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ClientOnly. Renders the throwing coin after it had been thrown.
 * @author WeathFolD
 *
 */
@RegistrationClass
@RegEntity(renderName = "renderer")
public class EntityThrowingCoin extends Entity {
	
	@SideOnly(Side.CLIENT)
	public static RendererCoin renderer;
	
	public static final double MAXHEIGHT = 2;
	private static final int MAXLIFE = 100;
	
	private double initHt;
	public EntityPlayer player;
	public ItemStack stack;
	public Vec3 axis;

	public EntityThrowingCoin(EntityPlayer player, ItemStack stack) {
		super(player.worldObj);
		this.player = player;
		this.stack = stack;
		initHt = player.posY;
		setPosition(player.posX, player.posY, player.posZ);
		axis = Vec3.createVectorHelper(rand.nextDouble() + .01, rand.nextDouble(), rand.nextDouble());
		this.ignoreFrustumCheck = true;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void onUpdate() {
		++ticksExisted;
		NBTTagCompound nbt = GenericUtils.loadCompound(stack);
		
		double prg = (double)(Minecraft.getSystemTime() - nbt.getLong("startTime"))/(40 * 50);
		setPosition(player.posX, 
			initHt + MAXHEIGHT * (-4 * (prg - 0.5) * (prg - 0.5) + 1),
			player.posZ);
		ItemStack cur = player.getCurrentEquippedItem();
		if(cur == null || cur.getItem() != ACItems.coin)
			setDead();
		if(posY < initHt || ticksExisted > MAXLIFE || !ACItems.coin.inProgress(stack)) {
			setDead();
		}
	}

	@Override
	protected void entityInit() {}

	@Override
	protected void readEntityFromNBT(NBTTagCompound var1) {
		setDead();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound var1) {}

}
