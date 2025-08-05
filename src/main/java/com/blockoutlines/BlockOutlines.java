package com.blockoutlines;

import com.blockoutlines.entity.OutlineBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockOutlines implements ModInitializer {
	public static final String MOD_ID = "block-outlines";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityType<OutlineBlockEntity> OUTLINE_BLOCK = Registry.register(
		Registries.ENTITY_TYPE,
		Identifier.of(MOD_ID, "outline_block"),
		EntityType.Builder.<OutlineBlockEntity>create(OutlineBlockEntity::new, SpawnGroup.MISC)
			.eyeHeight(0.5f)
			.maxTrackingRange(4)
			.trackingTickInterval(20)
			.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "outline_block")))
	);

	@Override
	public void onInitialize() {
		LOGGER.info("Block Outlines mod initialized!");
	}
}