package com.blockoutlines.client.gui;

import com.blockoutlines.BlockOutlinesClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final BlockOutlinesClient modClient;
    private int red, green, blue;
    private ColorSlider redSlider, greenSlider, blueSlider;
    
    // Predefined colors
    private static final int[] PRESET_COLORS = {
        0xFF0000, // Red (255,0,0)
        0x00FF00, // Green (0,255,0)
        0x0000FF, // Blue (0,0,255)
        0x00FFFF, // Cyan (0,255,255)
        0xFF00FF, // Magenta (255,0,255)
        0xFFFF00, // Yellow (255,255,0)
        0xFFFFFF, // White (255,255,255)
        0x000000, // Black (0,0,0)
        -1        // Auto (special marker)
    };
    
    private static final String[] PRESET_NAMES = {
        "Red", "Green", "Blue", "Cyan", 
        "Magenta", "Yellow", "White", "Black", "Auto"
    };

    public ColorPickerScreen(Screen parent, BlockOutlinesClient modClient) {
        super(Text.literal("Color Picker"));
        this.parent = parent;
        this.modClient = modClient;
        
        // Get current color
        int currentColor = modClient.getOutlineColor();
        this.red = (currentColor >> 16) & 0xFF;
        this.green = (currentColor >> 8) & 0xFF;
        this.blue = currentColor & 0xFF;
    }

    @Override
    protected void init() {
        super.init();
        
        int startY = this.height / 2 - 70; // Moved down to give more space for color preview
        
        // RGB Sliders
        this.redSlider = new ColorSlider(
            this.width / 2 - 100, startY, 200, 20,
            Text.literal("Red: " + red), red / 255.0
        ) {
            @Override
            protected void updateMessage() {
                ColorPickerScreen.this.red = (int) (this.value * 255);
                this.setMessage(Text.literal("Red: " + ColorPickerScreen.this.red));
                updateColor();
            }

            @Override
            protected void applyValue() {
                ColorPickerScreen.this.red = (int) (this.value * 255);
                updateColor();
            }
        };
        this.addDrawableChild(redSlider);
        
        this.greenSlider = new ColorSlider(
            this.width / 2 - 100, startY + 25, 200, 20,
            Text.literal("Green: " + green), green / 255.0
        ) {
            @Override
            protected void updateMessage() {
                ColorPickerScreen.this.green = (int) (this.value * 255);
                this.setMessage(Text.literal("Green: " + ColorPickerScreen.this.green));
                updateColor();
            }

            @Override
            protected void applyValue() {
                ColorPickerScreen.this.green = (int) (this.value * 255);
                updateColor();
            }
        };
        this.addDrawableChild(greenSlider);
        
        this.blueSlider = new ColorSlider(
            this.width / 2 - 100, startY + 50, 200, 20,
            Text.literal("Blue: " + blue), blue / 255.0
        ) {
            @Override
            protected void updateMessage() {
                ColorPickerScreen.this.blue = (int) (this.value * 255);
                this.setMessage(Text.literal("Blue: " + ColorPickerScreen.this.blue));
                updateColor();
            }

            @Override
            protected void applyValue() {
                ColorPickerScreen.this.blue = (int) (this.value * 255);
                updateColor();
            }
        };
        this.addDrawableChild(blueSlider);
        
        // Preset color buttons (3-3-3 layout: 3 rows of 3) - after sliders
        int buttonWidth = 60;
        int buttonHeight = 20;
        int presetStartY = startY + 85;
        
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int x, y;
            
            // 3x3 layout
            int col = i % 3;
            int row = i / 3;
            int startX = this.width / 2 - (3 * buttonWidth + 2 * 5) / 2; // 3 buttons with 5px spacing
            x = startX + col * (buttonWidth + 5);
            y = presetStartY + row * (buttonHeight + 5);
            
            int color = PRESET_COLORS[i];
            String name = PRESET_NAMES[i];
            
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(name),
                button -> {
                    if (color == -1) {
                        // Auto color - extract from target block
                        setAutoColor();
                    } else {
                        setPresetColor(color);
                    }
                }
            ).dimensions(x, y, buttonWidth, buttonHeight).build());
        }
        
        // Close button - positioned below presets
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            button -> this.close()
        ).dimensions(this.width / 2 - 25, presetStartY + 85, 50, 20).build());
    }
    
    private void setPresetColor(int color) {
        // Disable auto color mode when manually setting a color
        modClient.setAutoColorMode(false);
        
        this.red = (color >> 16) & 0xFF;
        this.green = (color >> 8) & 0xFF;
        this.blue = color & 0xFF;
        
        // Update sliders using our custom setValue method
        this.redSlider.setValue(red / 255.0);
        this.greenSlider.setValue(green / 255.0);
        this.blueSlider.setValue(blue / 255.0);
        
        updateColor();
    }
    
    private void setAutoColor() {
        // Extract color from the target block
        int autoColor = extractBlockColor(modClient.getTargetBlock());
        String blockName = modClient.getTargetBlock().getName().getString();
        
        // Apply the extracted color without disabling auto color mode
        this.red = (autoColor >> 16) & 0xFF;
        this.green = (autoColor >> 8) & 0xFF;
        this.blue = autoColor & 0xFF;
        
        // Update sliders using our custom setValue method
        this.redSlider.setValue(red / 255.0);
        this.greenSlider.setValue(green / 255.0);
        this.blueSlider.setValue(blue / 255.0);
        
        // Apply the color to the client
        modClient.setOutlineColor(autoColor);
        
        // Enable auto color mode AFTER setting the color to avoid conflicts
        modClient.setAutoColorMode(true);
        
        // Optional: Show feedback to player if client is available
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("Auto color enabled for " + blockName + ": #" + 
                        String.format("%06X", autoColor)), 
                    false
                );
            }
        } catch (Exception e) {
            // Ignore if messaging fails
        }
    }
    
    private int extractBlockColor(net.minecraft.block.Block targetBlock) {
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
    
    private int getOreDropColor(net.minecraft.block.Block block) {
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
    
    private int getBlockColorFallback(net.minecraft.block.Block block) {
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
    
    private void updateColor() {
        // Disable auto color mode when manually adjusting RGB sliders
        modClient.setAutoColorMode(false);
        
        // Apply color immediately for live preview
        int color = (red << 16) | (green << 8) | blue;
        modClient.setOutlineColor(color);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Color preview box - positioned at the top
        int currentColor = (red << 16) | (green << 8) | blue;
        int previewX = this.width / 2 - 25;
        int previewY = 40; // Fixed position at top, below title
        
        // Draw color preview with border
        context.fill(previewX - 1, previewY - 1, previewX + 51, previewY + 21, 0xFFFFFFFF); // White border
        context.fill(previewX, previewY, previewX + 50, previewY + 20, 0xFF000000 | currentColor); // Color fill
        
        // Color value text
        String hexColor = String.format("#%06X", currentColor);
        String colorText = hexColor;
        if (modClient.isAutoColorMode()) {
            colorText += " (Auto)";
        }
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(colorText), 
            this.width / 2, previewY + 25, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
    
    // Custom slider that allows public access to value field
    private static class ColorSlider extends SliderWidget {
        public ColorSlider(int x, int y, int width, int height, Text message, double value) {
            super(x, y, width, height, message, value);
        }
        
        public void setValue(double newValue) {
            this.value = Math.max(0.0, Math.min(1.0, newValue));
            this.updateMessage();
        }
        
        @Override
        protected void updateMessage() {
            // Override in subclasses
        }
        
        @Override
        protected void applyValue() {
            // Override in subclasses
        }
    }
}
