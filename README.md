# Block Outlines Mod

A Minecraft Fabric mod that automatically detects target blocks in the world and renders glowing outlines around them using the same outlines from spectral arrows (this was the main focus of this project). The outlines are client-side only and invisible to other players.

## Features

- **Automatic Block Detection**: Scans a configurable radius around the player for target blocks (default: diamond ore)
- **Direct Block Outlines**: Renders glowing outlines directly on detected blocks without spawning entities
- **Configurable Target Block**: Choose any block type to highlight through the config screen
- **Auto Color Extraction**: Automatically extracts colors from target blocks using map colors and fallbacks
- **Smart Auto Color Mode**: Colors automatically update when target block changes (enable by clicking "Auto" in color picker)
- **Custom Color Picker**: Full RGB color picker with preset colors and auto color functionality
- **Client-Side Only**: Completely client-side - no server modifications needed
- **Toggle Functionality**: Press `O` to enable/disable the outline system
- **Configuration Screen**: Press `Right Shift` to open block selection and settings

## Usage

1. Install the mod in your Fabric mods folder
2. Launch Minecraft and join any world
3. Press `O` to toggle the block outline system on/off
4. Press `Right Shift` to open configuration and select target blocks and colors
5. Target blocks within range will automatically show glowing outlines

## How It Works

The mod uses the same outlines from spectral arrows by directly hooking into Minecraft's `WorldRenderer`:

### Steps:

1. **Block Scanning**: The mod continuously scans for target blocks around the player using a configurable radius:

```java
private void scanForTargetBlocks(MinecraftClient client) {
    ClientWorld world = client.world;
    BlockPos playerPos = client.player.getBlockPos();
    Set<BlockPos> currentPositions = new HashSet<>();
    
    // Scan in a cube around the player
    for (int x = -scanRadius; x <= scanRadius; x++) {
        for (int y = -scanRadius; y <= scanRadius; y++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
                BlockPos pos = playerPos.add(x, y, z);
                BlockState blockState = world.getBlockState(pos);
                if (blockState.isOf(targetBlock)) {
                    currentPositions.add(pos.toImmutable());
                }
            }
        }
    }
}
```

2. **WorldRenderer Mixin**: The mod injects into Minecraft's `WorldRenderer` to hook the rendering pipeline:

```java
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderBlockEntities", at = @At("TAIL"))
    private void renderCustomBlockOutlines(MatrixStack matrices,
                                         VertexConsumerProvider.Immediate entityVertexConsumers,
                                         VertexConsumerProvider.Immediate effectVertexConsumers,
                                         Camera camera, float tickProgress, CallbackInfo ci) {
        BlockOutlinesClient client = BlockOutlinesClient.getInstance();
        if (client != null && client.isEnabled() && !client.getTrackedBlockPositions().isEmpty()) {
            OutlineVertexConsumerProvider outlineProvider = this.bufferBuilders.getOutlineVertexConsumers();
            
            CustomBlockOutlineRenderer.renderGlowingBlockOutlines(
                matrices, camera, outlineProvider, this.world,
                client.getTrackedBlockPositions(), client.getTargetBlock(), client.getOutlineColor()
            );
        }
    }
}
```

3. **Outline Rendering**: The mod renders invisible block models with outline capability using `OutlineVertexConsumerProvider`:

```java
public static void renderGlowingBlockOutlines(MatrixStack matrices, Camera camera,
                                            OutlineVertexConsumerProvider outlineProvider,
                                            World world, Set<BlockPos> blockPositions,
                                            Block targetBlock, int color) {
    // Set the outline color
    int red = (color >> 16) & 0xFF;
    int green = (color >> 8) & 0xFF;  
    int blue = color & 0xFF;
    outlineProvider.setColor(red, green, blue, 255);
    
    // Render each block with invisible faces but visible outlines
    for (BlockPos pos : blockPositions) {
        matrices.push();
        matrices.translate(pos.getX() - camera.getPos().x,
                          pos.getY() - camera.getPos().y, 
                          pos.getZ() - camera.getPos().z);
        
        renderInvisibleBlock(world.getBlockState(pos), pos, matrices, 
                           outlineProvider, blockRenderManager, world);
        matrices.pop();
    }
    
    outlineProvider.draw(); // Triggers the glow post-processing
}
```

4. **Invisible Block Rendering**: Block faces are made transparent while preserving outline data:

```java
private static class InvisibleVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    
    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        // Set alpha to 0 to make block faces completely transparent
        return this.delegate.color(red, green, blue, 0);
    }
    
    // Other methods delegate normally to preserve outline geometry
}
```

This approach eliminates the overhead of spawning fake entities (what I was doing before) and provides better visual quality compared to a traditional wireframe overlay.

## Controls

- `O` - Toggle block outlines on/off
- `Right Shift` - Open configuration screen for block selection and settings

## History

This mod was made starting with me using hasOutline on normal living entities, then item entities, then trying to get it to work with block entities.

[Block Entity Outlines](https://github.com/burneikis/Block-Entity-Outlines)

I couldn't get all block entities to work initially, and assumed normal blocks would be much harder, so I thought of rendering fake falling sand entities with the outline, and built a mod for that.

[Fake Block Test](https://github.com/burneikis/fake-block-test-template-1.21.5)

Its kinda cool that you can render client side fake entities, worth going back in the commits and playing with the falling sand version, you can even move the fake blocks with pistons, which is kinda cooked.

Then I started this repo, using fake falling block entities - which I knew I could add an outline to - to create outlines for any block, and it works, but it seems a little dumb to spawn fake entities for this, so I started trying to use the code I made to just directly render outline blocks instead of making an entire entity for each block. The mod I first did that in was:

[Diamond Ore Outlines](https://github.com/burneikis/diamond-ore-outlines)

So that worked, and I added the code from that back into this mod, and updated the gui with color picker and here we are.

## ToDo

- [ ] Add support for multiple different blocks at once
- [ ] Improve performance