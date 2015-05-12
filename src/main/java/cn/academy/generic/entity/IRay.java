package cn.academy.generic.entity;

import net.minecraft.util.Vec3;

import org.lwjgl.util.vector.Vector2f;

/**
 * States used in RenderRay classes.
 * @author WeAthFolD
 */
public interface IRay {
	
	Vec3 getPosition();
	
	Vector2f getLookingAngle();
	
	double getLength();
	
}
