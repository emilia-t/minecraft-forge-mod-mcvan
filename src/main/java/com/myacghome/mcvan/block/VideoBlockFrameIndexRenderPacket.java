package com.myacghome.mcvan.block;

import com.myacghome.mcvan.Client;
import com.myacghome.mcvan.StoreVan;
import com.myacghome.mcvan.util.VideoData;
import com.myacghome.mcvan.util.VideoDataNull;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * s2c 服务端发送命令
 */
public class VideoBlockFrameIndexRenderPacket {
    private final BlockPos pos;
    private final int index;
    private String url;
    private int code;
    public VideoBlockFrameIndexRenderPacket(BlockPos pos ,String url ,int index,int code) {
        this.pos = pos;
        this.url = url;
        this.index = index;
        this.code = code;
    }

    /**
     * 反序列化
     * @param buf
     */
    public VideoBlockFrameIndexRenderPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.url = buf.readUtf();
        this.index = buf.readInt();
        this.code = buf.readInt();
    }

    /**
     * 序列化
     * @param buf
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeUtf(url);
        buf.writeInt(index);
        buf.writeInt(code);
    }

    /**
     * 客户端处理
     * @param ctx
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {

            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {// 确保只在客户端执行

                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();// 获取客户端世界
                if (client.level != null) {
                    StoreVan storeVan = StoreVan.getInstance();
                    VideoData videoData = storeVan.getVideoData(url);
                    if(code == 0){
                        if(videoData instanceof VideoDataNull){
//                        ToolVan.logToChat("[VideoBlockFrameIndexRenderPacket]视频还未入库");
                        }
                        else{
//                        ToolVan.logToChat("[VideoBlockFrameIndexRenderPacket]已更新动态材质");
                            Client.getDynamicTextureSource().updateTextureFromPixels(videoData.getFrame(index));
                        }
                    }
                    else if(code == 1609){
                        if(videoData instanceof VideoDataNull){
//                        ToolVan.logToChat("[VideoBlockFrameIndexRenderPacket]视频还未入库");
                        }
                        else{
//                        ToolVan.logToChat("[VideoBlockFrameIndexRenderPacket]已更新动态材质");
                            Client.getDynamicTextureSource1609().updateTextureFromPixels(videoData.getFrame(index));
                        }
                    }
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
