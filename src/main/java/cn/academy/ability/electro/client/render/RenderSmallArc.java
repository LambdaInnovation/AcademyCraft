/**
 * 
 */
package cn.academy.ability.electro.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.client.render.IPointFactory.NormalVert;
import cn.academy.misc.client.render.eff.Piece;

/**
 * Small arc effect renderer. This baseclass is usually used on surroundings rendering and skill prepration. <br/>
 * You must create ONE instance for each <code>SkillState</code> call(Or, each player), for varying effects.
 * @author WeathFolD
 */
public class RenderSmallArc {

	private static Random RNG = new Random();
	private IPointFactory factory;
	private List<Piece> alivePieces = new ArrayList<Piece>();
	long lastRefTime;
	int refWait;
	public boolean showing = false;
	public int itensity = 8, iten_off = 5;
	public int showFrom = 300, showTo = 500;
	public int hideFrom = 300, hideTo = 600;
	public double sizeFrom = 0.5, sizeTo = 0.9;
	
	/**
	 * Initialize a smallArc render with given point generator.
	 */
	public RenderSmallArc(IPointFactory fac, double size, int _itensity) {
		factory = fac;
		sizeFrom = size * 0.8;
		sizeTo = size * 1.2;
		itensity = _itensity;
		iten_off = (int) (itensity * 0.6);
		update();
	}
	
	public void draw() {
		update();
		GL11.glPushMatrix(); {
			for(Piece p : alivePieces) {
				p.draw();
			}
		} GL11.glPopMatrix();
	}
	
	private void update() {
		//Showing and hiding
		long time = Minecraft.getSystemTime();
		if(time - lastRefTime > refWait || lastRefTime == 0) {
			lastRefTime = time;
			if(showing) {
				refWait = randIntv(showFrom, showTo);
			} else {
				refWait = randIntv(hideFrom, hideTo);
			}
			showing = !showing;
			if(!showing) {
				alivePieces.clear();
			} else {
				int cnt = itensity + RNG.nextInt(iten_off);
				for(int i = 0; i < cnt; ++i) {
					alivePieces.add(createPiece());
				}
			}
		}
	}
	
	/**
	 * Setup a piece
	 * @return
	 */
	private PieceSmallArc createPiece() {
		PieceSmallArc piece = new PieceSmallArc(randIntv(sizeFrom, sizeTo));
		NormalVert nv = factory.next();
		double yaw = Math.atan2(nv.normal.xCoord, nv.normal.zCoord) * 180 / Math.PI;
		double pitch = Math.atan2(nv.normal.yCoord, 
			Math.sqrt(nv.normal.xCoord * nv.normal.xCoord + nv.normal.zCoord * nv.normal.zCoord)) * 180 / Math.PI;
		piece.setup(nv.vert.xCoord, nv.vert.yCoord, nv.vert.zCoord, yaw, pitch);
		return piece;
	}
	
	private static int randIntv(int fr, int to) {
		return RNG.nextInt(to - fr) + fr;
	}
	
	private static double randIntv(double fr, double to) {
		return RNG.nextDouble() * (to - fr)+ fr;
	}

}
