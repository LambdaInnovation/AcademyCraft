package cn.academy.core.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public abstract class KeyHandler {

	public void onKeyDown() {}
	
	public void onKeyUp() {}
	
	/**
	 * This happens when the KeyBinding is a non-global one, 
	 * and player opens any GUI or jumps out of the game.
	 */
	public void onKeyAbort() {}
	
	public void onKeyTick() {}
	
	protected Minecraft getMC() {
		return Minecraft.getMinecraft();
	}
	
	protected EntityPlayer getPlayer() {
		return getMC().thePlayer;
	}
	
}
