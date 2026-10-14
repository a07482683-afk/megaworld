package com.megaworld;

import com.megaworld.block.AntiGravityBlock;
import com.megaworld.block.BouncePadBlock;
import com.megaworld.block.CheckpointBlock;
import com.megaworld.block.ComputerBlock;
import com.megaworld.block.MagnetBlock;
import com.megaworld.block.SpeedPadBlock;
import com.megaworld.block.VanishingBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModBlocks {
    public static final Block BOUNCE_PAD = register("bounce_pad",
            new BouncePadBlock(FabricBlockSettings.copyOf(Blocks.SLIME_BLOCK)));
    public static final Block SPEED_PAD = register("speed_pad",
            new SpeedPadBlock(FabricBlockSettings.copyOf(Blocks.STONE)));
    public static final Block CHECKPOINT = register("checkpoint",
            new CheckpointBlock(FabricBlockSettings.copyOf(Blocks.GOLD_BLOCK)));
    public static final Block COMPUTER = register("computer",
            new ComputerBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK).nonOpaque().luminance(state -> 6)));
    public static final Block ANTI_GRAVITY_BLOCK = register("anti_gravity_block",
            new AntiGravityBlock(FabricBlockSettings.copyOf(Blocks.AMETHYST_BLOCK)));
    public static final Block VANISHING_BLOCK = register("vanishing_block",
            new VanishingBlock(FabricBlockSettings.copyOf(Blocks.STONE_BRICKS)));
    public static final Block MAGNET_BLOCK = register("magnet_block",
            new MagnetBlock(FabricBlockSettings.copyOf(Blocks.IRON_BLOCK)));

    private static Block register(String name, Block block) {
        Registry.register(Registries.ITEM, MegaWorld.id(name), new BlockItem(block, new FabricItemSettings()));
        return Registry.register(Registries.BLOCK, MegaWorld.id(name), block);
    }

    public static void init() {}
}
