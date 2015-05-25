package cn.academy.core.entity;

import net.minecraft.util.Vec3;

import org.lwjgl.util.vector.Vector2f;

/**
 * States used in RenderRay classes.
 * @author WeAthFolD
 */
public interface IRay {
	
	Vec3 getPosition();
	
	/**
	 * Return a normalized vector representing ray's direction.
	 */
	Vec3 getLookingDirection();
	
	boolean needsViewOptimize();
	
	double getLength();
	
	/**
	 * @return An alpha multiplyer. Can be used for blend out.
	 */
	double getAlpha();
	
}
