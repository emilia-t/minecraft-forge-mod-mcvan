package com.myacghome.mcvan.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
数据包结构类似于：
{
  "name": "dance_video",//视频名称，自动从url中截取(从最后一个 "/" 开始截取，并且移除 ".*" )
  "url": "https://atsw.top/evan/dance_video.mp4",//视频链接
  "frameRate": 30,//每秒多少帧
  "frameCount": 30,//单位为帧
  "videoDuration": 1,//单位为秒
  "frames": [
    [],
    []
  ]
}
 */
public class VideoData {
    private final String name;
    private final String url;
    private int frameRate;
    private float videoDuration;
    private List<int[]> frames = new ArrayList<>();
    private int resolutionX;//实际的视频分辨率x
    private int resolutionY;//实际的视频分辨率y
    public static final int FPS = 30;
    public static final String fps = "30";//所有视频固定输出帧率
    public static final int blockPixelDefault = 64;//视频方块每方块的最大像素(64)
    public static final double blockPixelDefaultD = 64.0D;//视频方块每方块的最大像素(64)
    public static final float blockPixelDefaultF = 64.0F;//视频方块每方块的最大像素(64)
    public static final int blockWidthDefault = 4;//视频方块的宽度
    public static final int blockHeightDefault = 3;//视频方块的高度
    public static final int resolutionXDefault = 256;//最大宽度（像素）
    public static final int resolutionYDefault = -2;//等比例缩放（后期会通过第一帧的帧长度并除以256得到具体的高度）
    private int frameLastAddSerial = 0;

    /* 以下是 16进制 4 x 8 分辨率 单帧图像示例
        0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,
        0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,
        0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,
        0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000,0xFF000000
     */
    private int currentFrameIndex = 0;
    public static final String U_V_D = "U_V_D";
    public VideoData(String url,int resolutionX,int resolutionY) {
        if(resolutionY <= 0){
            resolutionY = resolutionYDefault;
        }
        if(resolutionX <= 0){
            resolutionX = resolutionXDefault;
        }
        System.out.println("[VideoData] 新建videoData 分辨率("+resolutionX+" x "+resolutionY+")");
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
        if(Objects.equals(url, U_V_D)){
            this.url = U_V_D;
            this.name = U_V_D;
        }
        else{
            this.url = url;
            int lastSlash = url.lastIndexOf('/');
            int dotIndex = url.lastIndexOf('.');
            if (lastSlash != -1 && dotIndex > lastSlash) {
                this.name = url.substring(lastSlash + 1, dotIndex);
            } else {
                this.name = "undefined_url";
            }
        }
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public int getFrameRate() { return frameRate; }
    public void setFrameRate(int frameRate) { this.frameRate = frameRate; }
    public float getVideoDuration() { return videoDuration; }
    public void setVideoDuration(float videoDuration) { this.videoDuration = videoDuration; }
    public int getFrameCount(){return frames.size();}
    public List<int[]> getFrames() { return frames; }
    public int getResolutionX() { return resolutionX; }
    public int getResolutionY() { return resolutionY; }

    private int getVideoResolutionYType1(int[] frameData){ //通过获取第一帧的帧长并除以 resolutionXDefault 得到实际的视频高度
        if(this.resolutionX<=0){
            return (int)Math.ceil((double)frameData.length/(double)resolutionXDefault);
        }
        else{
            return (int)Math.ceil((double)frameData.length/(double)this.resolutionX);
        }
    }


    private int getFrameSerialType1(int[] frameData) {//通过获取视频帧的第一个像素来获取帧序列

        int pixel = frameData[0];// 提取第一个像素的 ARGB 值

        int r = (pixel >> 16) & 0xFF;// 提取 R、G、B 值
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;

        return (r << 16) | (g << 8) | b;// 组合成帧序号
    }

    public void addFrame(int[] pixelsFrameData) {
        if(this.resolutionY==resolutionYDefault){
            this.resolutionY=getVideoResolutionYType1(pixelsFrameData);
        }
        int nowSerial = getFrameSerialType1(pixelsFrameData);
        if(nowSerial<=this.frameLastAddSerial){
            return;//避免重复帧存入
        }
        this.frameLastAddSerial=nowSerial;
        frames.add(pixelsFrameData);
    }

    public int[] getFrame(int index) {
        if (index < 0 || index >= frames.size()) {
            return new int[0];
        }
        return frames.get(index);
    }

    public int[] getCurrentFrame() {
        if (frames.isEmpty()) return new int[0];
        return frames.get(currentFrameIndex);
    }

    public void nextFrame() {
        currentFrameIndex = (currentFrameIndex + 1) % frames.size();
    }

    public void saveLastFrameToFile() {
        if (frames == null || frames.isEmpty()) {
            System.out.println("[VideoData] 无法保存最后一帧：帧列表为空");
            return;
        }


        int[] lastFrame = frames.get(frames.size() - 1);// 获取最后一帧数据


        if (resolutionX <= 0 || resolutionY <= 0) {// 检查分辨率是否有效
            System.out.println("[VideoData] 错误：无效的分辨率设置("+resolutionX+" x "+resolutionY+")");
            System.out.println("[VideoData] 错误：url(" + url + ")");
            return;
        }


        if (lastFrame.length != resolutionX * resolutionY) {// 检查帧数据大小是否匹配分辨率
            System.out.printf("[VideoData] 错误：帧数据大小(%d)与分辨率(%d x %d = %d)不匹配%n",
                    lastFrame.length, resolutionX, resolutionY, resolutionX * resolutionY);
            return;
        }

        try {

            com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(resolutionX, resolutionY, false);// 创建NativeImage对象（使用RGBA格式）


            for (int y = 0; y < resolutionY; y++) {// 将int[]数据转换为NativeImage
                for (int x = 0; x < resolutionX; x++) {
                    int index = y * resolutionX + x;
                    if (index < lastFrame.length) {
                        int argb = lastFrame[index];
                        // 转换ARGB到RGBA格式
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                        image.setPixelRGBA(x, y, rgba);
                    }
                }
            }


            Path screenshotsDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");// 获取Minecraft截图目录
            Files.createDirectories(screenshotsDir);


            String filename = String.format(// 生成唯一文件名
                    "%s_lastframe_%d.png",
                    this.name, System.currentTimeMillis()
            );
            Path outputPath = screenshotsDir.resolve(filename);


            image.writeToFile(outputPath);// 保存图像文件
            System.out.println("[VideoData] 最后一帧已保存至: " + outputPath);


            image.close();// 释放NativeImage资源
        } catch (IOException e) {
            System.err.println("[VideoData] 保存最后一帧时发生IO异常:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[VideoData] 处理帧数据时发生错误:");
            e.printStackTrace();
        }
    }

    public void saveRandomFrameToFile() {
        if (frames == null || frames.isEmpty()) {
            System.out.println("[VideoData] 无法保存随机一帧：帧列表为空");
            return;
        }
        int selectIndex = CreateRandomNumber.CreateRandomNumber(0,frames.size() - 1);

        int[] selectFrame = frames.get(selectIndex);// 获取随机一帧数据


        if (resolutionX <= 0 || resolutionY <= 0) {// 检查分辨率是否有效
            System.out.println("[VideoData] 错误：无效的分辨率设置("+resolutionX+" x "+resolutionY+")");
            System.out.println("[VideoData] 错误：url(" + url + ")");
            return;
        }


        if (selectFrame.length != resolutionX * resolutionY) {// 检查帧数据大小是否匹配分辨率
            System.out.printf("[VideoData] 错误：帧数据大小(%d)与分辨率(%d x %d = %d)不匹配%n",
                    selectFrame.length, resolutionX, resolutionY, resolutionX * resolutionY);
            return;
        }

        try {
            com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(resolutionX, resolutionY, false);// 创建NativeImage对象（使用RGBA格式）

            for (int y = 0; y < resolutionY; y++) {// 将int[]数据转换为NativeImage
                for (int x = 0; x < resolutionX; x++) {
                    int index = y * resolutionX + x;
                    if (index < selectFrame.length) {
                        int argb = selectFrame[index];
                        // 转换ARGB到RGBA格式
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;
                        int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                        image.setPixelRGBA(x, y, rgba);
                    }
                }
            }

            Path screenshotsDir = net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");//Minecraft截图目录
            Files.createDirectories(screenshotsDir);


            String filename = String.format(// 生成唯一文件名
                    "%s_frame_%d_%d.png",
                    this.name,
                    selectIndex,
                    System.currentTimeMillis()
            );
            Path outputPath = screenshotsDir.resolve(filename);

            image.writeToFile(outputPath);// 保存图像文件
            System.out.println("[VideoData] 随机一帧已保存至: " + outputPath);

            image.close();
        } catch (IOException e) {
            System.err.println("[VideoData] 保存随机一帧时发生IO异常:");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[VideoData] 处理帧数据时发生错误:");
            e.printStackTrace();
        }
    }
}