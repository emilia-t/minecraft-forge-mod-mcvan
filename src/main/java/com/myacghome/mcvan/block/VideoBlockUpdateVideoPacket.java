package com.myacghome.mcvan.block;

import com.myacghome.mcvan.StoreVan;
import com.myacghome.mcvan.util.VideoData;
import com.myacghome.mcvan.util.VideoDataNull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * 用于更新帧（服务端向客户端发送） 只会更新客户端的 storeVan内的视频数据，视屏数据不会保存在方块实体上
 */
public class VideoBlockUpdateVideoPacket {
    private final int[] pixelsFrameData;
    private final String url;
    private int resolutionX;
    private int resolutionY;

    /**
     * 由服务端创建
     * @param pixelsFrameData
     * @param url
     * @param resolutionX
     * @param resolutionY
     */
    public VideoBlockUpdateVideoPacket( int[] pixelsFrameData, String url , int resolutionX, int resolutionY) {
        this.pixelsFrameData = pixelsFrameData;
        this.url = url;
        this.resolutionX=resolutionX;
        this.resolutionY=resolutionY;
    }

    /**
     * 反序列化构造器
     */
    public VideoBlockUpdateVideoPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf();         // 读取URL字符串
        this.resolutionX = buf.readInt(); // 读取分辨率X
        this.resolutionY = buf.readInt(); // 读取分辨率Y
        int length = buf.readInt();       // 读取帧数组长度
        this.pixelsFrameData = new int[length];
        for (int i = 0; i < length; i++) {
            pixelsFrameData[i] = buf.readInt(); // 逐个读取帧数据
        }
    }

    /**
     * 序列化方法
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(url);              // 写入URL字符串
        buf.writeInt(resolutionX);      // 写入分辨率X
        buf.writeInt(resolutionY);      // 写入分辨率Y
        buf.writeInt(pixelsFrameData.length); // 写入帧数组长度
        for (int value : pixelsFrameData) {
            buf.writeInt(value);        // 逐个写入帧数据
        }
    }

    /**
     * 数据包处理逻辑（客户端执行）
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {// 确保只在客户端执行
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();// 获取客户端世界
                if (client.level != null) {
                    /*
                    //1.在客户端线程中创建 videoData
                    //2.存储或更新 videoData 帧数据
                    */
                    StoreVan store = StoreVan.getInstance();
                    VideoData isExists = store.getVideoData(url);
                    if(isExists instanceof VideoDataNull){//视频数据还未创建
                        System.out.println("[VideoBlockUpdateVideoPacket]新创建视频数据分辨率:"+resolutionX+" x "+resolutionY);
                        VideoData videoData = new VideoData(url,resolutionX,resolutionY);
                        store.setVideoData(url,videoData);
                    }
                    else{//视频数据已经创建
                        store.getVideoData(url).addFrame(pixelsFrameData);
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
