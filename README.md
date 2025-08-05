# Block Outlines Mod

A Minecraft Fabric mod that automatically detects diamond blocks in the world and renders floating outline blocks above them. The outline blocks are client-side only and invisible to other players.

## Features

- **Automatic Diamond Ore Detection**: Scans a 16-block radius around the player for diamond ore, deepslate diamond ore, and diamond blocks
- **Floating Outline Blocks**: Displays floating diamond ore blocks with glowing outlines at the exact coordinates of detected diamond ore
- **Glowing Outlines**: All outline blocks have a glowing effect for better visibility
- **Client-Side Only**: Completely client-side - no server modifications needed
- **Toggle Functionality**: Press `O` to enable/disable the outline system
- **Automatic Cleanup**: Outline blocks are automatically removed when diamond blocks are broken

## Usage

1. Install the mod in your Fabric mods folder
2. Launch Minecraft and join any world
3. Press `O` to toggle the block outline system on/off
4. Place diamond blocks in the world to see floating outline blocks appear above them
5. Break diamond blocks to automatically remove their outline blocks

## How It Works

The mod continuously scans for diamond blocks within a 32-block radius of the player. When diamond blocks are found, it spawns client-side floating entities above them that render as various ore blocks with glowing outlines. These entities:

- Have no gravity (float in place)
- Are invisible to the server and other players
- Automatically despawn after 60 seconds
- Are arranged in a circular pattern above each diamond block

## Technical Details

- **Entity System**: Uses custom `OutlineBlockEntity` based on `FallingBlockEntity`
- **Client-Side Rendering**: Leverages Minecraft's built-in `FallingBlockEntityRenderer`
- **World Scanning**: Periodically scans for diamond blocks and updates outline display
- **Memory Management**: Automatic cleanup prevents memory leaks

## Controls

- `O` - Toggle block outlines on/off

## Requirements

- Minecraft 1.21.5
- Fabric Loader 0.16.14+
- Fabric API

## Based On

This mod is built using code from the fake-block-test-template-1.21.5, extending its client-side entity rendering capabilities to create an automatic diamond block detection and outline system.