package com.blockoutlines;

import com.blockoutlines.client.OutlineBlockEntityRenderer;
import com.blockoutlines.entity.OutlineBlockEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
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
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockOutlinesClient implements ClientModInitializer {
    
    private static KeyBinding toggleOutlinesKey;
    private boolean outlinesEnabled = false;
    private Set<BlockPos> trackedDiamondBlocks = new HashSet<>();
    private Map<BlockPos, Integer> outlineEntityMap = new HashMap<>();
    private AtomicInteger entityIdCounter = new AtomicInteger(1000000);
    private int scanRadius = 16;
    private int tickCounter = 0;
    
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BlockOutlines.OUTLINE_BLOCK, OutlineBlockEntityRenderer::new);
        
        toggleOutlinesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.block-outlines.toggle_outlines",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.block-outlines.general"
        ));
        
        // Clear outlines when world changes
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof OutlineBlockEntity) {
                outlineEntityMap.values().removeIf(id -> id == entity.getId());
            }
        });
        
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
            outlineEntityMap.clear();
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
        
        
        
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    // Check if chunk is loaded
                    if (!world.isChunkLoaded(pos)) {
                        continue;
                    }
                    
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isOf(Blocks.DIAMOND_BLOCK) || blockState.isOf(Blocks.DIAMOND_ORE) || blockState.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                        currentDiamondBlocks.add(pos);
                    }
                }
            }
        }
        
        
        Set<BlockPos> newBlocks = new HashSet<>(currentDiamondBlocks);
        newBlocks.removeAll(trackedDiamondBlocks);
        
        Set<BlockPos> removedBlocks = new HashSet<>(trackedDiamondBlocks);
        removedBlocks.removeAll(currentDiamondBlocks);
        
        
        
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
            outlineEntityMap.put(diamondPos, outlineEntity.getId());
        }
    }
    
    
    private void removeOutlineAtPosition(ClientWorld world, BlockPos pos) {
        Integer entityId = outlineEntityMap.get(pos);
        if (entityId != null) {
            Entity entity = world.getEntityById(entityId);
            if (entity instanceof OutlineBlockEntity) {
                entity.discard();
            }
            outlineEntityMap.remove(pos);
        }
    }
    
    private void clearAllOutlines(MinecraftClient client) {
        if (client.world == null) return;
        
        ClientWorld world = client.world;
        for (Integer entityId : new HashSet<>(outlineEntityMap.values())) {
            Entity entity = world.getEntityById(entityId);
            if (entity instanceof OutlineBlockEntity) {
                entity.discard();
            }
        }
        
        outlineEntityMap.clear();
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
        return -entityIdCounter.incrementAndGet();
    }
}