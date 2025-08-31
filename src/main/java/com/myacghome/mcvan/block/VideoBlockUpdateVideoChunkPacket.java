package com.myacghome.mcvan.block;

import com.myacghome.mcvan.StoreVan;
import com.myacghome.mcvan.util.VideoData;
import com.myacghome.mcvan.util.VideoDataNull;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 用于更新帧（服务端向客户端发送） 以 分块的方式发送，适用于需要发送大型视频时
 */

public class VideoBlockUpdateVideoChunkPacket {
    private final int[] pixelsChunkData;
    private final String url;
    private final int resolutionX;
    private final int resolutionY;
    private final int frameNumber;
    private final int chunkOffset;
    private final int totalSize;

    public VideoBlockUpdateVideoChunkPacket(int[] pixelsChunkData, String url,
                                            int resolutionX, int resolutionY,
                                            int frameNumber, int chunkOffset,
                                            int totalSize) {
        this.pixelsChunkData = pixelsChunkData;
        this.url = url;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
        this.frameNumber = frameNumber;
        this.chunkOffset = chunkOffset;
        this.totalSize = totalSize;
    }


    public void toBytes(FriendlyByteBuf buf) {// 序列化
        buf.writeUtf(url);
        buf.writeInt(resolutionX);
        buf.writeInt(resolutionY);
        buf.writeInt(frameNumber);
        buf.writeInt(chunkOffset);
        buf.writeInt(totalSize);
        buf.writeVarIntArray(pixelsChunkData);
    }

    public VideoBlockUpdateVideoChunkPacket(FriendlyByteBuf buf) {//反序列化方法
        this.url = buf.readUtf();
        this.resolutionX = buf.readInt();
        this.resolutionY = buf.readInt();
        this.frameNumber = buf.readInt();
        this.chunkOffset = buf.readInt();
        this.totalSize = buf.readInt();
        this.pixelsChunkData = buf.readVarIntArray();
    }


    public void handle(Supplier<NetworkEvent.Context> ctx) {// 处理程序（客户端侧）
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {// 确保只在客户端执行
                net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();// 获取客户端世界
                if (client.level != null) {
                    FrameReassemblyManager.handleChunk(this);// 使用帧重组管理器处理分块
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static class FrameReassemblyManager {// 帧重组管理器
        private static final Map<String, Map<Integer, FrameBuffer>> buffers = new ConcurrentHashMap<>();
        private static VideoBlockUpdateVideoChunkPacket chunkPacket;
        public static void handleChunk(VideoBlockUpdateVideoChunkPacket packet) {
            chunkPacket=packet;
            String videoKey = packet.url;
            int frameNum = packet.frameNumber;

            buffers.computeIfAbsent(videoKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(frameNum, k -> new FrameBuffer(packet.totalSize))
                    .addChunk(packet.pixelsChunkData, packet.chunkOffset);
        }

        private static class FrameBuffer {
            private final int[] data;
            private final AtomicInteger received;
            private final int totalSize;

            public FrameBuffer(int totalSize) {
                this.data = new int[totalSize];
                this.received = new AtomicInteger(0);
                this.totalSize = totalSize;
            }

            public synchronized void addChunk(int[] chunk, int offset) {
                if (offset < 0 || offset + chunk.length > totalSize) {// 检查偏移和长度是否有效
                    System.err.println("无效的分块偏移或长度: offset=" + offset +
                            ", length=" + chunk.length +
                            ", totalSize=" + totalSize);
                    return;
                }

                System.arraycopy(chunk, 0, data, offset, chunk.length);// 将分块数据复制到缓冲区
                int newReceived = received.addAndGet(chunk.length);

                if (newReceived >= totalSize) {// 检查是否已接收完整帧
                    processCompleteFrame();
                }
            }

            private void processCompleteFrame() {
                StoreVan store = StoreVan.getInstance();
                VideoData videoData = store.getVideoData(chunkPacket.url);

                if (videoData instanceof VideoDataNull) {// 如果视频数据不存在，则创建新的
                    videoData = new VideoData(chunkPacket.url, chunkPacket.resolutionX, chunkPacket.resolutionY);
                    store.setVideoData(chunkPacket.url, videoData);
                }

                videoData.addFrame(data);// 添加完整帧到视频数据

                buffers.values().removeIf(frameMap -> {// 清理缓冲区
                    frameMap.remove(chunkPacket.frameNumber);
                    return frameMap.isEmpty();
                });
            }
        }
    }
}