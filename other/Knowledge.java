package com.myacghome.mcvan;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Service;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//收集一些forge的开发知识
public class Knowledge {
    // In a client class: ExampleClass
    @Contract(pure = true)
    public static @NotNull Runnable unsafeRunMethodExample(Object param1, Object param2) {
        return () -> {};
    }
    @Contract(pure = true)
    public static @Nullable Object safeCallMethodExample() {
        return null;
    }
    // This event is on the mod bus(游戏事件总线)
    private Runnable modEventHandler(RegisterEvent event) {
        return ()->{};
    }
    // This event is on the forge bus(模组特定事件总线)
    private static void forgeEventHandler(AttachCapabilitiesEvent<Entity> event) {
        // ...
    }
    public static void main(String[] args) {
    /***
    ####关于侧面 (sides) start
    ***/
        //1.我的世界游戏由多个java进程组成，每个java进程都有自己的内存空间，因此不同java进程之间的数据是不共享的。
        //  其中逻辑客户端在 Render Thread 中运行
        //  逻辑服务器始终在 Server Thread 中运行
        /**
         ##执行端特定操作
         一些代码的运行线程必须在特定的线程中运行，否则会出现一些意想不到的问题。
         判断侧面的方法

         Level.isClientSide 获取当前线程是否为客户端线程   返回值为true表示在客户端
         ##使用DistExecutor区分执行不同侧面应该执行的方法
         runWhenOn()
         callWhenOn()
         **/
        //在某个普通类中
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,//运行侧面
                () -> Knowledge.unsafeRunMethodExample("var1", "var2")//运行方法
        );
        DistExecutor.safeCallWhenOn(
                Dist.DEDICATED_SERVER,
                () -> Knowledge::safeCallMethodExample
        );
        DistExecutor.unsafeCallWhenOn(
                Dist.CLIENT,
                () -> Knowledge::safeCallMethodExample
        );
        /**
         ##线程组
         如果   Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER 为 true
         则    则当前线程很可能位于逻辑服务器上。
         否则  很可能位于逻辑客户端。这在您无法访问 Level 对象以检查 isClientSide 时检索逻辑端很有用
         **/
        //常见问题
        //每当你想从一个逻辑端向另一个逻辑端发送信息时，你必须始终使用网络数据包。
        //在单玩家场景中，直接从逻辑服务器向逻辑客户端传输数据是非常方便的。
        //由于逻辑客户端和逻辑服务器共享同一个 JVM，
        //因此向和从静态字段写入的线程都会导致各种竞态条件和与线程相关的经典问题。
    /***
    ####关于侧面 (sides) end
    ***/
    /***
    ####关于事件 (events) start
    ***/
        //Forge 使用一个事件总线，允许模组拦截来自各种 Vanilla 和模组行为的各种事件。
        //示例：当右击木棍时，可以使用事件执行操作。
        //大多数事件使用的主事件总线位于 MinecraftForge#EVENT_BUS 。(游戏事件总线)
        //还有一个用于特定情况下的模特定事件总线位于 FMLJavaModLoadingContext#getModEventBus ，(模组特定事件总线)
        //你应该只在特定情况下使用此总线。有关此总线的更多信息请见下文。
        //每个事件都在这些总线之一上触发：大多数事件在 Forge 主事件总线上触发，但也有一些在模组特定事件总线上触发。

        //创建事件处理器
        //事件处理器方法只有一个参数，不返回结果。该方法可以是静态的或实例的，具体取决于实现。
    }
        // In the mod constructor 在模组构造器中注册事件处理器
        Knowledge(FMLJavaModLoadingContext context) {
            IEventBus modEventBus = context.getModEventBus();
            modEventBus.addListener(this::modEventHandler);
        }
}
        //编写实例注解事件处理器
        //此事件处理器监听 EntityItemPickupEvent ，正如其名称所示，每当 Entity 拾取物品时，就会将其发布到事件总线。
        class MyForgeEventHandler  {
        @SubscribeEvent
        public void pickupItem(EntityItemPickupEvent event) {
            System.out.println("Item picked up!");
        }}
        //要注册此事件处理器，请使用 MinecraftForge.EVENT_BUS.register(...) 并传递事件处理器所在类的实例。
        //MinecraftForge.EVENT_BUS.register();
        //FMLJavaModLoadingContext.get().getModEventBus().register(...);

        //编写静态注解事件处理器
        //与实例处理器不同的是，它还标记为 static 。为了注册静态事件处理器，不需要类的实例。必须传递 Class 本身。
        class MyStaticForgeEventHandler {
        @SubscribeEvent
        public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
            // ...
        }}
        //必须像这样注册： MinecraftForge.EVENT_BUS.register(MyStaticForgeEventHandler.class)

        // 一个类可以被 @Mod$EventBusSubscriber 注解。
        // 当 @Mod 类本身被构造时，此类会自动注册到 MinecraftForge#EVENT_BUS 。
        // 这本质上等同于在 @Mod 类的构造函数末尾添加 MinecraftForge.EVENT_BUS.register(AnnotatedClass.class); 。
        // 您可以将您想监听的 bus 传递给 @Mod$EventBusSubscriber 注解。
        // 建议您还指定 mod id，因为注解过程可能无法推断出来，
        // 以及您要注册的 bus，因为它可以作为确保您在正确 bus 上的提醒。
        // 您还可以指定 Dist 或物理面来加载此事件订阅者。这可以用来确保不在专用服务器上加载客户端特定的事件订阅者。
        // 类似于这样：
        @Mod.EventBusSubscriber(modid = "mcvan", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
        class MyStaticClientOnlyEventHandler {
            @SubscribeEvent//渲染级别阶段事件
            public static void drawLast(RenderLevelStageEvent event) {
//                System.out.println("Drawing!");
            }
        }
    /***
    ####关于事件 (events) end
    ***/
    /***
    ####关于资源 (Resources) start
    ***/
    //所有资源应具有蛇形路径和文件名（小写，使用“_”作为单词边界），这在 1.11 及更高版本中是强制性的。
    //在资源文件夹中，您可以有以下子文件夹：
    //assets/modid/lang/ 包含语言文件
    //assets/modid/models/ 包含模型文件
    //assets/modid/textures/ 包含材质文件
    //assets/modid/sounds/ 包含声音文件
    //assets/modid/blockstates/ 包含方块状态文件
    //assets/modid/recipes/ 包含配方文件
    //assets/modid/loot_tables/ 包含战利品表文件
    //assets/modid/advancements/ 包含进度文件
    //assets/modid/data/ 包含数据文件（例如模型数据）
    //assets/modid/recipes/ 包含配方文件
    //assets/modid/structures/ 包含结构文件
    //assets/modid/functions/ 包含函数文件
    //assets/modid/entity/ 包含实体模型文件
    //assets/modid/particles/ 包含粒子文件
    //assets/modid/renderers/ 包含渲染器文件
    //assets/modid/shaders/ 包含着色器文件
//    Minecraft 使用 ResourceLocation 来识别资源。
//        ResourceLocation 包含两部分：一个命名空间和一个路径。
//        它通常指向 assets/<namespace>/<ctx>/<path> 的资源，
//        其中 ctx 是一个上下文特定的路径片段，这取决于 ResourceLocation 的使用方式。
//        当 ResourceLocation 以字符串形式写入/读取时，它被视为 <namespace>:<path> 。
//        如果省略了命名空间和冒号，那么当字符串被读取到 ResourceLocation 中时，命名空间将始终默认为 "minecraft" 。
//        模组应该将其资源放入与模组 ID 相同的命名空间中
//        （例如，ID 为 examplemod 的模组应将其资源放置在 assets/examplemod 和 data/examplemod 中，
//        指向这些文件的 ResourceLocation 将看起来像 examplemod:<path> ）。
//        这不是强制要求，在某些情况下，使用不同的（甚至多个）命名空间可能是可取的。
//        ResourceLocation 也在资源系统之外使用，因为它们是唯一标识对象（例如，注册表）的绝佳方式。