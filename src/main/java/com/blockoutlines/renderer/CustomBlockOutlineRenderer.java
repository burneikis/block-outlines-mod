package com.blockoutlines.renderer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public class CustomBlockOutlineRenderer {
    
    public static void renderGlowingBlockOutlines(
        MatrixStack matrices,
        Camera camera,
        OutlineVertexConsumerProvider outlineProvider,
        World world,
        Set<BlockPos> blockPositions,
        Block targetBlock
    ) {
        if (blockPositions.isEmpty()) {
            return;
        }
        
        // Set bright cyan outline color
        outlineProvider.setColor(0, 255, 255, 255); // RGBA: Cyan with full opacity
        
        // Get camera position for relative positioning
        Vec3d cameraPos = camera.getPos();
        
        // Get block render manager
        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
        
        // Render each block using the outline system
        for (BlockPos pos : blockPositions) {
            BlockState state = world.getBlockState(pos);
            if (!state.isAir() && state.isOf(targetBlock)) {
                
                matrices.push();
                
                // Translate to block position relative to camera
                matrices.translate(
                    pos.getX() - cameraPos.x,
                    pos.getY() - cameraPos.y,
                    pos.getZ() - cameraPos.z
                );
                
                // Render the block with invisible faces but preserve outline capability
                renderInvisibleBlock(state, pos, matrices, outlineProvider, blockRenderManager, world);
                
                matrices.pop();
            }
        }
        
        // Draw all buffered outline vertices - this triggers the real glow post-processing
        outlineProvider.draw();
    }
    
    // Render invisible block method adapted from block-outlines-mod approach
    private static void renderInvisibleBlock(
        BlockState blockState, 
        BlockPos blockPos, 
        MatrixStack matrixStack, 
        OutlineVertexConsumerProvider outlineProvider,
        BlockRenderManager blockRenderManager,
        World world
    ) {
        if (blockState.getRenderType() != BlockRenderType.MODEL) {
            return;
        }

        // Generate the block model parts using the block's render seed
        List<BlockModelPart> modelParts = blockRenderManager
                .getModel(blockState)
                .getParts(Random.create(blockState.getRenderingSeed(blockPos)));

        // Use EntityTranslucent render layer - supports outlines and alpha blending
        RenderLayer renderLayer = RenderLayer.getEntityTranslucent(
            Identifier.of("minecraft", "textures/atlas/blocks.png"), 
            true
        );
        VertexConsumer vertexConsumer = outlineProvider.getBuffer(renderLayer);

        // Wrap with InvisibleVertexConsumer to make faces transparent
        InvisibleVertexConsumer invisibleConsumer = new InvisibleVertexConsumer(vertexConsumer);

        // Render using model renderer directly
        blockRenderManager.getModelRenderer().render(
            world,
            modelParts,
            blockState,
            blockPos,
            matrixStack,
            invisibleConsumer,
            false,
            OverlayTexture.DEFAULT_UV
        );
    }
    
    // InvisibleVertexConsumer that makes block faces transparent
    private static class InvisibleVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        public InvisibleVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return this.delegate.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            // Set alpha to 0 to make it completely transparent
            return this.delegate.color(red, green, blue, 0);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return this.delegate.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return this.delegate.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return this.delegate.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this.delegate.normal(x, y, z);
        }
    }
}
