package com.blockoutlines.client;

import com.blockoutlines.entity.OutlineBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.FallingBlockEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class OutlineBlockEntityRenderer extends EntityRenderer<OutlineBlockEntity, FallingBlockEntityRenderState> {
    private final BlockRenderManager blockRenderManager;
    
    public OutlineBlockEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.blockRenderManager = context.getBlockRenderManager();
    }
    
    @Override
    public void render(FallingBlockEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Scale down by .01% to prevent z-fighting
        // matrices.scale(0.9999f, 0.9999f, 0.9999f);
        
        if (state.blockState.getRenderType() == BlockRenderType.MODEL) {
            matrices.translate(-0.5, 0.0, -0.5);
            this.blockRenderManager.renderBlockAsEntity(state.blockState, matrices, vertexConsumers, light, light);
        }
        
        matrices.pop();
    }
    
    @Override
    public FallingBlockEntityRenderState createRenderState() {
        return new FallingBlockEntityRenderState();
    }
    
    @Override
    public void updateRenderState(OutlineBlockEntity entity, FallingBlockEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.blockState = entity.getBlockState();
    }
    
}