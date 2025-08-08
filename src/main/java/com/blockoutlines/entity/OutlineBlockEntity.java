package com.blockoutlines.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class OutlineBlockEntity extends FallingBlockEntity {
    private int lifeTime = 0;
    private static final int MAX_LIFETIME = 1200; // 60 seconds at 20 TPS
    private BlockState displayBlockState;

    public OutlineBlockEntity(EntityType<? extends FallingBlockEntity> entityType, World world) {
        super(entityType, world);
        this.dropItem = false;
        this.displayBlockState = Blocks.STONE.getDefaultState();
    }

    public OutlineBlockEntity(EntityType<? extends FallingBlockEntity> entityType, World world, BlockState blockState) {
        this(entityType, world);
        this.displayBlockState = blockState;
    }

    @Override
    public BlockState getBlockState() {
        return this.displayBlockState;
    }

    public void setDisplayBlockState(BlockState blockState) {
        this.displayBlockState = blockState;
    }

    @Override
    public void tick() {
        super.tick();
        
        this.lifeTime++;
        
        if (this.lifeTime > MAX_LIFETIME) {
            this.discard();
        }
        
        if (this.getY() < -64 || this.getY() > 320) {
            this.discard();
        }
    }

    @Override
    public boolean hasNoGravity() {
        return true;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    @Override
    public boolean isOnGround() {
        return super.isOnGround();
    }

    @Override
    public void move(net.minecraft.entity.MovementType movementType, net.minecraft.util.math.Vec3d movement) {
        // Prevent any movement - stay exactly in place
    }

    public int getLifeTime() {
        return this.lifeTime;
    }

    public BlockPos getBlockPos() {
        return BlockPos.ofFloored(this.getX(), this.getY(), this.getZ());
    }
}