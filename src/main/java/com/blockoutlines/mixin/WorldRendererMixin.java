package com.blockoutlines.mixin;

import com.blockoutlines.BlockOutlinesClient;
import com.blockoutlines.renderer.CustomBlockOutlineRenderer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    
    @Shadow
    private BufferBuilderStorage bufferBuilders;
    
    @Shadow
    private ClientWorld world;
    
    // Inject after block entities are rendered to add our custom outlines
    @Inject(
        method = "renderBlockEntities", 
        at = @At("TAIL")
    )
    private void renderCustomBlockOutlines(
        MatrixStack matrices,
        VertexConsumerProvider.Immediate entityVertexConsumers,
        VertexConsumerProvider.Immediate effectVertexConsumers,
        Camera camera,
        float tickProgress,
        CallbackInfo ci
    ) {
        // Only render if the mod is enabled and we have block positions to render
        BlockOutlinesClient client = BlockOutlinesClient.getInstance();
        if (client != null && client.isEnabled() && !client.getTrackedBlockPositions().isEmpty()) {
            
            // Get the outline vertex consumer provider from the buffer builders
            OutlineVertexConsumerProvider outlineProvider = this.bufferBuilders.getOutlineVertexConsumers();
            
            // Render our custom block outlines
            CustomBlockOutlineRenderer.renderGlowingBlockOutlines(
                matrices,
                camera,
                outlineProvider,
                this.world,
                client.getTrackedBlockPositions(),
                client.getTargetBlock(),
                client.getOutlineColor()
            );
        }
    }
    
    // Force getEntitiesToRender to return true so outline pass runs
    @Inject(
        method = "getEntitiesToRender", 
        at = @At("RETURN"), 
        cancellable = true
    )
    private void forceEntityOutlineReturn(
        Camera camera, 
        Frustum frustum, 
        List<Entity> output,
        CallbackInfoReturnable<Boolean> cir
    ) {
        // Force return true when our mod is active to ensure outline pass runs
        BlockOutlinesClient client = BlockOutlinesClient.getInstance();
        if (client != null && client.isEnabled() && !client.getTrackedBlockPositions().isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
