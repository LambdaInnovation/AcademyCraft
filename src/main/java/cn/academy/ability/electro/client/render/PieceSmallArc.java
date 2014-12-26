/**
 * 
 */
package cn.academy.ability.electro.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.client.render.eff.Piece;

/**
 * @author WeathFolD
 *
 */
public class PieceSmallArc extends Piece {
	
	private static final ResourceLocation[] ANIM = ACClientProps.ANIM_SMALL_ARC;
	
	private int waitFrom = 100, waitTo = 150; //The random waiting time interval.
	private long lastChangeTime;
	private int changeWait;
	private int curIndex;
	
	public double offArea, rotArea;
	
	private Vec3 origPos = vec();
	private double origPitch, origYaw;

	public PieceSmallArc(double size) {
		super(size, size);
		randomWaitTime();
		offArea = size * .7;
		rotArea = 140;
		this.hasLight = false;
	}
	
	public void setup(double px, double py, double pz, double yaw, double pitch) {
		offset.xCoord = origPos.xCoord = px;
		offset.yCoord = origPos.yCoord = py;
		offset.zCoord = origPos.zCoord = pz;
		origPitch = rotPitch = pitch;
		origYaw = rotYaw = yaw;
	}
	
	@Override
	protected void onUpdate() {
		long time = Minecraft.getSystemTime();
		//System.out.println("WTF");
		
		if(lastChangeTime == 0) lastChangeTime = time;
		//Random animation
		if(time - lastChangeTime > changeWait) {
			randomWaitTime();
			lastChangeTime = time;
			curIndex = RNG.nextInt(ANIM.length);
			randomTransform();
		}
		
	}

	@Override
	public ResourceLocation getTexture() {
		return ANIM[curIndex];
	}
	
	private void randomWaitTime() {
		changeWait = rangeRand(waitFrom, waitTo);
	}
	
	private void randomTransform() {
		if(RNG.nextDouble() > 0.7)
			randVec(offset, origPos, offArea);
		rotPitch = origPitch + midRand(rotArea);
		rotYaw = origYaw + 2 * midRand(rotArea);
	}
	
	private void randVec(Vec3 vec, Vec3 orig, double area) {
		vec.xCoord = orig.xCoord + midRand(area);
		vec.yCoord = orig.yCoord + midRand(area);
		vec.zCoord = orig.zCoord + midRand(area);
	}
	
	private static int rangeRand(int from, int to) {
		return RNG.nextInt(to - from) + from;
	}
	
	/**
	 * Generate a random between(-0.5iv, 0.5iv)
	 */
	private static double midRand(double iv) {
		return (RNG.nextDouble() - 0.5) * iv;
	}

}
