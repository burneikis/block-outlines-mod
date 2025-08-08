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
    private int outlineColor = 0xFFFFFF; // Default white color (RGB)
    private boolean autoColorMode = false; // Whether to automatically update color when target block changes
    
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
            
            // Auto update color if auto color mode is enabled
            if (autoColorMode) {
                updateAutoColor();
            }
            
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
    
    public int getOutlineColor() {
        return outlineColor;
    }
    
    public void setOutlineColor(int color) {
        this.outlineColor = color & 0xFFFFFF; // Ensure it's a valid RGB color
    }
    
    public boolean isAutoColorMode() {
        return autoColorMode;
    }
    
    public void setAutoColorMode(boolean autoColorMode) {
        this.autoColorMode = autoColorMode;
        
        // If enabling auto color mode, update the color immediately
        if (autoColorMode) {
            updateAutoColor();
        }
    }
    
    private void updateAutoColor() {
        // Extract color using the same logic as the ColorPickerScreen
        int autoColor = extractBlockColor(targetBlock);
        setOutlineColor(autoColor);
        
        // Optional: Log the color change
        LOGGER.info("Auto color updated for {}: #{}", 
            targetBlock.getName().getString(), 
            String.format("%06X", autoColor));
    }
    
    private int extractBlockColor(Block targetBlock) {
        // Try multiple methods to get the block's representative color
        
        // Method 1: Check for ore-specific colors based on their drops
        int oreColor = getOreDropColor(targetBlock);
        if (oreColor != -1) {
            return oreColor;
        }
        
        // Method 2: Try to get the map color from the block's default state
        try {
            net.minecraft.block.BlockState defaultState = targetBlock.getDefaultState();
            net.minecraft.block.MapColor mapColor = defaultState.getMapColor(null, null);
            
            if (mapColor != null && mapColor != net.minecraft.block.MapColor.CLEAR) {
                return mapColor.color;
            }
        } catch (Exception e) {
            // Fallback if map color extraction fails
        }
        
        // Method 3: Try to get color from material/block properties
        try {
            net.minecraft.block.BlockState defaultState = targetBlock.getDefaultState();
            // Some blocks have material colors that can be accessed
            if (defaultState.hasBlockEntity()) {
                // Skip blocks with entities for now to avoid complexity
                return getBlockColorFallback(targetBlock);
            }
        } catch (Exception e) {
            // Continue to fallback
        }
        
        // Method 4: Fallback to predefined colors for common blocks
        return getBlockColorFallback(targetBlock);
    }
    
    private int getOreDropColor(Block block) {
        // Return colors based on what the ores actually drop (more vibrant and accurate)
        if (block == net.minecraft.block.Blocks.DIAMOND_ORE || 
            block == net.minecraft.block.Blocks.DEEPSLATE_DIAMOND_ORE) {
            return 0x00FFFF; // Bright cyan like diamond items
        } else if (block == net.minecraft.block.Blocks.EMERALD_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_EMERALD_ORE) {
            return 0x00FF00; // Bright green like emerald items
        } else if (block == net.minecraft.block.Blocks.GOLD_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_GOLD_ORE ||
                   block == net.minecraft.block.Blocks.NETHER_GOLD_ORE) {
            return 0xFFD700; // Bright gold like gold items
        } else if (block == net.minecraft.block.Blocks.IRON_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_IRON_ORE) {
            return 0xD8D8D8; // Light gray like iron ingots
        } else if (block == net.minecraft.block.Blocks.COPPER_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_COPPER_ORE) {
            return 0xE77C56; // Copper orange like copper ingots
        } else if (block == net.minecraft.block.Blocks.REDSTONE_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_REDSTONE_ORE) {
            return 0xFF0000; // Bright red like redstone dust
        } else if (block == net.minecraft.block.Blocks.LAPIS_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_LAPIS_ORE) {
            return 0x1E90FF; // Bright blue like lapis lazuli
        } else if (block == net.minecraft.block.Blocks.COAL_ORE || 
                   block == net.minecraft.block.Blocks.DEEPSLATE_COAL_ORE) {
            return 0x2F2F2F; // Dark charcoal like coal items
        } else if (block == net.minecraft.block.Blocks.NETHER_QUARTZ_ORE) {
            return 0xF0F0F0; // White like quartz
        } else if (block == net.minecraft.block.Blocks.ANCIENT_DEBRIS) {
            return 0x654740; // Dark brown like ancient debris texture
        } else if (block.getName().getString().toLowerCase().contains("ore")) {
            // Generic ore detection - try to determine color from ore name
            String oreName = block.getName().getString().toLowerCase();
            if (oreName.contains("tin")) {
                return 0xC0C0C0; // Silver for tin
            } else if (oreName.contains("silver")) {
                return 0xC0C0C0; // Silver color
            } else if (oreName.contains("lead")) {
                return 0x5A5A5A; // Dark gray for lead
            } else if (oreName.contains("zinc")) {
                return 0xB8B8B8; // Light gray for zinc
            } else if (oreName.contains("nickel")) {
                return 0x8F8F8F; // Medium gray for nickel
            } else if (oreName.contains("aluminum") || oreName.contains("aluminium")) {
                return 0xE0E0E0; // Light gray for aluminum
            } else if (oreName.contains("uranium")) {
                return 0x32CD32; // Lime green for uranium (radioactive)
            } else if (oreName.contains("ruby")) {
                return 0xFF1493; // Deep pink for ruby
            } else if (oreName.contains("sapphire")) {
                return 0x0F52BA; // Deep blue for sapphire
            } else if (oreName.contains("topaz")) {
                return 0xFFD700; // Golden yellow for topaz
            } else if (oreName.contains("amethyst")) {
                return 0x9966CC; // Purple for amethyst
            } else {
                return 0xA0A0A0; // Default ore color - medium gray
            }
        } else {
            return -1; // Not an ore, continue to other methods
        }
    }
    
    private int getBlockColorFallback(Block block) {
        // Map common non-ore blocks to appropriate colors
        if (block.getName().getString().toLowerCase().contains("wood") ||
           block.getName().getString().toLowerCase().contains("log") ||
           block.getName().getString().toLowerCase().contains("plank")) {
            return 0x8B4513; // Brown for wood
        } else if (block.getName().getString().toLowerCase().contains("stone")) {
            return 0x808080; // Gray for stone
        } else if (block.getName().getString().toLowerCase().contains("grass")) {
            return 0x228B22; // Green for grass
        } else if (block.getName().getString().toLowerCase().contains("dirt")) {
            return 0x8B4513; // Brown for dirt
        } else if (block.getName().getString().toLowerCase().contains("water")) {
            return 0x4169E1; // Blue for water
        } else if (block.getName().getString().toLowerCase().contains("lava")) {
            return 0xFF4500; // Orange-red for lava
        } else if (block.getName().getString().toLowerCase().contains("sand")) {
            return 0xF4E4BC; // Sandy beige for sand blocks
        } else if (block.getName().getString().toLowerCase().contains("clay")) {
            return 0xA0522D; // Clay brown for clay blocks  
        } else if (block.getName().getString().toLowerCase().contains("wool")) {
            return 0xFFFFFF; // White for wool (base color)
        } else if (block.getName().getString().toLowerCase().contains("glass")) {
            return 0xE6F3FF; // Light blue tint for glass
        } else {
            // Default fallback color - light gray
            return 0xC0C0C0;
        }
    }
    
    // Legacy methods for backward compatibility with config screen
    public boolean isOutlinesEnabled() {
        return enabled;
    }
    
    public void setOutlinesEnabled(boolean enabled) {
        setEnabled(enabled);
    }
}