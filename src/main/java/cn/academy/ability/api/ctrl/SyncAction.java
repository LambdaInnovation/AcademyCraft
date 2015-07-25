package cn.academy.ability.api.ctrl;

import java.util.UUID;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * @author EAirPeter
 */
public abstract class SyncAction {

	protected UUID uuid;
	int intv = -1;
	int lastInformed = 0;
	
	protected final boolean isRemote = FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT);
	
	/**
	 * The associated player
	 * null for started from server
	 */
	protected EntityPlayer player = null;
	
	/**
	 * Construct a SyncAction
	 * Notice: Every subclass of SyncAction must have a constructor with no parameter
	 * @param interval Server side will send an update to client side every interval ticks, while -1 for never.
	 */
	protected SyncAction(int interval) {
		intv = interval;
		state = State.CREATED;
		uuid = UUID.randomUUID();
	}
	
	private State state;
	
	public static enum State {
		CREATED,
		STARTED,
		ENDED,
		ABORTED,
	}
	
	/* start from client
	 * send to server(start)
	 * server reply, server.onStart or nothing
	 * corresponding: client.onStart or nothing
	 */
	/* start from server
	 * send to client(start), server.onStart
	 * client.onStart or abortAtServer
	 */
	/**
	 * Called when this start at both sides
	 */
	public void onStart() {
	}
	
	/* (server) tick and send(every ${interval})
	 * server inform
	 */
	/**
	 * Called every tick at both sides
	 */
	public void onTick() {
	}
	
	
	/* end from client
	 * send to server
	 * server reply, server inform(final) and (server.onAbort or server.onEnd) 
	 * client.onUpdate and (corresponding: client.onAbort or client.onEnd) 
	 */
	/* end from server
	 * send to client, server inform(final) and server.onEnd
	 * client.onUpdate and client.onEnd
	 */
	/**
	 * Called when ended at both sides
	 */
	public void onEnd() {
	}
	
	/* abort from client
	 * send to server
	 * server inform(final) and server.onAbort
	 * client.onUpdate and client.onAbort
	 */
	/* abort from server
	 * server inform(final) and server.onAbort
	 * client.onUpdate and client.onAbort
	 */
	/**
	 * Called when aborted at both sides
	 * This is nothing to do with network
	 * If any, please use NBT operation(Final)
	 */
	public void onAbort() {
	}
	
	/**
	 * called after onEnd or onAbort
	 */
	public void onFinalize() {
	}
	
	public void readNBTStart(NBTTagCompound tag) {
	}
	public void readNBTUpdate(NBTTagCompound tag) {
	}
	public void readNBTFinal(NBTTagCompound tag) {
	}
	public void writeNBTStart(NBTTagCompound tag) {
	}
	public void writeNBTUpdate(NBTTagCompound tag) {
	}
	public void writeNBTFinal(NBTTagCompound tag) {
	}
	
	/**
	 * @return Whether this SyncAction is local. a.k.a. Is at the side where it started.
	 * 	<br/>Returns true when: <br/>
	 * 	* At server and started at server <br/>
	 * 	* At the client that constructed this SyncAction
	 */
	public final boolean isLocal() {
		if(isRemote)
			return isLocalClient();
		return player == null;
	}
	
	@SideOnly(Side.CLIENT)
	private boolean isLocalClient() {
		return Minecraft.getMinecraft().thePlayer.equals(player);
	}
	
	/**
	 * @return The action's state
	 */
	public final State getState() {
		return state;
	}
	
	private static final String NBT_UUID = "0";
	private static final String NBT_STATE = "1";
	private static final String NBT_INTERVAL = "2";
	private static final String NBT_OBJECT = "3";
	
	final void setNBTStart(NBTTagCompound tag) {
		uuid = UUID.fromString(tag.getString(NBT_UUID));
		intv = tag.getInteger(NBT_INTERVAL);
		if (tag.hasKey(NBT_OBJECT))
			readNBTStart(tag.getCompoundTag(NBT_OBJECT));
	}
	final void setNBTUpdate(NBTTagCompound tag) {
		if (tag.hasKey(NBT_OBJECT))
			readNBTUpdate(tag.getCompoundTag(NBT_OBJECT));
	}
	final void setNBTFinal(NBTTagCompound tag) {
		if (tag.hasKey(NBT_OBJECT))
			readNBTFinal(tag.getCompoundTag(NBT_OBJECT));
	}
	final NBTTagCompound getNBTStart() {
		NBTTagCompound tag = new NBTTagCompound();
		tag.setString(NBT_UUID, uuid.toString());
		tag.setInteger(NBT_INTERVAL, intv);
		NBTTagCompound obj = new NBTTagCompound();
		writeNBTStart(obj);
		tag.setTag(NBT_OBJECT, obj);
		return tag;
	}
	final NBTTagCompound getNBTUpdate() {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagCompound obj = new NBTTagCompound();
		writeNBTUpdate(obj);
		tag.setTag(NBT_OBJECT, obj);
		return tag;
	}
	final NBTTagCompound getNBTFinal() {
		NBTTagCompound tag = new NBTTagCompound();
		NBTTagCompound obj = new NBTTagCompound();
		writeNBTFinal(obj);
		tag.setTag(NBT_OBJECT, obj);
		return tag;
	}
	
	final void start() {
		state = State.STARTED;
		onStart();
	}
	
	final void end(NBTTagCompound tag) {
		System.out.println("SA#END0");
		if (state.equals(State.STARTED)) {
			state = State.ENDED;
			setNBTFinal(tag);
			onEnd();
			onFinalize();
		}
	}
	
	final NBTTagCompound end() {
		NBTTagCompound tag = TAG_EMPTY;
		if (state.equals(State.STARTED)) {
			state = State.ENDED;
			tag = getNBTFinal();
			onEnd();
			onFinalize();
		}
		return tag;
	}
	
	final void abort(NBTTagCompound tag) {
		if (state.equals(State.STARTED)) {
			state = State.ABORTED;
			setNBTFinal(tag);
			onAbort();
			onFinalize();
		}
	}
	
	final NBTTagCompound abort() {
		NBTTagCompound tag = TAG_EMPTY;
		if (state.equals(State.STARTED)) {
			state = State.ABORTED;
			tag = getNBTFinal();
			onAbort();
			onFinalize();
		}
		return tag;
	}

	static final UUID getUUIDFromNBT(NBTTagCompound tag) {
		if (tag.hasKey(NBT_UUID))
			return UUID.fromString(tag.getString(NBT_UUID));
		else
			return null;
	}
	
	static final NBTTagCompound TAG_EMPTY = new NBTTagCompound();
	
}
