package com.blockoutlines;

import com.blockoutlines.client.OutlineBlockEntityRenderer;
import com.blockoutlines.client.gui.BlockOutlinesConfigScreen;
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
    
    private static BlockOutlinesClient instance;
    private static KeyBinding toggleOutlinesKey;
    private static KeyBinding openConfigKey;
    private boolean outlinesEnabled = false;
    private Set<BlockPos> trackedDiamondOres = new HashSet<>();
    private Map<BlockPos, Integer> outlineEntityMap = new HashMap<>();
    private AtomicInteger entityIdCounter = new AtomicInteger(1000000);
    private int scanRadius = 16;
    private int scanRate = 10; // How many ticks between scans (10 = 0.5 seconds)
    private int tickCounter = 0;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        EntityRendererRegistry.register(BlockOutlines.OUTLINE_BLOCK, OutlineBlockEntityRenderer::new);
        
        toggleOutlinesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.block-outlines.toggle_outlines",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.block-outlines.general"
        ));
        
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.block-outlines.open_config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
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
            
            while (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }
            
            if (outlinesEnabled && client.player != null && client.world != null) {
                tickCounter++;
                // Scan based on configurable scan rate
                if (tickCounter % scanRate == 0) {
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
            trackedDiamondOres.clear();
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
        Set<BlockPos> currentDiamondOres = new HashSet<>();
        
        
        
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isOf(Blocks.DIAMOND_ORE)) {
                        currentDiamondOres.add(pos);
                    }
                }
            }
        }
        
        
        Set<BlockPos> newBlocks = new HashSet<>(currentDiamondOres);
        newBlocks.removeAll(trackedDiamondOres);
        
        Set<BlockPos> removedBlocks = new HashSet<>(trackedDiamondOres);
        removedBlocks.removeAll(currentDiamondOres);
        
        
        
        for (BlockPos pos : newBlocks) {
            spawnOutlineAtDiamondBlock(world, pos);
        }
        
        for (BlockPos pos : removedBlocks) {
            removeOutlineAtPosition(world, pos);
        }
        
        trackedDiamondOres = currentDiamondOres;
    }
    
    private void spawnOutlineAtDiamondBlock(ClientWorld world, BlockPos diamondPos) {
        // Spawn 1 block above the diamond ore to be visible (not inside the block)
        Vec3d spawnPos = Vec3d.of(diamondPos).add(0.5, 0.0, 0.5);
        
        // Create a glowing block that's more visible through solid blocks
        OutlineBlockEntity outlineEntity = new OutlineBlockEntity(BlockOutlines.OUTLINE_BLOCK, world, Blocks.DIAMOND_ORE.getDefaultState());
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
        trackedDiamondOres.clear();
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
    
    private void openConfigScreen(MinecraftClient client) {
        client.setScreen(new BlockOutlinesConfigScreen(client.currentScreen, this));
    }
    
    public boolean isOutlinesEnabled() {
        return outlinesEnabled;
    }
    
    public void setOutlinesEnabled(boolean enabled) {
        if (this.outlinesEnabled != enabled) {
            this.outlinesEnabled = enabled;
            MinecraftClient client = MinecraftClient.getInstance();
            
            if (!enabled) {
                clearAllOutlines(client);
            } else {
                trackedDiamondOres.clear();
                outlineEntityMap.clear();
                updateDiamondBlockOutlines(client);
            }
            
            BlockOutlines.LOGGER.info("Block outlines {}", enabled ? "enabled" : "disabled");
            
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("Block outlines " + (enabled ? "enabled" : "disabled")), false);
            }
        }
    }
    
    public int getScanRadius() {
        return scanRadius;
    }
    
    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(16, Math.min(64, radius));
    }
    
    public int getScanRate() {
        return scanRate;
    }
    
    public void setScanRate(int rate) {
        this.scanRate = Math.max(1, Math.min(20, rate));
    }
    
    public static BlockOutlinesClient getInstance() {
        return instance;
    }
}