package com.blockoutlines;

import com.blockoutlines.client.gui.BlockOutlinesConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockOutlinesClient implements ClientModInitializer {
    
    public static final String MOD_ID = "block-outlines";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static BlockOutlinesClient instance;
    
    // Configuration
    private boolean enabled = false;
    private int scanRadius = 32; // Blocks to scan around player
    private int scanRate = 20; // Ticks between scans (20 = 1 second)
    private Block targetBlock = Blocks.DIAMOND_ORE; // Configurable target block
    
    // State tracking
    private int tickCounter = 0;
    private final Set<BlockPos> trackedBlockPositions = ConcurrentHashMap.newKeySet();
    private final Set<BlockPos> previousPositions = new HashSet<>();
    
    // Keybinding
    private static KeyBinding toggleOutlinesKey;
    private static KeyBinding openConfigKey;
    
    @Override
    public void onInitializeClient() {
        instance = this;
        
        // Register keybinding
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
        
        // Register client tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle keybinding
            while (toggleOutlinesKey.wasPressed()) {
                toggleOutlines(client);
            }
            
            while (openConfigKey.wasPressed()) {
                openConfigScreen(client);
            }
            
            // Perform periodic scans if enabled
            if (enabled && client.player != null && client.world != null) {
                tickCounter++;
                if (tickCounter % scanRate == 0) {
                    scanForTargetBlocks(client);
                    tickCounter = 0; // Reset counter to prevent overflow
                }
            }
        });
        
        LOGGER.info("Block Outlines client initialized!");
    }
    
    private void toggleOutlines(MinecraftClient client) {
        enabled = !enabled;
        
        if (!enabled) {
            // Clear all tracked positions when disabled
            trackedBlockPositions.clear();
            previousPositions.clear();
        } else {
            // Immediate scan when enabled
            scanForTargetBlocks(client);
        }
        
        // Send feedback to player
        if (client.player != null) {
            String status = enabled ? "enabled" : "disabled";
            client.player.sendMessage(
                Text.literal("Block Outlines " + status), 
                false
            );
        }
        
        LOGGER.info("Block Outlines {}", enabled ? "enabled" : "disabled");
    }
    
    private void scanForTargetBlocks(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        
        ClientWorld world = client.world;
        BlockPos playerPos = client.player.getBlockPos();
        Set<BlockPos> currentPositions = new HashSet<>();
        
        // Scan in a cube around the player
        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    
                    // Check if this position has the target block
                    BlockState blockState = world.getBlockState(pos);
                    if (blockState.isOf(targetBlock) || 
                        (targetBlock == Blocks.DIAMOND_ORE && blockState.isOf(Blocks.DEEPSLATE_DIAMOND_ORE))) {
                        currentPositions.add(pos.toImmutable());
                    }
                }
            }
        }
        
        // Update tracked positions
        synchronized (trackedBlockPositions) {
            trackedBlockPositions.clear();
            trackedBlockPositions.addAll(currentPositions);
        }
        
        // Log changes for debugging
        if (!currentPositions.equals(previousPositions)) {
            int found = currentPositions.size();
            if (found > 0) {
                LOGGER.debug("Found {} target block(s) within {} blocks", found, scanRadius);
            }
            previousPositions.clear();
            previousPositions.addAll(currentPositions);
        }
    }
    
    private void openConfigScreen(MinecraftClient client) {
        client.setScreen(new BlockOutlinesConfigScreen(client.currentScreen, this));
    }
    
    // Public getters for mixin access
    public static BlockOutlinesClient getInstance() {
        return instance;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            
            if (!enabled) {
                trackedBlockPositions.clear();
                previousPositions.clear();
            } else {
                MinecraftClient client = MinecraftClient.getInstance();
                scanForTargetBlocks(client);
            }
            
            LOGGER.info("Block Outlines {}", enabled ? "enabled" : "disabled");
        }
    }
    
    public Set<BlockPos> getTrackedBlockPositions() {
        return new HashSet<>(trackedBlockPositions);
    }
    
    public Block getTargetBlock() {
        return targetBlock;
    }
    
    public void setTargetBlock(Block block) {
        if (this.targetBlock != block) {
            this.targetBlock = block;
            
            // Clear existing tracked positions to force fresh scan
            trackedBlockPositions.clear();
            previousPositions.clear();
            
            if (enabled) {
                MinecraftClient client = MinecraftClient.getInstance();
                scanForTargetBlocks(client);
            }
        }
    }
    
    // Configuration getters/setters
    public int getScanRadius() {
        return scanRadius;
    }
    
    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(8, Math.min(64, radius));
    }
    
    public int getScanRate() {
        return scanRate;
    }
    
    public void setScanRate(int rate) {
        this.scanRate = Math.max(5, Math.min(100, rate));
    }
    
    // Legacy methods for backward compatibility with config screen
    public boolean isOutlinesEnabled() {
        return enabled;
    }
    
    public void setOutlinesEnabled(boolean enabled) {
        setEnabled(enabled);
    }
}