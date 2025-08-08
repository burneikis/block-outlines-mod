package com.blockoutlines;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockOutlines implements ModInitializer {
	public static final String MOD_ID = "block-outlines";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Block Outlines mod initialized!");
	}
}