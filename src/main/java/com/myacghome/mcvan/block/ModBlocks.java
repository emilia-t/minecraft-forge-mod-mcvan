package com.myacghome.mcvan.block;

import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块注册地
 */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "mcvan");

    public static final RegistryObject<Block> VIDEO_SCREEN_BLOCK =
            BLOCKS.register(
                    "video_screen",
                    () -> new VideoScreenBlock(
                                BlockBehaviour
                                .Properties.of()
                                .strength(3.0f)
                                .noOcclusion()
                                .requiresCorrectToolForDrops()
                    )
            );
    public static final RegistryObject<Block> VIDEO_SCREEN_BLOCK_1609 =
            BLOCKS.register(
                    "video_screen_1609",
                    () -> new VideoScreenBlock1609(
                                    BlockBehaviour
                                    .Properties.of()
                                    .strength(9.0f)
                                    .noOcclusion()
                                    .requiresCorrectToolForDrops()
                    )
            );
}
