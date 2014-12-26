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
	
	private int waitFrom = 50, waitTo = 150; //The random waiting time interval.
	private long lastChangeTime;
	private int changeWait;
	private int curIndex;
	
	private int showFrom = 100, showTo = 500; //The random showing(also hiding) time length interval
	private int showTime;
	private long lastShowTime;
	private boolean isShowing;
	
	private double offArea, rotArea;
	private double offTolerance;
	
	private Vec3 origPos = vec(), origRot = vec();

	public PieceSmallArc(double size) {
		super(size, size);
		randomWaitTime();
		randomShowTime();
		offArea = size * 0.8;
		rotArea = 30;
		offTolerance = size * 1.2;
		isShowing = true;
	}
	
	public void setup(double px, double py, double pz, double rx, double ry, double rz) {
		offset.xCoord = origPos.xCoord = px;
		offset.yCoord = origPos.yCoord = py;
		offset.zCoord = origPos.zCoord = pz;
		rotation.xCoord = origRot.xCoord = rx;
		rotation.yCoord = origRot.yCoord = ry;
		rotation.zCoord = origRot.zCoord = rz;
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
		}
		
		//Random show/hide
		if(lastShowTime == 0) lastShowTime = time;
		if(time - lastShowTime > showTime) {
			randomShowTime();
			randomTransform();
			lastShowTime = time;
			isShowing = !isShowing;
		}
	}

	@Override
	public ResourceLocation getTexture() {
		return ANIM[curIndex];
	}
	
	private void randomWaitTime() {
		changeWait = rangeRand(waitFrom, waitTo);
	}
	
	private void randomShowTime() {
		showTime = rangeRand(showFrom, showTo);
	}
	
	private void randomTransform() {
		randVec(offset, offArea, origPos, offTolerance);
		randVec(rotation, rotArea);
	}
	
	private void randVec(Vec3 vec, double area) {
		vec.xCoord += midRand(area);
		vec.yCoord += midRand(area);
		vec.zCoord += midRand(area);
	}
	
	private void randVec(Vec3 vec, double area, Vec3 orig, double cst) {
		randVec(vec, area);
		if(Math.abs(orig.xCoord - vec.xCoord) < cst) 
			vec.xCoord = orig.xCoord;
		if(Math.abs(orig.yCoord - vec.yCoord) < cst) 
			vec.yCoord = orig.yCoord;
		if(Math.abs(orig.zCoord - vec.zCoord) < cst) 
			vec.zCoord = orig.zCoord;
	}
	
	public boolean doesDraw() {
		return isShowing;
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
