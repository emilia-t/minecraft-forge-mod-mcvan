package com.myacghome.mcvan.block;

import com.myacghome.mcvan.util.VideoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class ScreenDynamicTexture {
    private final ResourceLocation textureId;
    private final DynamicTexture dynamicTexture;
    private final Minecraft mc = Minecraft.getInstance();
    private final int width = VideoData.blockWidthDefault*VideoData.blockPixelDefault;
    private final int height = VideoData.blockHeightDefault*VideoData.blockPixelDefault;
    private int updateCount = 0;
    private boolean fillBlack = false;
    public static int viewScreenX=0;//实际显示区域的宽度像素
    public static int viewScreenY=0;//实际显示区域的高度像素
    private static final ResourceLocation DEFAULT_SILVER_GRAY_TEXTURE = new ResourceLocation("textures/block/tuff.png"); // 灰色凝灰岩纹理
    private boolean isDefaultTextureActive = true;// 默认材质标记 当没有帧图像时
    public static final int DEFAULT_WIDTH = 256;// 默认材质尺寸
    public static final int DEFAULT_HEIGHT = 192;
    public static final int MAX_PIXELS_SIZE = 49152;

    public ScreenDynamicTexture(String id) {
        this.dynamicTexture = new DynamicTexture(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
        this.textureId = mc.getTextureManager().register(id, dynamicTexture);
        fillSilverGray();
    }

    /**
     * 填充银灰色默认材质（灰色凝灰岩）
     */
    public void fillSilverGray() {
        NativeImage image = dynamicTexture.getPixels();
        if (image != null) {
            for (int y = 0; y < DEFAULT_HEIGHT; y++) {// 创建纯银灰色背景
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    image.setPixelRGBA(x, y, 0xFFC0C0C0);// 银灰色：RGB(192, 192, 192)
                }
            }

            for (int y = 0; y < DEFAULT_HEIGHT; y++) {// 添加凝灰岩纹理特征（简单的噪声模式）
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    if ((x + y) % 4 == 0) {// 添加一些纹理变化
                        int pixel = image.getPixelRGBA(x, y);
                        int variation = (int)(Math.random() * 20) - 10;// 稍微加深或减浅像素
                        int r = Math.min(255, Math.max(0, ((pixel >> 16) & 0xFF) + variation));
                        int g = Math.min(255, Math.max(0, ((pixel >> 8) & 0xFF) + variation));
                        int b = Math.min(255, Math.max(0, (pixel & 0xFF) + variation));
                        image.setPixelRGBA(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
                    }
                }
            }

            dynamicTexture.upload();
            setViewScreenX(DEFAULT_WIDTH);// 设置视图尺寸
            setViewScreenY(DEFAULT_HEIGHT);
            isDefaultTextureActive = true;
        }
    }

    /**
     * 接收一个16进制的图像数据来更新动态材质
     * 对于不匹配纹理尺寸的将自动填充黑色
     * @param pixels ARGB格式的像素数组
     */
    public void updateTextureFromPixels(int[] pixels) {
        if(pixels.length > MAX_PIXELS_SIZE){
            return;
        }
        NativeImage image = dynamicTexture.getPixels();
        int expectedPixels = width * height;
        if(viewScreenX==0 || viewScreenY==0){//避免重复计算（以第一帧为准）
            setViewScreenX(width);//固定宽度
            setViewScreenY(pixels.length/width);
        }
        if(isDefaultTextureActive){//当之前是默认材质时需要重新更新 viewScreen
            setViewScreenX(width);//固定宽度
            setViewScreenY(pixels.length/width);
            isDefaultTextureActive = false;// 重置默认材质标记
        }
        if (pixels.length != expectedPixels) {//非全屏视频 需要填充其他颜色
            if(updateCount == 0){
                if(fillBlack){
                    System.out.println("[ScreenDynamicTexture] 16进制数据长度不匹配纹理尺寸将自动填充黑色");
                }
                else{
                    System.out.println("[ScreenDynamicTexture] 16进制数据长度不匹配纹理尺寸将自动填充透明色");
                }
            }

            int[] correctedPixels = new int[expectedPixels];// 创建新的像素数组并用黑色或透明色填充
            Arrays.fill(correctedPixels, fillBlack?0xFF000000:0x00000000);

            int startPos = (expectedPixels - pixels.length) / 2;// 计算居中位置
            startPos = Math.max(0, startPos); // 确保不会为负

            int copyLength = Math.min(pixels.length, expectedPixels - startPos);// 复制原有像素数据到中间位置
            System.arraycopy(pixels, 0, correctedPixels, startPos, copyLength);

            pixels = correctedPixels;
        }

        for (int y = 0; y < height; y++) {// 更新纹理像素（进行左右翻转）
            for (int x = width-1; x >= 0; x--) {//行
                int index = y * width + x;
                assert image != null;
                image.setPixelRGBA(Math.abs(x-255), y, pixels[index]);
            }
        }

        dynamicTexture.upload();

        updateCount++;
    }

    public ResourceLocation getTextureLocation() {
        return textureId;
    }

    public void clearTexture() {
        fillSilverGray();// 改为使用银灰色默认材质
        NativeImage image = dynamicTexture.getPixels();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assert image != null;
                image.setPixelRGBA(x, y, 0xFFFFFFFF);
            }
        }
        dynamicTexture.upload();
    }

    public void upload(){
        dynamicTexture.upload();
    }

    /**
     * 测试用途的一个彩色渐变动态材质
     */
    public void updateTexture() {
        NativeImage image = dynamicTexture.getPixels();
        int tick = (int) (mc.getFrameTime() * 10);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = (x + tick) % 256;
                int g = (y + tick) % 256;
                int b = (x * y + tick) % 256;
                int a = 255;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                assert image != null;
                image.setPixelRGBA(x, y, argb);
            }
        }
        dynamicTexture.upload();
    }

    private void saveCurrentFrameToFile() {
        try {
            String textureName = this.textureId.toString().replace(":", "_");
            NativeImage image = this.dynamicTexture.getPixels();
//            if (this.isAllBlackFast(image)) {
//                return;
//            }
            Path screenshotsDir = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(screenshotsDir);

            String filename = textureName + "_video_frame_" + System.currentTimeMillis() + ".png";
            Path outputPath = screenshotsDir.resolve(filename);

            image.writeToFile(outputPath);
            System.out.println("[VideoRenderer] Saved frame to: " + outputPath);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
//            System.out.println("在VideoScreenRenderer中已执行保存当前帧");
        }
    }
    public static int getViewScreenX() {
        return viewScreenX;
    }

    public void setViewScreenX(int viewScreenX) {
        ScreenDynamicTexture.viewScreenX = viewScreenX;
    }

    public static int getViewScreenY() {
        return viewScreenY;
    }

    public void setViewScreenY(int viewScreenY) {
        ScreenDynamicTexture.viewScreenY = viewScreenY;
    }
}