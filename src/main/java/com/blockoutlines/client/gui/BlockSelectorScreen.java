package com.blockoutlines.client.gui;

import com.blockoutlines.BlockOutlinesClient;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BlockSelectorScreen extends Screen {
    private final Screen parent;
    private final BlockOutlinesClient modClient;
    private TextFieldWidget searchField;
    private List<Block> allBlocks;
    private List<Block> filteredBlocks;
    private Block selectedBlock;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 9;
    private static final int VISIBLE_ROWS = 6;
    private static final int ITEM_SIZE = 18;
    private static final int ITEM_SPACING = 2;

    public BlockSelectorScreen(Screen parent, BlockOutlinesClient modClient) {
        super(Text.literal("Select Target Block"));
        this.parent = parent;
        this.modClient = modClient;
        this.selectedBlock = modClient.getTargetBlock();
        
        // Get all blocks that have items (can be placed/obtained)
        this.allBlocks = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            Item item = block.asItem();
            if (item != null && item != net.minecraft.item.Items.AIR && item instanceof BlockItem) {
                allBlocks.add(block);
            }
        }
        this.filteredBlocks = new ArrayList<>(allBlocks);
    }

    @Override
    protected void init() {
        super.init();

        // Search field
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 40, 200, 20, Text.literal("Search blocks..."));
        this.searchField.setPlaceholder(Text.literal("Search blocks..."));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.searchField.setFocused(true);
        this.addSelectableChild(searchField);
        this.setInitialFocus(searchField);

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
            Text.literal("Cancel"),
            button -> this.close()
        ).dimensions(this.width / 2 - 50, this.height - 25, 100, 20).build());
    }

    private void onSearchChanged(String search) {
        String searchLower = search.toLowerCase();
        this.filteredBlocks = allBlocks.stream()
                .filter(block -> {
                    Identifier id = Registries.BLOCK.getId(block);
                    return id.toString().toLowerCase().contains(searchLower) ||
                           block.getName().getString().toLowerCase().contains(searchLower);
                })
                .collect(Collectors.toList());
        this.scrollOffset = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        // Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        // Search field
        this.searchField.render(context, mouseX, mouseY, delta);

        // Block grid
        renderBlockGrid(context, mouseX, mouseY);

        // Selected block info
        if (selectedBlock != null) {
            String blockName = selectedBlock.getName().getString();
            context.drawCenteredTextWithShadow(this.textRenderer, 
                Text.literal("Selected: " + blockName), 
                this.width / 2, this.height - 75, 0xFFFFFF);
        }
    }

    private void renderBlockGrid(DrawContext context, int mouseX, int mouseY) {
        int startX = this.width / 2 - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING)) / 2;
        int startY = 80;
        
        int totalItems = Math.min(filteredBlocks.size() - scrollOffset, ITEMS_PER_ROW * VISIBLE_ROWS);
        
        for (int i = 0; i < totalItems; i++) {
            int blockIndex = scrollOffset + i;
            if (blockIndex >= filteredBlocks.size()) break;
            
            Block block = filteredBlocks.get(blockIndex);
            ItemStack stack = new ItemStack(block.asItem());
            
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
            int y = startY + row * (ITEM_SIZE + ITEM_SPACING);
            
            // Highlight selected block with white border
            if (block == selectedBlock) {
                context.drawBorder(x - 1, y - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 0xFFFFFFFF);
            }
            
            // Highlight hovered block
            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                context.fill(x - 1, y - 1, x + ITEM_SIZE + 1, y + ITEM_SIZE + 1, 0x80FFFFFF);
            }
            
            // Render item
            context.drawItem(stack, x, y);
            
            // Render tooltip on hover
            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(block.getName());
                tooltip.add(Text.literal(Registries.BLOCK.getId(block).toString()).styled(style -> style.withColor(0x888888)));
                context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check if clicking on a block in the grid
        int startX = this.width / 2 - (ITEMS_PER_ROW * (ITEM_SIZE + ITEM_SPACING)) / 2;
        int startY = 80;
        
        int totalItems = Math.min(filteredBlocks.size() - scrollOffset, ITEMS_PER_ROW * VISIBLE_ROWS);
        
        for (int i = 0; i < totalItems; i++) {
            int blockIndex = scrollOffset + i;
            if (blockIndex >= filteredBlocks.size()) break;
            
            int row = i / ITEMS_PER_ROW;
            int col = i % ITEMS_PER_ROW;
            int x = startX + col * (ITEM_SIZE + ITEM_SPACING);
            int y = startY + row * (ITEM_SIZE + ITEM_SPACING);
            
            if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                this.selectedBlock = filteredBlocks.get(blockIndex);
                modClient.setTargetBlock(selectedBlock);
                this.close();
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, filteredBlocks.size() - (ITEMS_PER_ROW * VISIBLE_ROWS));
        
        if (verticalAmount > 0 && scrollOffset > 0) {
            scrollOffset = Math.max(0, scrollOffset - ITEMS_PER_ROW);
        } else if (verticalAmount < 0 && scrollOffset < maxScroll) {
            scrollOffset = Math.min(maxScroll, scrollOffset + ITEMS_PER_ROW);
        }
        
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.searchField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
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