package cn.academy.core.entity;

import net.minecraft.util.Vec3;

import org.lwjgl.util.vector.Vector2f;

/**
 * States used in RenderRay classes.
 * @author WeAthFolD
 */
public interface IRay {
	
	void onRenderTick();
	
	Vec3 getPosition();
	
	/**
	 * Return a normalized vector representing ray's direction.
	 */
	Vec3 getLookingDirection();
	
	/**
	 * @return If this ray is spawned at player's hand and need to be treated differently for 1st and 3rd person
	 */
	boolean needsViewOptimize();
	
	double getLength();
	
	//---Advanced parameters
	/**
	 * @return An alpha multiplyer. Can be used for blend out.
	 */
	double getAlpha();
	
	/**
	 * Get the advance distance of the ray starting point. Can be used for blend out.
	 */
	double getStartFix();
	
	/**
	 * Get the current ray width multiplyer. Used for blending
	 */
	double getWidth();
	
}
