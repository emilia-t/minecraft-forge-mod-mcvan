package com.myacghome.mcvan.item;

import com.myacghome.mcvan.IndexMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.myacghome.mcvan.block.ModBlocks;
import com.myacghome.mcvan.block.VideoScreenBlock;
import com.myacghome.mcvan.block.VideoScreenBlock1609;
/**CLASS
 describe: 全部物品的主注册类
 notepad: 无
 **/
public class ModItems {
    public static DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, IndexMod.MOD_ID);// 创建一个延迟寄存器来保存所有将在“mcvan”命名空间下注册的物品

    public static final
    RegistryObject<Item> BIG_BOW = ITEMS.register(
            "big_bow",
            ()->new BigBowItem(new Item.Properties().durability(384))
    );//放大弓

    public static final
    RegistryObject<Item> BIG_ARROW = ITEMS.register(
            "big_arrow",
            ()->new BigArrowItem(new Item.Properties())
    );//放大箭

    public static final RegistryObject<Item> VIDEO_SCREEN_BLOCK = ITEMS.register(
            "video_screen",
            () -> new VideoScreenBlockItem(
                    (VideoScreenBlock) ModBlocks.VIDEO_SCREEN_BLOCK.get(),
                    new Item.Properties()
            )
    );//视频屏幕方块的物品注册

    public static final RegistryObject<Item> VIDEO_SCREEN_BLOCK_1609 = ITEMS.register(
            "video_screen_1609",
            () -> new VideoScreenBlock1609Item(
                    (VideoScreenBlock1609) ModBlocks.VIDEO_SCREEN_BLOCK_1609.get(),
                    new Item.Properties()
            )
    );//视频屏幕方块的物品注册

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
