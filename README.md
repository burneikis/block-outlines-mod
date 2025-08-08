# Block Outlines Mod

A Minecraft Fabric mod that automatically detects target blocks in the world and renders glowing outlines around them using advanced mixin-based rendering. The outlines are client-side only and invisible to other players.

## Features

- **Automatic Block Detection**: Scans a configurable radius around the player for target blocks (default: diamond ore)
- **Direct Block Outlines**: Renders glowing outlines directly on detected blocks without spawning entities
- **Configurable Target Block**: Choose any block type to highlight through the config screen
- **Advanced Rendering**: Uses mixin-based rendering for invisible faces with glowing outlines
- **Client-Side Only**: Completely client-side - no server modifications needed
- **Toggle Functionality**: Press `O` to enable/disable the outline system
- **Configuration Screen**: Press `Right Shift` to open block selection and settings

## Usage

1. Install the mod in your Fabric mods folder
2. Launch Minecraft and join any world
3. Press `O` to toggle the block outline system on/off
4. Press `Right Shift` to open configuration and select target blocks
5. Target blocks within range will automatically show glowing outlines

## How It Works

The mod uses advanced **mixin-based rendering** that directly hooks into Minecraft's `WorldRenderer`:

1. **World Scanning**: Continuously scans for target blocks within a configurable radius
2. **Mixin Injection**: Injects custom rendering code into the block entity rendering phase
3. **Invisible Rendering**: Renders block geometry with transparent faces while preserving outline capability
4. **Outline Processing**: Uses Minecraft's built-in outline system for perfect glowing effects

This approach eliminates the overhead of spawning fake entities and provides better performance and visual quality.

## Technical Details

- **Mixin-Based Rendering**: Direct injection into `WorldRenderer` for optimal performance  
- **Advanced Transparency**: Uses `InvisibleVertexConsumer` to make faces transparent while preserving geometry
- **Outline Compatibility**: Leverages `OutlineVertexConsumerProvider` for built-in glow effects
- **Memory Efficient**: No entity spawning - uses direct rendering approach
- **Configurable Scanning**: Adjustable radius and refresh rates

## Controls

- `O` - Toggle block outlines on/off
- `Right Shift` - Open configuration screen for block selection and settings

## Requirements

- Minecraft 1.21.5+
- Fabric Loader 0.16.14+
- Fabric API

## Implementation

This mod implements the advanced rendering approach from diamond-ore-outlines, using:
- **WorldRenderer Mixin**: Direct rendering injection
- **Custom Block Outline Renderer**: Invisible face rendering with outline preservation  
- **No Entity Spawning**: Pure mixin-based approach for better performance