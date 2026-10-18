package com.megaworld.block;

import com.megaworld.computer.ComputerHooks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/** Компьютер: ПКМ открывает рабочий стол в стиле Windows 11. */
public class ComputerBlock extends HorizontalFacingBlock {
    private static final VoxelShape NORTH = VoxelShapes.cuboid(1 / 16.0, 0, 5 / 16.0, 15 / 16.0, 15 / 16.0, 10 / 16.0);
    private static final VoxelShape SOUTH = VoxelShapes.cuboid(1 / 16.0, 0, 6 / 16.0, 15 / 16.0, 15 / 16.0, 11 / 16.0);
    private static final VoxelShape EAST = VoxelShapes.cuboid(6 / 16.0, 0, 1 / 16.0, 11 / 16.0, 15 / 16.0, 15 / 16.0);
    private static final VoxelShape WEST = VoxelShapes.cuboid(5 / 16.0, 0, 1 / 16.0, 10 / 16.0, 15 / 16.0, 15 / 16.0);

    public ComputerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            ComputerHooks.openComputer.run();
        }
        return ActionResult.success(world.isClient);
    }
}
