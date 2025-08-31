package com.myacghome.mcvan.util;

import com.myacghome.mcvan.ToolVan;
import java.io.IOException;

public class FFmpegChecker {
    public static boolean isFFmpegAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.out.println("FFmpeg 检查失败: {}"+ e.getMessage());
            return false;
        }
    }
}