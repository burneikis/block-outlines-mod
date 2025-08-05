package com.blockoutlines.client.gui;

import com.blockoutlines.BlockOutlinesClient;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

public class BlockOutlinesConfigScreen extends Screen {
    private final Screen parent;
    private final BlockOutlinesClient modClient;
    private boolean outlinesEnabled;
    private int scanRadius;
    private int scanRate;
    private ButtonWidget toggleButton;
    private SliderWidget radiusSlider;
    private SliderWidget scanRateSlider;

    public BlockOutlinesConfigScreen(Screen parent, BlockOutlinesClient modClient) {
        super(Text.literal("Block Outlines Configuration"));
        this.parent = parent;
        this.modClient = modClient;
        this.outlinesEnabled = modClient.isOutlinesEnabled();
        this.scanRadius = modClient.getScanRadius();
        this.scanRate = modClient.getScanRate();
    }

    @Override
    protected void init() {
        super.init();

        // Toggle button
        this.toggleButton = ButtonWidget.builder(
            Text.literal("Outlines: " + (outlinesEnabled ? "ON" : "OFF")),
            button -> {
                outlinesEnabled = !outlinesEnabled;
                button.setMessage(Text.literal("Outlines: " + (outlinesEnabled ? "ON" : "OFF")));
                // Apply immediately
                modClient.setOutlinesEnabled(outlinesEnabled);
            }
        ).dimensions(this.width / 2 - 100, this.height / 2 - 90, 200, 20).build();
        this.addDrawableChild(toggleButton);

        // Target block selector button
        Block currentTarget = modClient.getTargetBlock();
        String blockName = currentTarget.getName().getString();
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Target: " + blockName),
            button -> MinecraftClient.getInstance().setScreen(new BlockSelectorScreen(this, modClient))
        ).dimensions(this.width / 2 - 100, this.height / 2 - 60, 200, 20).build());

        // Scan radius slider
        this.radiusSlider = new SliderWidget(
            this.width / 2 - 100, this.height / 2 - 30, 200, 20,
            Text.literal("Scan Radius: " + scanRadius), 
            (scanRadius - 16) / 48.0
        ) {
            @Override
            protected void updateMessage() {
                int oldRadius = BlockOutlinesConfigScreen.this.scanRadius;
                BlockOutlinesConfigScreen.this.scanRadius = (int) (this.value * 48) + 16;
                this.setMessage(Text.literal("Scan Radius: " + BlockOutlinesConfigScreen.this.scanRadius));
                
                // Apply immediately if the value actually changed
                if (oldRadius != BlockOutlinesConfigScreen.this.scanRadius) {
                    modClient.setScanRadius(BlockOutlinesConfigScreen.this.scanRadius);
                }
            }

            @Override
            protected void applyValue() {
                int oldRadius = BlockOutlinesConfigScreen.this.scanRadius;
                BlockOutlinesConfigScreen.this.scanRadius = (int) (this.value * 48) + 16;
                
                // Apply immediately if the value actually changed
                if (oldRadius != BlockOutlinesConfigScreen.this.scanRadius) {
                    modClient.setScanRadius(BlockOutlinesConfigScreen.this.scanRadius);
                }
            }
        };
        this.addDrawableChild(radiusSlider);

        // Scan rate slider
        this.scanRateSlider = new SliderWidget(
            this.width / 2 - 100, this.height / 2, 200, 20,
            Text.literal("Scan Rate: " + scanRate + " ticks"), 
            (scanRate - 1) / 19.0
        ) {
            @Override
            protected void updateMessage() {
                int oldRate = BlockOutlinesConfigScreen.this.scanRate;
                BlockOutlinesConfigScreen.this.scanRate = (int) (this.value * 19) + 1;
                this.setMessage(Text.literal("Scan Rate: " + BlockOutlinesConfigScreen.this.scanRate + " ticks"));
                
                // Apply immediately if the value actually changed
                if (oldRate != BlockOutlinesConfigScreen.this.scanRate) {
                    modClient.setScanRate(BlockOutlinesConfigScreen.this.scanRate);
                }
            }

            @Override
            protected void applyValue() {
                int oldRate = BlockOutlinesConfigScreen.this.scanRate;
                BlockOutlinesConfigScreen.this.scanRate = (int) (this.value * 19) + 1;
                
                // Apply immediately if the value actually changed
                if (oldRate != BlockOutlinesConfigScreen.this.scanRate) {
                    modClient.setScanRate(BlockOutlinesConfigScreen.this.scanRate);
                }
            }
        };
        this.addDrawableChild(scanRateSlider);

        // Close button (settings are applied immediately)
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Close"),
            button -> this.close()
        ).dimensions(this.width / 2 - 50, this.height / 2 + 40, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}