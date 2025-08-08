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
        0xFF0000, // Red
        0x00FF00, // Green  
        0x0000FF, // Blue
        0x00FFFF, // Cyan
        0xFF00FF, // Magenta
        0xFFFF00, // Yellow
        0xFFA500, // Orange
        0x800080, // Purple
        0xFFFFFF, // White
        0x000000  // Black
    };
    
    private static final String[] PRESET_NAMES = {
        "Red", "Green", "Blue", "Cyan", 
        "Magenta", "Yellow", "Orange", "Purple",
        "White", "Black"
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
        
        // Preset color buttons (5 columns, 2 rows for 10 colors) - after sliders
        int buttonWidth = 60;
        int buttonHeight = 20;
        int startX = this.width / 2 - (5 * buttonWidth + 4 * 5) / 2; // 5 buttons with 5px spacing
        int presetStartY = startY + 85;
        
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            int col = i % 5;
            int row = i / 5;
            int x = startX + col * (buttonWidth + 5);
            int y = presetStartY + row * (buttonHeight + 5);
            int color = PRESET_COLORS[i];
            String name = PRESET_NAMES[i];
            
            this.addDrawableChild(ButtonWidget.builder(
                Text.literal(name),
                button -> setPresetColor(color)
            ).dimensions(x, y, buttonWidth, buttonHeight).build());
        }
        
        // Close button - positioned below presets
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            button -> this.close()
        ).dimensions(this.width / 2 - 25, presetStartY + 60, 50, 20).build());
    }
    
    private void setPresetColor(int color) {
        this.red = (color >> 16) & 0xFF;
        this.green = (color >> 8) & 0xFF;
        this.blue = color & 0xFF;
        
        // Update sliders using our custom setValue method
        this.redSlider.setValue(red / 255.0);
        this.greenSlider.setValue(green / 255.0);
        this.blueSlider.setValue(blue / 255.0);
        
        updateColor();
    }
    
    private void updateColor() {
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
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(hexColor), 
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
