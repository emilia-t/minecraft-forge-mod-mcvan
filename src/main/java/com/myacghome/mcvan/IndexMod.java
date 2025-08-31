package com.myacghome.mcvan;

import net.minecraft.core.BlockPos;
import com.mojang.logging.LogUtils;
import com.myacghome.mcvan.block.ModBlockEntities;
import com.myacghome.mcvan.block.ModBlocks;
import com.myacghome.mcvan.block.VideoBlockFrameIndexRenderPacket;
import com.myacghome.mcvan.block.VideoControlMenu;
import com.myacghome.mcvan.entity.BigArrowEntity;
import com.myacghome.mcvan.entity.BossEntity;
import com.myacghome.mcvan.entity.ModEntities;
import com.myacghome.mcvan.item.ModItems;
import com.myacghome.mcvan.network.NetworkHandler;
import com.myacghome.mcvan.util.VideoData;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.Level;
/**CLASS
 * describe: 模组根类
 * notepad: 主要用来注册各种东西
 */
@Mod("mcvan")
public class IndexMod{
    public static final
    StoreVan store = StoreVan.getInstance();// 创建一个全局的Store对象
    public static final
    String MODID = "mcvan";// 在公共位置定义mod-id，以便所有内容都可以参考
    public static final
    String MOD_ID = "mcvan";// 在公共位置定义mod-id，以便所有内容都可以参考
    public static final
    String MOD_VERSION = "1.1";// 版本号
    public static final
    DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);// 创建一个延迟寄存器来保存所有将在“mcvan”命名空间下注册的方块
    public static final
    DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);// 创建一个延迟寄存器来保存CreativeModeTabs(创意模式选项卡)，这些标签都将在“mcvan”命名空间下注册
    private static final Logger LOGGER = LogUtils.getLogger();// 直接引用slf4j记录器
    private static Item[] allBowItems;//模组内所有弓的集合
    public static Path GAME_ROOT_PATH;

    /**
     * 注册相关
     */

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static final RegistryObject<MenuType<VideoControlMenu>> VIDEO_CONTROL_MENU =
            MENUS.register("video_control_menu", () ->
                    IForgeMenuType.create(VideoControlMenu::new)
            );


    public static final
    RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register(
            MOD_ID+"_tab",
        ()->{
            return CreativeModeTab.builder()
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .title(Component.translatable("itemGroup."+MODID+"."+MODID+"_tab"))
                .icon(() -> ModItems.BIG_ARROW.get().getDefaultInstance()) // 使用Lambda延迟获取//设置此选项卡的图标
                .displayItems(
                    (parameters,output)->{//设置选项卡内的物品
                        /**
                         * 这里可以添加选项卡内的物品
                         */
                        output.accept(ModItems.BIG_BOW.get());// 添加放大弓
                        output.accept(ModItems.BIG_ARROW.get());// 添加放大箭
                        output.accept(ModItems.VIDEO_SCREEN_BLOCK.get().getDefaultInstance());// 添加视频屏幕方块物品
                        output.accept(ModItems.VIDEO_SCREEN_BLOCK_1609.get().getDefaultInstance());// 添加视频屏幕方块物品
                        /**
                         * 这里可以添加选项卡内的物品
                         */
                    }
                ).build();
            }
    );// 创建一个id为“mcvan:mcvan_tab”的创造模式选项卡，该选项卡放置在战斗选项卡之后

    /**
     * describe: 根构造器
     * Constructor: IndexMod
     */
    public IndexMod(FMLJavaModLoadingContext context){
        IEventBus modEventBus = context.getModEventBus();// 获取mod事件总线

        BLOCKS.register(modEventBus);// 将延迟寄存器注册到mod事件总线，以便注册方块
        CREATIVE_MODE_TABS.register(modEventBus);// 将延迟寄存器注册到mod事件总线，以便注册创造模式的选项卡

        ModEntities.register(modEventBus);// 所有实体的注册
        ModItems.register(modEventBus);// 所有物品的注册
        ModBlocks.BLOCKS.register(modEventBus);// 所有方块的注册
        ModBlockEntities.register(modEventBus);// 所有方块实体的注册
        NetworkHandler.register();// 网络处理器的注册
        MENUS.register(modEventBus);// 所有菜单的注册

        MinecraftForge.EVENT_BUS.register(this);// 注册我们感兴趣的服务器和其他游戏活动

    }
    public IndexMod(){}
    @SuppressWarnings("all")
    static @NotNull Item @NotNull [] getAllBowItems() {
        if (allBowItems == null) {
            if (ModItems.BIG_BOW == null || !ModItems.BIG_BOW.isPresent()) {
                return new Item[]{};
            }
            allBowItems = new Item[]{ModItems.BIG_BOW.get()};
        }
        return allBowItems;
    }

    @SuppressWarnings("all")
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(
                ()->{// 使用 enqueueWork 包裹渲染器注册
                    EntityRenderers.register(// 注册箭矢实体的渲染
                        ModEntities.BIG_ARROW_ENTITY.get(),
                        BigArrowEntity.BigArrowRenderer::new
                    );
                    LOGGER.info(MODID+"客户端渲染器已注册...");
                }
            );
            /*
             * 设置透明渲染层
             */
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VIDEO_SCREEN_BLOCK.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.VIDEO_SCREEN_BLOCK_1609.get(), RenderType.cutout());
            LOGGER.info(MODID+"客户端已启动...");
        }

        @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
        public class ModEventHandler {

            @SubscribeEvent
            public static void onEntityAttributeCreate(EntityAttributeCreationEvent event) {
                event.put(ModEntities.BOSS.get(), BossEntity.createAttributes().build());
            }
        }
    }

    /**
     * 在世界加载时加载本地数据(主要是.vd视频数据)
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onWorldLoad(LevelEvent.Load event) {
            /*
             * 1.20.1 使用 getLevel() 而不是 getWorld()
             */
            if (event.getLevel() instanceof ServerLevel serverLevel) {// 防止在客户端执行
                if (serverLevel.dimension() == Level.OVERWORLD) {// 防止重复加载，但并不会意味着加载的数据只会在主世界有效
                    Path worldPath = serverLevel.getServer().getWorldPath(LevelResource.ROOT);
                    GAME_ROOT_PATH = worldPath;
                    System.out.println("[IndexMod]初始化本地数据中");
                    StoreVan.getInstance().initLocalData(worldPath);
                }
            }
        }

        /**
         * 玩家加入游戏事件处理
         **/
        @SubscribeEvent
        public static void onPlayerJoin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (!event.getEntity().level().isClientSide) {
                net.minecraft.server.MinecraftServer server = event.getEntity().getServer();// 获取服务器实例
                if (server != null) {
                    // 向所有玩家发送欢迎消息
                    // server.getPlayerList().broadcastSystemMessage(
                    //         Component.literal("有新玩家加入了游戏，欢迎光临！"),
                    //         false
                    // );
                    /*
                     * 初始状态更新动态材质
                     */
                    ConcurrentHashMap<String, VideoData> allVideoData = store.getAllVideoData();
                    BlockPos pos = new BlockPos(0,0,0);
                    allVideoData.forEach((key, value) -> {
                        if(value.getResolutionX() >= 1024){
                            NetworkHandler.INSTANCE.send(
                                    PacketDistributor.ALL.noArg(),//所有玩家都要收到
                                    new VideoBlockFrameIndexRenderPacket(pos, key,0,1609)
                            );
                        }
                        else if(value.getResolutionX() >= 256){
                            NetworkHandler.INSTANCE.send(
                                    PacketDistributor.ALL.noArg(),//所有玩家都要收到
                                    new VideoBlockFrameIndexRenderPacket(pos, key, 0, 0)
                            );
                        }
                    });
                }
            }
        }
    }
}
