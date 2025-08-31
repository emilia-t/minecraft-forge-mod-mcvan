package com.myacghome.mcvan.block;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;

/**
 * 实体方块注册地
 */
@Mod.EventBusSubscriber(modid = "mcvan", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "mcvan");

    public static final RegistryObject<BlockEntityType<VideoScreenBlockEntity>> VIDEO_SCREEN =
            BLOCK_ENTITIES.register(
                    "video_screen",
                    () -> BlockEntityType.Builder.of(
                            VideoScreenBlockEntity::new, ModBlocks.VIDEO_SCREEN_BLOCK.get()
                            )
                            .build(null));

    public static final RegistryObject<BlockEntityType<VideoScreenBlockEntity1609>> VIDEO_SCREEN_1609 =
            BLOCK_ENTITIES.register(
                    "video_screen_1609",
                    () -> BlockEntityType.Builder.of(
                                    VideoScreenBlockEntity1609::new, ModBlocks.VIDEO_SCREEN_BLOCK_1609.get()
                            )
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
