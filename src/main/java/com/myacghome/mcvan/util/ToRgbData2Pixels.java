package com.myacghome.mcvan.util;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class ToRgbData2Pixels {
    public static final int [] nullPixel = new int[0];
    /**
     * 将原始的rgb数据转换为16进制的帧图像
     * @param rgbData
     * @param resolutionX
     */
    @Contract(pure = true)
    public static int[] ToRgbData2Pixels(byte @NotNull [] rgbData , int resolutionX) {
            if(rgbData.length%3!=0)return nullPixel;//rgbData一定是 3 的整数倍
            int resolutionY = (int)Math.ceil((double)rgbData.length/((double)resolutionX * 3.0d));
            final int totalPixels = resolutionX * resolutionY;
            int[] pixels = new int[totalPixels];
            for (int i = 0, j = 0; i < totalPixels; i++) {
                pixels[i] = 0b11111111000000000000000000000000
                        | ((rgbData[j++] & 0b11111111) << 16)
                        | ((rgbData[j++] & 0b11111111) << 8)
                        | (rgbData[j++] & 0b11111111);
            }
            return pixels;
    }
    @Contract(pure = true)
    public static int RgbDataToPixel(byte rData, byte gData, byte bData) {
        return ((rData & 0b11111111) << 16) // R 分量，左移 16 位
                | ((gData & 0b11111111) << 8)  // G 分量，左移 8 位
                | (bData & 0b11111111);        // B 分量
    }
}
