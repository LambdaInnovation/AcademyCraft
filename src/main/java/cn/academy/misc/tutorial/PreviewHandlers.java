package cn.academy.misc.tutorial;

import net.minecraft.block.Block;
import net.minecraft.item.Item;

public final class PreviewHandlers {

	private PreviewHandlers() {}

	public static final IPreviewHandler nothing = () -> {};

	public static IPreviewHandler drawsBlock(Block block) {
		return () -> {
			// TODO Implement
		};
	}

	public static IPreviewHandler drawsItem(Item item) {
		return () -> {
			// TODO
		};
	}

}
