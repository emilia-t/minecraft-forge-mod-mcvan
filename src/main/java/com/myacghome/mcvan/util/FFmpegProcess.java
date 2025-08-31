package com.myacghome.mcvan.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;


public class FFmpegProcess {
    private Process process;
    private Thread outputReader;
    private Thread errorReader;
    private int frameNumber = 0; // 添加帧序号计数器
    private int resolutionX;
    private int resolutionY;

    public static int[] getVideoResolution(String url) {

        String sanitizedUrl = url.contains(" ") ? "\"" + url + "\"" : url;// 通过URL获取视频分辨率// 确保URL被引号包裹


        String[] command = {// 构建命令数组（避免使用shell）
                "ffprobe",
                "-i", sanitizedUrl,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0"
        };

        System.out.println("[FFmpegProcess] getVideoResolution command: " + String.join(" ", command));

        try {
            Process process = new ProcessBuilder(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));


            StringBuilder errorOutput = new StringBuilder();// 读取错误输出
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
            }


            String line;// 读取标准输出
            int width = 0;
            int height = 0;
            while ((line = reader.readLine()) != null) {
                System.out.println("ffprobe output: " + line);
                String[] dimensions = line.split("x");
                if (dimensions.length == 2) {
                    try {
                        width = Integer.parseInt(dimensions[0].trim());
                        height = Integer.parseInt(dimensions[1].trim());
                        System.out.println("Resolution: " + width + "x" + height);
                        return new int[]{width, height};
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse resolution: " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || width == 0 || height == 0) {
                System.err.println("ffprobe failed. Exit code: " + exitCode);
                System.err.println("Error output: " + errorOutput.toString());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting video resolution: " + e.getMessage());
            e.printStackTrace();
        }

        return new int[]{0, 0};
    }


    public void start(String url, FrameHandle frameHandle, byte widthBlocks, byte heightBlocks ) {// 通过URL获取并解析视频
        try {
            System.out.println("[FFmpegProcess] Attempting to process video from: " + url);

            int[] resolutionXY = getVideoResolution(url);// 首先获取视频原始分辨率
            setResolutionX(resolutionXY[0]);
            setResolutionY(resolutionXY[1]);
            System.out.println("[FFmpegProcess] 获取到视频分辨率: " + resolutionXY[0] + "x" + resolutionXY[1]);

            int width = widthBlocks * VideoData.blockPixelDefault; // 确定内容分辨率和输出分辨率
            int height = heightBlocks * VideoData.blockPixelDefault;
            int contentWidth = width;
            int contentHeight = (int)Math.ceil((double) (contentWidth * resolutionXY[1]) / (double) resolutionXY[0]);
            String[] command = {
                    "ffmpeg",
                    "-i", url,
                    "-vsync", "0",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-pix_fmt", "rgb24",
                    "-vf", "scale="+contentWidth+":"+contentHeight+":force_original_aspect_ratio=decrease",//强制原始纵横比=减小
                    "-"
            };
            System.out.println("[FFmpegProcess]:");
            System.out.println(Arrays.toString(command));
            process = new ProcessBuilder(command).start();


            outputReader = new Thread(() -> {// 启动线程读取标准输出
                try {
                    int frameSize = contentWidth * contentHeight * 3;
                    byte[] frameBuffer = new byte[frameSize];
                    int offset = 0;
                    int read;

                    while ((read = process.getInputStream().read(frameBuffer, offset, frameSize - offset)) != -1) {
                        offset += read;
                        if (offset == frameSize) {
                            frameNumber++; // 递增帧序号

                            int frameNumberRGB = frameNumber;// 修改第一个像素为帧序号
                            frameBuffer[0] = (byte) (frameNumberRGB >> 16 & 0xFF); // R
                            frameBuffer[1] = (byte) (frameNumberRGB >> 8 & 0xFF);  // G
                            frameBuffer[2] = (byte) (frameNumberRGB & 0xFF);       // B
                            frameHandle.frameHandle(frameBuffer.clone(), frameNumber); // 传递帧序号
                            frameBuffer = new byte[frameSize]; // 创建新缓冲区
                            offset = 0;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("FFmpeg输出读取错误: " + e.getMessage());
                }
            });
            outputReader.start();


            errorReader = new Thread(() -> {// 启动线程读取错误输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("FFmpeg: " + line);
                    }
                } catch (IOException e) {
                    System.out.println("FFmpeg 错误流读取失败: " + e.getMessage());
                }
            });
            errorReader.start();

        }
        catch (IOException e) {
            System.out.println("启动 FFmpeg 失败: " + e.getMessage());
        }
    }


    public static int[] getVideoResolution(Path fileFullPath) {// 通过本地 .vd 文件获取视频分辨率
        String filePath = fileFullPath.toString();


        String[] command = {// 构建命令数组（避免使用shell）
                "ffprobe",
                "-i", filePath,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0"
        };

        System.out.println("[FFmpegProcess] getVideoResolution command: " + String.join(" ", command));

        try {
            Process process = new ProcessBuilder(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));


            StringBuilder errorOutput = new StringBuilder();// 读取错误输出
            String errorLine;
            while ((errorLine = errorReader.readLine()) != null) {
                errorOutput.append(errorLine).append("\n");
            }


            String line;
            int width = 0;
            int height = 0;
            while ((line = reader.readLine()) != null) {// 读取标准输出
                System.out.println("ffprobe output: " + line);
                String[] dimensions = line.split("x");
                if (dimensions.length == 2) {
                    try {
                        width = Integer.parseInt(dimensions[0].trim());
                        height = Integer.parseInt(dimensions[1].trim());
                        System.out.println("Resolution: " + width + "x" + height);
                        return new int[]{width, height};
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse resolution: " + line);
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 || width == 0 || height == 0) {
                System.err.println("ffprobe failed. Exit code: " + exitCode);
                System.err.println("Error output: " + errorOutput.toString());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error getting video resolution: " + e.getMessage());
            e.printStackTrace();
        }

        return new int[]{0, 0};
    }


    public void start(Path fileFullPath, FrameHandle frameHandle, byte widthBlocks, byte heightBlocks) {// 通过本地 .vd 文件解析视频
        try {
            String filePath = fileFullPath.toString();
            System.out.println("[FFmpegProcess] Attempting to process video from: " + filePath);

            int[] resolutionXY = getVideoResolution(fileFullPath);// 首先获取视频原始分辨率
            System.out.println("[FFmpegProcess] 获取到视频分辨率: " + resolutionXY[0] + "x" + resolutionXY[1]);

            int width = widthBlocks * VideoData.blockPixelDefault;// 确定内容分辨率和输出分辨率
            int height = heightBlocks * VideoData.blockPixelDefault;
            int contentWidth = width;
            int contentHeight = (int) Math.ceil((double) (contentWidth * resolutionXY[1]) / (double) resolutionXY[0]);
            String[] command = {
                    "ffmpeg",
                    "-i", filePath,
                    "-vsync", "0",
                    "-f", "image2pipe",
                    "-vcodec", "rawvideo",
                    "-pix_fmt", "rgb24",
                    "-vf", "scale=" + contentWidth + ":" + contentHeight + ":force_original_aspect_ratio=decrease",
                    "-"
            };
            System.out.println("[FFmpegProcess]:");
            System.out.println(Arrays.toString(command));
            process = new ProcessBuilder(command).start();


            outputReader = new Thread(() -> {// 启动线程读取标准输出
                try {
                    int frameSize = contentWidth * contentHeight * 3;
                    byte[] frameBuffer = new byte[frameSize];
                    int offset = 0;
                    int read;

                    while ((read = process.getInputStream().read(frameBuffer, offset, frameSize - offset)) != -1) {
                        offset += read;
                        if (offset == frameSize) {
                            frameNumber++;

                            int frameNumberRGB = frameNumber;// 修改第一个像素为帧序号
                            frameBuffer[0] = (byte) (frameNumberRGB >> 16 & 0xFF);
                            frameBuffer[1] = (byte) (frameNumberRGB >> 8 & 0xFF);
                            frameBuffer[2] = (byte) (frameNumberRGB & 0xFF);
                            frameHandle.frameHandle(frameBuffer.clone(), frameNumber);
                            frameBuffer = new byte[frameSize];
                            offset = 0;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("FFmpeg输出读取错误: " + e.getMessage());
                }
            });
            outputReader.start();


            errorReader = new Thread(() -> {// 启动线程读取错误输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("FFmpeg: " + line);
                    }
                } catch (IOException e) {
                    System.out.println("FFmpeg 错误流读取失败: " + e.getMessage());
                }
            });
            errorReader.start();

        } catch (IOException e) {
            System.out.println("启动 FFmpeg 失败: " + e.getMessage());
        }
    }

    public int getResolutionX() {
        return resolutionX;
    }

    public void setResolutionX(int resolutionX) {
        this.resolutionX = resolutionX;
    }

    public int getResolutionY() {
        return resolutionY;
    }

    public void setResolutionY(int resolutionY) {
        this.resolutionY = resolutionY;
    }

    // 视频帧处理接口
    public interface FrameHandle {
        void frameHandle(byte[] frameData, int frameNumber); // 修改接口以传递帧序号
    }
}