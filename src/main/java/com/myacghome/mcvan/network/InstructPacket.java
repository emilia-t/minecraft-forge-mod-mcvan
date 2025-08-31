package com.myacghome.mcvan.network;

import com.myacghome.mcvan.ToolVan;
import com.myacghome.mcvan.block.VideoBlockUpdateVideoChunkPacket;
import com.myacghome.mcvan.block.VideoScreenBlockEntity;
import com.myacghome.mcvan.block.VideoScreenBlockEntity1609;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

import com.myacghome.mcvan.util.*;
import static com.myacghome.mcvan.IndexMod.store;
import static com.myacghome.mcvan.network.InstructCreate.load_video;
import static com.myacghome.mcvan.network.InstructCreate.get_video_frame_count;

public class InstructPacket {
    private final int code;
    private final String content; // JSON格式指令内容
    private InstructParser parsedCommand; // 缓存解析后的指令
    private FFmpegProcess ffmpegProcess;//外部FFmpeg进程
    private VideoDownloader videoDownloader;
    public InstructPacket(int code, String content) {
        this.code = code;
        this.content = content;
    }
    /**
     * 反序列化
     * @param buf
     */
    public InstructPacket(FriendlyByteBuf buf) {
        this.code = buf.readInt();
        this.content = buf.readUtf();
    }

    /**
     * 序列化
     * @param buf
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(code);
        buf.writeUtf(content);
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }

    /**
     * 获取解析后的指令对象
     * @return 解析后的指令
     */
    public InstructParser getCommand() {
        if (parsedCommand == null) {
            parsedCommand = InstructParser.fromJson(content);
        }
        return parsedCommand;
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            try {
                InstructParser command = getCommand();
                if (ctx.get().getDirection().getReceptionSide().isClient()) {//接收端为客户端
                    handleClientCommand(command,player);
                } else {//接收端为服务端
                    handleServerCommand(command,player);
                }
            } catch (Exception e) {
                System.err.println("[InstructPacket]Error processing command: " + content);
                e.printStackTrace();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 服务端执行
     * @param command
     */
    private void handleServerCommand(InstructParser command,ServerPlayer player) {
        switch (command.getName()) {
            case load_video: {
                String url = command.getParamValue("url");
                Number posX = ToString2Number.ToString2Number(command.getParamValue("posX"));
                Number posY = ToString2Number.ToString2Number(command.getParamValue("posY"));
                Number posZ = ToString2Number.ToString2Number(command.getParamValue("posZ"));


                BlockPos pos = new BlockPos(posX.intValue(),posY.intValue(),posZ.intValue());//获取方块实体坐标
                BlockEntity be = player.level().getBlockEntity(pos);

                int widthBlocks;  // 改为int避免溢出//转换为分辨率和方块数
                int heightBlocks; // 改为int避免溢出
                byte safeWidthBlocks;
                byte safeHeightBlocks;
                int x1;//分辨率
                int y1;
                double x1d;//分辨率
                double y1d;
                if(be instanceof VideoScreenBlockEntity){//4x3方块
                    widthBlocks=4;
                    safeWidthBlocks=(byte)widthBlocks;
                    heightBlocks=3;
                    safeHeightBlocks=(byte)heightBlocks;
                    x1=256;
                    y1=-2;
                    x1d=256.0d;
                    y1d=-2.0d;
                }
                else if(be instanceof VideoScreenBlockEntity1609){//16x9方块
                    widthBlocks=4;
                    safeWidthBlocks=(byte)widthBlocks;
                    heightBlocks=3;
                    safeHeightBlocks=(byte)heightBlocks;
                    x1=1024;
                    y1=-2;
                    x1d=1024.0d;
                    y1d=-2.0d;
                }else{//默认值4x3
                    widthBlocks=4;
                    safeWidthBlocks=(byte)widthBlocks;
                    heightBlocks=3;
                    safeHeightBlocks=(byte)heightBlocks;
                    x1=256;
                    y1=-2;
                    x1d=256.0d;
                    y1d=-2.0d;
                }
                if(Objects.equals(x1, 0) || Objects.equals(y1, 0)){//需要设置为默认值
                    x1 = VideoData.resolutionXDefault;
                    y1 = VideoData.resolutionYDefault;
                    widthBlocks = VideoData.blockWidthDefault;
                    heightBlocks = VideoData.blockHeightDefault;
                    safeWidthBlocks = (byte) widthBlocks;//防止宽度大于 127
                    safeHeightBlocks = (byte) heightBlocks;//防止高度大于 127
                }
                else {//需要设置为自定义值
                    if(Objects.equals(x1,1024)){

                        widthBlocks = (int) Math.ceil(x1d / VideoData.blockPixelDefaultD);// 向上取整计算块数（使用Math.ceil）
                        if(y1 == VideoData.resolutionYDefault){
                            heightBlocks = 9;
                        }
                        else{
                            heightBlocks = (int) Math.ceil(y1d / VideoData.blockPixelDefaultD);
                        }
                    }
                    else if(Objects.equals(x1,256)){

                        widthBlocks = (int) Math.ceil(x1d / VideoData.blockPixelDefaultD);// 向上取整计算块数（使用Math.ceil）
                        if(y1 == VideoData.resolutionYDefault){
                            heightBlocks = VideoData.blockHeightDefault;
                        }
                        else{
                            heightBlocks = (int) Math.ceil(y1d / VideoData.blockPixelDefaultD);
                        }
                    }
                    safeWidthBlocks = (widthBlocks > Byte.MAX_VALUE) ? Byte.MAX_VALUE : (byte) widthBlocks;//防止宽度大于 127
                    safeHeightBlocks = (heightBlocks > Byte.MAX_VALUE) ? Byte.MAX_VALUE : (byte) heightBlocks;//防止高度大于 127
                }

                if(url == null){
                    break;
                }
                if(Objects.equals(url,"")){
                    break;
                }
                System.out.println("[服务端InstructPacket]视频链接：" + url);
                System.out.println("[服务端InstructPacket]XYZ：" + posX + " , " + posY + " , " + posZ);
                System.out.println("[服务端InstructPacket]正在发送视频数据包");
                    /*
                     * 1.创建ffmpeg进程
                     * 2.获取视频帧数据
                     * 3.将视频帧数据转换为像素数组
                     * 4.发送视频帧数据包至客户端
                     * 5.保存视频数据(videoData 内存中)在服务端以备用
                     * 6.保存源视频文件在服务端以备用( .mp4 之类的 存档 .storeVan内)
                     */
                ffmpegProcess = new FFmpegProcess();
                Number finalX = x1;
                Number finalY = y1;
                ffmpegProcess.start(
                        url,
                        (FFmpegProcess.FrameHandle) (frameData, frameNumber) -> {
                            /* 从第一个像素中提取帧序号
                             * int frameNumberRGB = (frameData[0] & 0xFF) << 16 | (frameData[1] & 0xFF) << 8 | (frameData[2] & 0xFF);
                             * System.out.println("[服务端InstructPacket]Received frame number: " + frameNumberRGB);
                             */
                            int maxChunkSize = 18432; // 18432 pixel 分块
                            int[] tempPixels = ToRgbData2Pixels.ToRgbData2Pixels(frameData, (int) finalX);
                            int totalSize = tempPixels.length;

                            for (int offset = 0; offset < totalSize; offset += maxChunkSize) {
                                NetworkHandler.INSTANCE.send(
                                        PacketDistributor.ALL.noArg(),
                                        new VideoBlockUpdateVideoChunkPacket(
                                                Arrays.copyOfRange(tempPixels,offset, Math.min(offset + maxChunkSize, totalSize)),
                                                url,
                                                (int) finalX,
                                                (int) finalY,
                                                frameNumber,
                                                offset,
                                                totalSize
                                        )
                                );// 发送分块数据包
                            }

                            if (tempPixels.length <= 1) {
                                return;
                            }
                            else {
                                VideoData isExists = store.getVideoData(url);// 将帧数据保存在服务端的storeVan内以备用
                                if (isExists instanceof VideoDataNull) { // 视频数据还未创建
                                    VideoData videoData = new VideoData(url, (int) finalX, (int) finalY);
                                    store.setVideoData(url, videoData);
                                } else { // 视频数据已创建
                                    store.getVideoData(url).addFrame(tempPixels);
                                }
                                //System.out.println("[服务端InstructPacket]已发送视频帧数据包");
                            }
                        },
                        safeWidthBlocks,
                        safeHeightBlocks
                );
                int resolution_X = ffmpegProcess.getResolutionX();
                int resolution_Y = ffmpegProcess.getResolutionY();
                videoDownloader = new VideoDownloader(resolution_X,resolution_Y,url,safeWidthBlocks,safeHeightBlocks);
                videoDownloader.downloadAndProcessVideo();
                break;
            }
            case get_video_frame_count: {
                String url = command.getParamValue("url");
                VideoData videoData = store.getVideoData(url);
                if (videoData instanceof VideoDataNull) {
                    break;
                } else {
                    System.out.println("[服务端InstructPacket]该视频帧数总数为:" + videoData.getFrameCount());
                }
                break;
            }
            default: {
                System.err.println("[服务端InstructPacket]未知的指令: " + command.getName());
            }
        }
    }

    /**
     * 客户端执行
     * @param command
     */
    private void handleClientCommand(InstructParser command,ServerPlayer player) {
        switch (command.getName()) {
            case get_video_frame_count:{
                String url = command.getParamValue("url");
                VideoData videoData = store.getVideoData(url);
                if(videoData instanceof VideoDataNull){
                    break;
                }
                else {
                    ToolVan.logToChat("[客户端InstructPacket]该视频帧数总数为:"+videoData.getFrameCount());
                }
                break;
            }
            default:{
                ToolVan.logToChat("[客户端InstructPacket]未知的指令: " + command.getName());
            }
        }
    }
}
