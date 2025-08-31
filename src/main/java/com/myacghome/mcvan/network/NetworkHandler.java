package com.myacghome.mcvan.network;

import com.myacghome.mcvan.block.*;
import com.myacghome.mcvan.block.VideoBlockFrameIndexRenderPacket;
import com.myacghome.mcvan.block.VideoBlockUpdateVideoChunkPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@SuppressWarnings("all")
public class NetworkHandler {
    public static final String PROTOCOL_VERSION = "1";//协议版本
    public static final SimpleChannel INSTANCE =
            NetworkRegistry.newSimpleChannel(//创建一个频道
                new ResourceLocation("mcvan", "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    /**
     * 注册数据包类型
     * 将自定义数据包（如 UpdateUrlPacket、FrameDataPacket）绑定到特定 ID，并关联序列化与处理逻辑。
     * 客户端通过 INSTANCE.sendToServer() 发送数据包到服务器，
     * 服务器通过 INSTANCE.send(PacketDistributor.TRACKING_ENTITY, ...) 发送到客户端。
     */
    public static void register() {

        INSTANCE.registerMessage(id++,
                VideoBlockUpdateUrlPacket.class,
                VideoBlockUpdateUrlPacket::toBytes,
                VideoBlockUpdateUrlPacket::new,
                VideoBlockUpdateUrlPacket::handle);// 注册URL更新包

        INSTANCE.registerMessage(id++,
                VideoBlockUpdateVideoPacket.class,
                VideoBlockUpdateVideoPacket::toBytes,
                VideoBlockUpdateVideoPacket::new,
                VideoBlockUpdateVideoPacket::handle);// 注册视频帧更新包

        INSTANCE.registerMessage(id++,
                InstructPacket.class,
                InstructPacket::toBytes,
                InstructPacket::new,
                InstructPacket::handle);// 注册全局指令包

        INSTANCE.registerMessage(id++,
                VideoBlockFrameIndexNextPacket.class,
                VideoBlockFrameIndexNextPacket::toBytes,
                VideoBlockFrameIndexNextPacket::new,
                VideoBlockFrameIndexNextPacket::handle);// 注册视频帧下一帧索引更新包

        INSTANCE.registerMessage(id++,
                VideoBlockFrameIndexRenderPacket.class,
                VideoBlockFrameIndexRenderPacket::toBytes,
                VideoBlockFrameIndexRenderPacket::new,
                VideoBlockFrameIndexRenderPacket::handle);// 注册渲染目标帧包 (s2c服务端往客户端)

        INSTANCE.registerMessage(id++,
                VideoBlockFrameIndexBackPacket.class,
                VideoBlockFrameIndexBackPacket::toBytes,
                VideoBlockFrameIndexBackPacket::new,
                VideoBlockFrameIndexBackPacket::handle);// 注册视频帧上一帧索引更新包

        INSTANCE.registerMessage(id++,
                VideoBlockFrameIndexRestartPacket.class,
                VideoBlockFrameIndexRestartPacket::toBytes,
                VideoBlockFrameIndexRestartPacket::new,
                VideoBlockFrameIndexRestartPacket::handle);// 注册视频帧重新开始更新包

        INSTANCE.registerMessage(id++,
                VideoBlockUpdateVideoChunkPacket.class,
                VideoBlockUpdateVideoChunkPacket::toBytes,
                VideoBlockUpdateVideoChunkPacket::new,
                VideoBlockUpdateVideoChunkPacket::handle);// 注册视频帧重新开始更新包
    }
}
/*

下面是这些数据包的使用示例

InstructPacket包：
1. 创建和发送指令包
java
// 创建JSON指令内容
String jsonCommand = "{"
    + "\"type\":\"command\","
    + "\"name\":\"play_video\","
    + "\"parameters\":\"--url https://example.com/video.mp4\""
    + "}";

// 发送给所有客户端（服务端代码）
NetworkHandler.INSTANCE.send(
    PacketDistributor.ALL.noArg(), // 发送给所有连接客户端
    new InstructPacket(1, jsonCommand)
);

// 客户端发送给服务端（客户端代码）
NetworkHandler.INSTANCE.sendToServer(
    new InstructPacket(2, "{...}")
);
2. 处理指令包（示例扩展）
java
public void handle(Supplier<NetworkEvent.Context> ctx) {
    ctx.get().enqueueWork(() -> {
        // 解析JSON指令
        JsonObject json = new JsonParser().parse(content).getAsJsonObject();
        String type = json.get("type").getAsString();
        String name = json.get("name").getAsString();

        if ("command".equals(type)) {
            switch (name) {
                case "play_video":
                    String parameters = json.get("parameters").getAsString();
                    handlePlayVideo(parameters);
                    break;
                case "pause_video":
                    handlePauseVideo();
                    break;
                // 添加更多指令...
            }
        }
    });
    ctx.get().setPacketHandled(true);
}

private void handlePlayVideo(String parameters) {
    // 解析参数并执行播放操作
    if (parameters.startsWith("--url")) {
        String url = parameters.substring(6);
        System.out.println("播放视频: " + url);
    }
}


 */