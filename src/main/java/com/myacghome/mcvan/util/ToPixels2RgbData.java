package com.myacghome.mcvan.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ToPixels2RgbData {
    public static final byte [] nullRgbData = new byte[0];
    /**
     * 将16进制的像素转换为rgbData
     * @param pixels
     * @param resolutionX
     */
    public static byte[] PixelsToRgbData(int[] pixels, int resolutionX) {
        if (pixels == null || resolutionX <= 0) return nullRgbData;
        int resolutionY = (int) Math.ceil((double) pixels.length / resolutionX);// 计算分辨率 Y
        if (resolutionY <= 0) return nullRgbData;// 如果分辨率 Y 无效，返回 null
        int totalPixels = resolutionX * resolutionY;// 计算总像素数
        byte[] rgbData = new byte[totalPixels * 3]; // 每个像素占用 3 个字节（R, G, B）
        for (int i = 0, j = 0; i < totalPixels; i++) {
            int pixel = pixels[i]; // 获取当前像素值
            rgbData[j++] = (byte) ((pixel >> 16) & 0b11111111); // R 分量
            rgbData[j++] = (byte) ((pixel >> 8) & 0b11111111);  // G 分量
            rgbData[j++] = (byte) (pixel & 0b11111111);         // B 分量
        }
        return rgbData;
    }
    /**
     * 将16进制的像素转换为rgbData
     * @param pixel
     */
    @Contract(pure = true)
    public static byte @NotNull [] PixelToRgbData(int pixel) {
        return new byte[]{
                (byte) ((pixel >> 16) & 0b11111111), // R 分量
                (byte) ((pixel >> 8) & 0b11111111),  // G 分量
                (byte) (pixel & 0b11111111)          // B 分量
        };
    }
}
