package cn.academy.ability.api.ctrl.test;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cn.academy.ability.api.ctrl.SyncAction;

public class AT1 extends SyncAction {

	public AT1() {
		super(20);
	}

	@Override
	public void onStart() {
		msg("onStart");
	}

	@Override
	public void onTick() {
		//msg("onTick");
		if (!isRemote) {
			data1 = rng.nextInt(2147483647);
		}
	}

	@Override
	public void onEnd() {
		msg("onEnd " + data1 + " " + data2);
	}

	@Override
	public void onAbort() {
		msg("onAbort " + data1 + " " + data2);
	}
	
	@Override
	public void readNBTStart(NBTTagCompound tag) {
		data2 = tag.getInteger("data2");
		msg("read start " + data1 + " " + data2);
	}
	@Override
	public void readNBTUpdate(NBTTagCompound tag) {
		data1 = tag.getInteger("data1");
		msg("read update " + data1 + " " + data2);
	}
	@Override
	public void readNBTFinal(NBTTagCompound tag) {
		readNBTUpdate(tag);
		data2 = tag.getInteger("data2");
		msg("read final " + data1 + " " + data2);
	}
	@Override
	public void writeNBTStart(NBTTagCompound tag) {
		tag.setInteger("data2", data2);
		msg("write start " + data1 + " " + data2);
	}
	@Override
	public void writeNBTUpdate(NBTTagCompound tag) {
		tag.setInteger("data1", data1);
		msg("write update " + data1 + " " + data2);
	}
	@Override
	public void writeNBTFinal(NBTTagCompound tag) {
		writeNBTUpdate(tag);
		tag.setInteger("data2", data2);
		msg("write final " + data1 + " " + data2);
	}
	
	public static void msg(String msg) {
		TM.msg("AT1", msg);
	}
	
	int data1, data2;
	Random rng = new Random();
	
}
