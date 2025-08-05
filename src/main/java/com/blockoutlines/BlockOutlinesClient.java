package com.blockoutlines;

import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import com.blockoutlines.entity.OutlineBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockOutlinesClient implements ClientModInitializer {
    
    private static KeyBinding toggleOutlinesKey;
    private boolean outlinesEnabled = false;
    private Set<BlockPos> trackedDiamondBlocks = new HashSet<>();
    private Set<Integer> spawnedOutlineEntities = new HashSet<>();
    private int scanRadius = 16;
    private int tickCounter = 0;
    
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BlockOutlines.OUTLINE_BLOCK, FallingBlockEntityRenderer::new);
        
        toggleOutlinesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.block-outlines.toggle_outlines",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.block-outlines.general"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleOutlinesKey.wasPressed()) {
                toggleOutlines(client);
            }
            
            if (outlinesEnabled && client.player != null && client.world != null) {
                tickCounter++;
                // Scan every 10 ticks (0.5 seconds) for more responsive updates
                if (tickCounter % 10 == 0) {
                    updateDiamondBlockOutlines(client);
                }
            }
        });
        
        BlockOutlines.LOGGER.info("Block Outlines client initialized!");
    }
    
    private void toggleOutlines(MinecraftClient client) {
        outlinesEnabled = !outlinesEnabled;
        
        if (!outlinesEnabled) {
            clearAllOutlines(client);
        } else {
            // Clear tracked blocks to force fresh scan
            trackedDiamondBlocks.clear();
            // Immediately scan when enabled
            updateDiamondBlockOutlines(client);
        }
        
        BlockOutlines.LOGGER.info("Block outlines {}", outlinesEnabled ? "enabled" : "disabled");
        
        // Send chat message to player
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal("Block outlines " + (outlinesEnabled ? "enabled" : "disabled")), false);
        }
    }
    
    private void updateDiamondBlockOutlines(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        ClientWorld world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        Set<BlockPos> currentDiamondBlocks = new HashSet<>();
        
        BlockOutlines.LOGGER.debug("Scanning for diamond blocks/ore around player at {}", playerPos);
        
        int scannedBlocks = 0;
        int diamondBlocksFound = 0;
        
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    scannedBlocks++;
                    
                    // Check if chunk is loaded
                    if (!world.isChunkLoaded(pos)) {
                        continue;
                    }
                    
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isOf(Blocks.DIAMOND_BLOCK) || blockState.isOf(Blocks.DIAMOND_ORE) || blockState.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                        currentDiamondBlocks.add(pos);
                        diamondBlocksFound++;
                        BlockOutlines.LOGGER.info("Found diamond block/ore at {} - Block: {}", pos, blockState.getBlock().getName().getString());
                    }
                }
            }
        }
        
        BlockOutlines.LOGGER.info("Scanned {} blocks, found {} diamond blocks/ore", scannedBlocks, diamondBlocksFound);
        
        Set<BlockPos> newBlocks = new HashSet<>(currentDiamondBlocks);
        newBlocks.removeAll(trackedDiamondBlocks);
        
        Set<BlockPos> removedBlocks = new HashSet<>(trackedDiamondBlocks);
        removedBlocks.removeAll(currentDiamondBlocks);
        
        // Debug logging
        if (!currentDiamondBlocks.isEmpty()) {
            BlockOutlines.LOGGER.info("Found {} diamond blocks", currentDiamondBlocks.size());
        }
        
        if (!newBlocks.isEmpty()) {
            BlockOutlines.LOGGER.info("Spawning outlines for {} new diamond blocks/ore", newBlocks.size());
        }
        
        for (BlockPos pos : newBlocks) {
            spawnOutlineAtDiamondBlock(world, pos);
        }
        
        for (BlockPos pos : removedBlocks) {
            removeOutlineAtPosition(world, pos);
        }
        
        trackedDiamondBlocks = currentDiamondBlocks;
    }
    
    private void spawnOutlineAtDiamondBlock(ClientWorld world, BlockPos diamondPos) {
        // Spawn 1 block above the diamond ore to be visible (not inside the block)
        Vec3d spawnPos = Vec3d.of(diamondPos).add(0.5, 0.0, 0.5);
        
        // Create a glowing block that's more visible through solid blocks
        // It can't be the same block as the block it's outlining, so we use a diamond block
        OutlineBlockEntity outlineEntity = new OutlineBlockEntity(BlockOutlines.OUTLINE_BLOCK, world, Blocks.DIAMOND_BLOCK.getDefaultState());
        outlineEntity.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
        
        if (addClientEntity(world, outlineEntity)) {
            spawnedOutlineEntities.add(outlineEntity.getId());
            BlockOutlines.LOGGER.info("Spawned diamond ore outline at exact position {}, {}, {}", 
                spawnPos.x, spawnPos.y, spawnPos.z);
        }
    }
    
    
    private void removeOutlineAtPosition(ClientWorld world, BlockPos pos) {
        Vec3d targetPos = Vec3d.of(pos).add(0.5, 0.0, 0.5);
        
        List<Entity> nearbyEntities = world.getOtherEntities(null, 
            net.minecraft.util.math.Box.of(targetPos, 4.0, 4.0, 4.0));
        
        for (Entity entity : nearbyEntities) {
            if (entity instanceof OutlineBlockEntity && spawnedOutlineEntities.contains(entity.getId())) {
                entity.discard();
                spawnedOutlineEntities.remove(entity.getId());
            }
        }
    }
    
    private void clearAllOutlines(MinecraftClient client) {
        if (client.world == null) return;
        
        ClientWorld world = client.world;
        for (Integer entityId : new HashSet<>(spawnedOutlineEntities)) {
            Entity entity = world.getEntityById(entityId);
            if (entity instanceof OutlineBlockEntity) {
                entity.discard();
            }
        }
        
        spawnedOutlineEntities.clear();
        trackedDiamondBlocks.clear();
    }
    
    private boolean addClientEntity(ClientWorld world, Entity entity) {
        try {
            int entityId = generateClientEntityId();
            entity.setId(entityId);
            world.addEntity(entity);
            return true;
        } catch (Exception e) {
            BlockOutlines.LOGGER.error("Failed to add client entity: {}", e.getMessage());
            return false;
        }
    }
    
    private int generateClientEntityId() {
        return -(int)(System.currentTimeMillis() % Integer.MAX_VALUE);
    }
}