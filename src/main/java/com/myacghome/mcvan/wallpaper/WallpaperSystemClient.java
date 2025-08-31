package com.myacghome.mcvan.wallpaper;
import com.myacghome.mcvan.ToolVan;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonParseException;
/**
 * 实现动态壁纸的系统
 * 1.最大存储36个 16格 x 16格 的动态材质 每格 64px*64px
 * 2.每张壁纸的动态材质使用帧缓存技术加载帧,而不是全加载到内存中(单张壁纸最大缓存60帧)
 * 3.可以加载 .png .jpg .gif 图片
 * Wallpaper-----单张壁纸 a
 * Wallpapers----全部壁纸 6*6
 * FRAME---------单帧
 * FRAMES--------多帧(全帧)
 */
/* 本壁纸系统仅运行在客户端，并且在存档加载后(进入游戏)加载和启动 */
public class WallpaperSystemClient {
    public static final int MAX_WALLPAPERS_COUNT = 36; // 最大缓存纹理数
    public static final int MAX_WALLPAPER_WIDTH_BLOCKS = 16;
    public static final int MAX_WALLPAPER_HEIGHT_BLOCKS = 16;
    public static final int BLOCKS_WIDTH_PIXELS = 64;
    public static final int BLOCKS_HEIGHT_PIXELS = 64;
    public static final int MAX_WALLPAPER_FRAME_PIXELS = 1048576;
    public static final int MAX_WALLPAPERS_FRAME_PIXELS = 37748736;

    /* 使用并发安全的存储结构 */
    private static final Map<Integer, Wallpaper> wallpapersByID = new ConcurrentHashMap<>();
    private static final Map<String, Wallpaper> wallpapersByURL = new ConcurrentHashMap<>();
    private static final Map<Integer, List<Wallpaper>> frameRateGroups = new ConcurrentHashMap<>();//帧率分组调度
    private final Map<Integer, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public static final WallpaperSystemClient wallpaperSystemClient = new WallpaperSystemClient();
    public static WallpaperSystemClient getInstance(){
        return wallpaperSystemClient;
    }

    /* 私有属性 */
    private boolean ffmpegCheckerResult = true;
    private static ScheduledExecutorService scheduler;

    private WallpaperSystemClient() {
        if(FfmpegChecker.isFfmpegAvailable() == false){
            System.err.println("[WallpaperSystemClient] 警告! Ffmpeg 无法使用，请检查 Ffmpeg 是否正确安装，另外请在安装 Ffmpeg 后响应的更改系统环境变量！");
            ffmpegCheckerResult = false;
        }
        for (int wid = 0; wid < MAX_WALLPAPERS_COUNT; wid++) {// 初始化壁纸槽位
            wallpapersByID.put(wid, new Wallpaper(wid, Wallpaper.ReservedFieldURL));
        }
        scheduler = Executors.newSingleThreadScheduledExecutor();// 创建单线程调度器
    }

    /**
     * 检查壁纸插槽利用率
     */
    public int WallpapersUsageCount(){
        int count = 0;
        for (Wallpaper wallpaper : wallpapersByID.values()) {
            if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) continue;
            count++;
        }
        return count;
    }

    /**
     * 恢复指定槽位的壁纸播放
     */
    public boolean resumeWallpaperForWid(int wid) {
        if(!ffmpegCheckerResult){return false;}
        Optional<Wallpaper> optional = getWallpaper(wid);
        if (optional.isEmpty()) {
            return false;
        }
        Wallpaper wallpaper = optional.get();
        if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {
            return false; // 未使用槽位
        }
        wallpaper.resumeFrames();
        return true;
    }

    /**
     * 列出所有壁纸(wid + URL)
     */
    public ArrayList<String> WallpapersToString(){
        ArrayList<String> list = new ArrayList<>();
        wallpapersByID.forEach((id, wallpaper) -> {
            list.add("wid: "+id
                    +", URL: "+wallpaper.getURL()
                    +", X: "+wallpaper.getResolutionX()
                    +", Y: "+wallpaper.getResolutionY()
                    +", FPS: "+wallpaper.getFPS()
                    +", duration: "+wallpaper.getDuration()
                    +", frozenAtMs: "+wallpaper.getFrozenAtMs()
            );
        });
        return list;
    }

    /**
     * 壁纸分组方法
     */
    private void groupWallpapers() {
        frameRateGroups.clear();
        for (Wallpaper wallpaper : wallpapersByID.values()) {
            if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) continue;
            int fps = wallpaper.getFPS();
            frameRateGroups.computeIfAbsent(fps, k -> new ArrayList<>()).add(wallpaper);
        }
    }

    /**
     * 分组调度任务
     */
    public void startGroupScheduling() {
        scheduledTasks.values().forEach(task -> task.cancel(false));// 取消所有现有任务
        scheduledTasks.clear();// 取消所有现有任务

        for (Map.Entry<Integer, List<Wallpaper>> entry : frameRateGroups.entrySet()) {
            int fps = entry.getKey();
            long interval = 1000 / fps;
            ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
                for (Wallpaper wallpaper : entry.getValue()) {
                    wallpaper.updateFrame();
                }
            }, 0, interval, TimeUnit.MILLISECONDS);
            scheduledTasks.put(fps, future);
        }
    }

    /**
     * 通过URL获取壁纸
     */
    public Optional<Wallpaper> getWallpaper(String url) {
        if(!ffmpegCheckerResult){return Optional.empty();}
        return Optional.ofNullable(wallpapersByURL.get(url));
    }

    /**
     * 通过WID获取壁纸
     */
    public Optional<Wallpaper> getWallpaper(int wid) {
        if(!ffmpegCheckerResult){return Optional.empty();}
        if (wid < 0 || wid >= MAX_WALLPAPERS_COUNT) {// 验证WID是否有效
            return Optional.empty();
        }
        return Optional.ofNullable(wallpapersByID.get(wid));
    }

    /**
     * 通过URL添加壁纸
     * @return 添加成功返回true，无可用槽位返回false
     */
    public boolean setWallpaper(String url) {
        if(!ffmpegCheckerResult){return false;}
        for (int wid = 0; wid < MAX_WALLPAPERS_COUNT; wid++) {
            Wallpaper wallpaper = wallpapersByID.get(wid);
            if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {// 检查槽位是否空闲
                wallpaper.setURL(url);// 更新壁纸URL
                wallpaper.loadFrames();// 加载壁纸帧
                wallpapersByURL.put(url, wallpaper);
                groupWallpapers(); // 重新分组
                startGroupScheduling();
                return true;
            }
        }
        return false;
    }

    /**
     * 替换指定槽位的壁纸
     */
    public boolean replaceWallpaper(int wid, String newUrl) {
        if(!ffmpegCheckerResult){return false;}
        if (wid < 0 || wid >= MAX_WALLPAPERS_COUNT) {
            return false;
        }

        Wallpaper wallpaper = wallpapersByID.get(wid);

        if (!wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {// 移除旧URL映射
            wallpaper.unloadFrames();// 释放旧资源
            wallpaper.resetVideoMeta();// 重置VideoMeta
            wallpapersByURL.remove(wallpaper.getURL());
        }

        wallpaper.setURL(newUrl);// 设置新URL
        wallpaper.loadFrames();// 加载壁纸帧
        wallpapersByURL.put(newUrl, wallpaper);
        groupWallpapers(); // 重新分组
        startGroupScheduling();

        return true;
    }

    /**
     * 移除指定槽位的壁纸
     */
    public boolean removeWallpaper(int wid) {
        if(!ffmpegCheckerResult){return false;}
        if (wid < 0 || wid >= MAX_WALLPAPERS_COUNT) {
            return false;
        }

        Wallpaper wallpaper = wallpapersByID.get(wid);

        if (!wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {// 只有已使用的槽位才能移除
            wallpapersByURL.remove(wallpaper.getURL());
            wallpaper.unloadFrames();// 释放旧资源
            wallpaper.resetVideoMeta();// 重置VideoMeta
            wallpaper.setURL(Wallpaper.ReservedFieldURL);//重置为默认的值
            return true;
        }

        return false;
    }

    public boolean freezeWallpaperForWid(int wid){
        if(!ffmpegCheckerResult){return false;}
        Optional<Wallpaper> optional = getWallpaper(wid);
        if (optional.isEmpty()) {
            return false;
        }
        Wallpaper wallpaper = optional.get();
        if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {
            return false; // 未使用槽位
        }
        wallpaper.freezeFrames();
        return true;
    }

    /**
     * 通过wid找到壁纸并随机保存该壁纸的某一帧到游戏截图目录下
     * 如果保存成功则返回true否则返回false
     * @param wid
     * @return
     */
    /**
     * 通过wid找到壁纸并随机保存该壁纸的某一帧到游戏截图目录下
     * 如果保存成功则返回true否则返回false
     */
    public boolean saveRandomFrameForWid(int wid) {
        if(!ffmpegCheckerResult){return false;}
        Optional<Wallpaper> optional = getWallpaper(wid);
        if (optional.isEmpty()) {
            return false;
        }

        Wallpaper wallpaper = optional.get();
        if (wallpaper.getURL().equals(Wallpaper.ReservedFieldURL)) {
            return false; // 未使用槽位
        }

        Optional<NativeImage> frameOpt = wallpaper.getRandomFrame();
        if (frameOpt.isEmpty()) {
            return false;
        }

        NativeImage image = frameOpt.get();
        try {
            // 获取截图目录
            Path screenshotDir = Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots").resolve("wallpaper_frames");
            if (!screenshotDir.toFile().exists()) {
                screenshotDir.toFile().mkdirs();
            }

            // 生成文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            String fileName = "wallpaper_" + wid + "_" + timeStamp + ".png";
            File outputFile = screenshotDir.resolve(fileName).toFile();

            // 保存图片
            image.writeToFile(outputFile);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void todoInitTexture(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().execute(() -> {
            for (Wallpaper wallpaper : wallpapersByID.values()) {
                wallpaper.initTexture();
            }
        });
    }
}

class Wallpaper {
    public static final Pattern pattern2 = Pattern.compile("^[^/\\\\]*$");
    public static final String ReservedFieldURL = "http://localhost/unknown";
    public static final int MaxContentPixels = 1048576;
    public static final int MaxContentXPx = 1024;
    public static final int MaxContentYPx = 1024;
    public static final int PixelsBlock = 64; // Unit resolution on a block
    public static final int BorderTopPx = 8;
    public static final int BorderRightPx = 8;
    public static final int BorderBottomPx = 8;
    public static final int BorderLeftPx = 8;

    /* 私有属性 */
    private int WID = -1; // 壁纸ID不可变
    private String URL = "http://localhost/unknown"; // 主要索引
    private String name = "unnamed"; // 壁纸名称
    private int wallpaperContentX = 0;// 壁纸的内容 X 宽度
    private int wallpaperContentY = 0;// 壁纸的内容 Y 高度
    private boolean isDefaultTextureActive = true;// 默认材质标记 当没有帧图像时
    private boolean isPause = true;//是否处于暂停状态
    private volatile long frozenAtMs = 0;

    private final BlockingQueue<NativeImage> frameQueue = new ArrayBlockingQueue<>(60); // 帧缓存队列
    private NativeImage currentFrame; // 当前显示的帧
    private volatile boolean isFrameLoading = false; // 表示帧加载状态并限制重复加载下一帧
    private FfmpegProcess ffmpegProcess; // 用于加载帧的Ffmpeg进程
    private volatile boolean shouldLoop = true; // 循环播放标志

    /* VideoMeta */
    private int resolutionX;
    private int resolutionY;
    private byte FPS;//帧率不可设置为浮点数，也不支持动态帧率视频，最大为60，最小为1。常见的视频帧率如下：24(电影),30(电视),60(高刷)。
    private float duration;

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

    /* 动态材质相关 */
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureId;

    /**
     * 此类可以创建一张壁纸，注意URL参数不仅仅可以是网络视频资源的超链接还可以是本地视频资源的文件完整路径地址
     */
    public Wallpaper(int WID, String URL) {
        this.WID = WID;
        this.URL = URL;
        this.textureId = null; // 延迟注册
        this.dynamicTexture = null;
    }

    public void initTexture() {
        if (dynamicTexture == null) {
            Minecraft.getInstance().execute(() -> {
                this.dynamicTexture = new DynamicTexture(MaxContentXPx, MaxContentYPx, false);
                this.textureId = Minecraft.getInstance().getTextureManager()
                        .register(WID + "_about_custom_wallpapers", dynamicTexture);
                fillSilverGray();
            });
        }
    }

    public ResourceLocation getTextureId() {
        return textureId;
    }

    public boolean isDefaultTextureActive() {
        return isDefaultTextureActive;
    }

    public int getWID() {
        return WID;
    }

    public boolean getIsPause(){
        return isPause;
    }
    public void Pause(){//暂停
        this.isPause = true;
    }
    public void noPause(){//取消暂停
        this.isPause = false;
    }

    public byte getFPS() {
        return FPS;
    }
    private boolean setFPS(byte FPS) {
        if(FPS < 1 || FPS > 60){
            return false;
        }else{
            this.FPS = FPS;
            return true;
        }
    }
    private void resetFPS() {
        this.FPS = (byte)0;
    }

    public float getDuration(){
        return this.duration;
    }
    private boolean setDuration(float duration){
        if(duration>0){
            this.duration=duration;
            return true;
        }
        return false;
    }
    private void resetDuration(){
        this.duration=(float)0;
    }

    public String getURL() {
        return URL;
    }
    public boolean setURL(String url) {
        this.URL=url;
        return true;
    }

    public String getName() {
        return name;
    }
    public boolean setName(String name) {
        if(ToolVan.containsRegex(name,pattern2)){
            return false;
        }else{
            this.name = name;
            return true;
        }
    }

    public int getWallpaperContentY() {
        return wallpaperContentY;
    }
    public void setWallpaperContentY(int wallpaperContentY) {
        this.wallpaperContentY = wallpaperContentY;
    }

    public int getWallpaperContentX() {
        return wallpaperContentX;
    }
    public void setWallpaperContentX(int wallpaperContentX) {
        this.wallpaperContentX = wallpaperContentX;
    }

    public void setLooping(boolean looping) {
        this.shouldLoop = looping;
    }

    /**
     * 恢复播放冻结的壁纸
     */
    public void resumeFrames() {
        if (frozenAtMs <= 0) {
            return; // 没有有效的冻结时间点
        }
        VideoMeta videoMeta = FfmpegProcess.getVideoMeta(this.URL);
        int srcW = videoMeta.resolutionX();
        int srcH = videoMeta.resolutionY();
        // 过滤异常或极小分辨率
        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        // 计算缩放比例（等比缩放）
        double scale = Math.min(
                (double) MaxContentXPx / srcW,
                (double) MaxContentYPx / srcH
        );

        // 如果原图小于最大限制，则不缩放（scale ≥ 1）
        scale = Math.min(scale, 1.0);

        int targetW = (int) Math.ceil(srcW * scale);
        int targetH = (int) Math.ceil(srcH * scale);

        // 确保至少为 1×1
        targetW = Math.max(targetW, 1);
        targetH = Math.max(targetH, 1);

        // 保存冻结时间点
        long resumeAtMs = frozenAtMs;
        frozenAtMs = 0; // 重置冻结时间

        // 重新启动进程，从冻结的时间点开始
        startFrameLoading(targetW, targetH, resumeAtMs);
        noPause(); // 取消暂停状态
    }

    /**
     * 添加帧到缓存队列
     **/
    public boolean addFrameToQueue(NativeImage frame) {
        if (frameQueue.size() >= 60) {
            NativeImage oldestFrame = frameQueue.poll();
            if (oldestFrame != null) {
                oldestFrame.close(); // 释放最旧帧的内存
            }
        }
        return frameQueue.offer(frame);
    }

    /**
     * 获取当前帧
     **/
    public Optional<NativeImage> getCurrentFrame() {
        return Optional.ofNullable(currentFrame);
    }

    /**
     * 获取随机一帧
     **/
    public Optional<NativeImage> getRandomFrame() {
        if (frameQueue.isEmpty()) {
            System.out.println("[Wallpaper] 当前帧队列为空，无法保存截图。");
            return Optional.empty();
        }

        // 随机取一帧
        NativeImage[] frames = frameQueue.toArray(new NativeImage[0]);
        NativeImage image = frames[new Random().nextInt(frames.length)];
        if (image == null) {
            System.out.println("[Wallpaper] 随机帧为 null。");
            return Optional.empty();
        }
        return Optional.of(image);
    }

    /**
     * 启动帧加载进程（支持从指定时间开始）
     */
    public void startFrameLoading(int targetWidthPx, int targetHeightPx, long startMs) {
        stopFrameLoading(); // 确保之前的进程已停止

        ffmpegProcess = new FfmpegProcess();

        ffmpegProcess.setOnExit(() -> {
            if (shouldLoop && !isPause) {
                System.out.println("[Wallpaper] 视频播放结束，自动从头播放");
                startFrameLoading(targetWidthPx, targetHeightPx, 0); // 从头开始
            }
        });

        ffmpegProcess.start(this.URL, new FrameHandle() {
                    @Override
                    public void frameHandle(byte[] frameData, int frameNumber, VideoMeta videoMeta) {
                        int expected = targetWidthPx * targetHeightPx * 4;
                        if (frameData.length != expected) {
                            System.err.println("[FfmpegProcess] Frame size mismatch: expected=" + expected + ", actual=" + frameData.length);
                            return;
                        }

                        NativeImage image = new NativeImage(NativeImage.Format.RGBA, targetWidthPx, targetHeightPx, false);
                        ByteBuffer buffer = ByteBuffer.wrap(frameData);
                        buffer.order(ByteOrder.LITTLE_ENDIAN);

                        for (int y = 0; y < targetHeightPx; y++) {
                            for (int x = 0; x < targetWidthPx; x++) {
                                int r = buffer.get() & 0xFF;
                                int g = buffer.get() & 0xFF;
                                int b = buffer.get() & 0xFF;
                                int a = buffer.get() & 0xFF;
                                image.setPixelRGBA(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                            }
                        }

                        if(duration == 0 || FPS == 0){//初始化视频壁纸的视频属性
                            setResolutionX(videoMeta.resolutionX());
                            setResolutionY(videoMeta.resolutionY());
                            setFPS(videoMeta.FPS());
                            setDuration(videoMeta.duration());
                        }

                        addFrameToQueue(image);
                    }
                },
                targetWidthPx,
                targetHeightPx,
                startMs); // 传递开始时间
    }

    /**
     * 启动帧加载进程
     **/
    public void startFrameLoading(int targetWidthPx, int targetHeightPx) {
        startFrameLoading(targetWidthPx, targetHeightPx, 0);
    }

    /**
     * 停止帧加载
     **/
    public void stopFrameLoading() {
        if (ffmpegProcess != null) {
            ffmpegProcess.stop();
            ffmpegProcess = null;
        }

        frameQueue.forEach(NativeImage::close);// 释放帧缓存放队列中的所有帧
        frameQueue.clear();

        if (currentFrame != null) {
            currentFrame.close();
            currentFrame = null;
        }
    }

    /**
     * 关闭ffmpegProcess
     **/
    public void killFfmpegProcess() {
        if (ffmpegProcess != null) {
            ffmpegProcess.stop();
            ffmpegProcess = null;
        }
    }

    /**
     * 如果源视频宽度分辨率(x轴)大于MaxContentXPx
     * 或者源视频高度分辨率(y轴)大于MaxContentYPx
     * 则进行等比例缩小
     */
    public void loadFrames() {
        VideoMeta videoMeta = FfmpegProcess.getVideoMeta(this.URL);
        int srcW = videoMeta.resolutionX();
        int srcH = videoMeta.resolutionY();

        // 过滤异常或极小分辨率
        if (srcW <= 0 || srcH <= 0) {
            return;
        }

        // 计算缩放比例（等比缩放）
        double scale = Math.min(
                (double) MaxContentXPx / srcW,
                (double) MaxContentYPx / srcH
        );

        // 如果原图小于最大限制，则不缩放（scale ≥ 1）
        scale = Math.min(scale, 1.0);

        int targetW = (int) Math.ceil(srcW * scale);
        int targetH = (int) Math.ceil(srcH * scale);

        // 确保至少为 1×1
        targetW = Math.max(targetW, 1);
        targetH = Math.max(targetH, 1);

        startFrameLoading(targetW, targetH);
        noPause();
    }

    /* 重置时使用 */
    public void unloadFrames() {
        stopFrameLoading();
        Pause();
        fillSilverGray(); // 恢复默认纹理
    }

    /* 重置时使用 */
    public void resetVideoMeta() {
        setResolutionX(0);
        setResolutionY(0);
        resetFPS();
        resetDuration();
    }

    /* 冻结时使用 */
    public void freezeFrames() {
        frozenAtMs = (ffmpegProcess == null) ? 0 : ffmpegProcess.getCurrentMs();
        killFfmpegProcess();
        Pause();
    }
    public long getFrozenAtMs() {
        return frozenAtMs;
    }
    /**
     * 更新帧
     */
    public void updateFrame() {
        if (isPause) return; // 暂停阶段不做任何更新

        if (isFrameLoading || frameQueue.isEmpty()) return; // 帧未加载完毕或者帧缓存队列为空不做任何更新

        isFrameLoading = true;

        NativeImage nextFrame = frameQueue.poll();
        if (nextFrame != null) {
            if (currentFrame != null) {
                currentFrame.close(); // 释放前一帧内存
            }
            currentFrame = nextFrame; // 拿取下一帧
        }

        if (currentFrame != null) { // 更新纹理材质
            Minecraft.getInstance().execute(() -> {
                NativeImage texture = dynamicTexture.getPixels();
                if(currentFrame != null){
                    for (int y = 0; y < currentFrame.getHeight(); y++) {
                        for (int x = 0; x < currentFrame.getWidth(); x++) {
                            if (texture != null) {
                                texture.setPixelRGBA(x, y, currentFrame.getPixelRGBA(x, y));
                            }
                        }
                    }
                    dynamicTexture.upload();
                }
            });
        }

        isFrameLoading = false;
    }

    /**
     * 填充银灰色默认材质（灰色凝灰岩）
     */
    public void fillSilverGray() {
        if(!isPause){
            return;//必须先暂停才能设置为灰屏
        }
        NativeImage image = dynamicTexture.getPixels();
        if (image != null) {
            for (int y = 0; y < MaxContentXPx; y++) {// 创建纯银灰色背景
                for (int x = 0; x < MaxContentYPx; x++) {
                    image.setPixelRGBA(x, y, 0xFFC0C0C0);// 银灰色：RGB(192, 192, 192)
                }
            }
            for (int y = 0; y < MaxContentXPx; y++) {// 添加凝灰岩纹理特征（简单的噪声模式）
                for (int x = 0; x < MaxContentYPx; x++) {
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
            setWallpaperContentX(MaxContentXPx);// 设置视图尺寸
            setWallpaperContentY(MaxContentYPx);// 设置视图尺寸
            isDefaultTextureActive = true;
        }
    }
}

class FfmpegChecker {
    public static boolean isFfmpegAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            System.out.println("[FfmpegChecker] Ffmpeg 未通过检查: {}"+ e.getMessage());
            return false;
        }
    }
}

class FfmpegProcess {
    private Process process;
    private Thread outputReader;
    private Thread errorReader;
    private int frameNumber = 0; // 添加帧序号计数器
    private int resolutionX;
    private int resolutionY;
    private Runnable onExit;
    private final AtomicLong currentPtsMs = new AtomicLong(0);   // 单位毫秒

    public static VideoMeta getVideoMeta(String url) {
        String sanitizedUrl = url.contains(" ") ? "\"" + url + "\"" : url;

        try {
            String[] command = {
                    "ffprobe",
                    "-i", sanitizedUrl,
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries",
                    "stream=width,height,r_frame_rate:stream_tags=duration:stream=duration",
                    "-of", "csv"
            };

            Process process = new ProcessBuilder(command).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            System.out.println("[FfmpegProcess]VideoMeta:"+line);
            if (line != null) {
                String[] parts = line.split(",");//length == 5

                int width = Integer.parseInt(parts[1].trim());
                int height = Integer.parseInt(parts[2].trim());

                String fpsStr = parts[3].trim();// 帧率：parts[2]
                double fps = 0.0;
                if (fpsStr.contains("/")) {
                    String[] fpsParts = fpsStr.split("/");
                    fps = Double.parseDouble(fpsParts[0]) / Double.parseDouble(fpsParts[1]);
                } else {
                    fps = Double.parseDouble(fpsStr);
                }

                float durationFloat = Float.parseFloat(parts[4].trim());

                return new VideoMeta(width, height, (byte)fps, durationFloat, url);
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("[FfmpegProcess] Error getting video meta: " + e.getMessage());
            e.printStackTrace();
        }

        return new VideoMeta(0, 0, (byte)0, 0, url);
    }

    public void start(String url, FrameHandle frameHandle, int targetWidthPx, int targetHeightPx, long startMs) {// 通过URL获取并解析视频
        try {
            System.out.println("[FfmpegProcess] Attempting to process video from: " + url + " at " + startMs + "ms");

            String sanitizedUrl = url.contains(" ") ? "\"" + url + "\"" : url;
            VideoMeta videoMeta = getVideoMeta(url);// 首先获取视频原始分辨率
            setResolutionX(videoMeta.resolutionX());
            setResolutionY(videoMeta.resolutionY());

            long seconds = startMs / 1000;// 构建时间参数 (将毫秒转换为HH:MM:SS.ms格式)
            long msPart = startMs % 1000;
            String timeArg = String.format("%02d:%02d:%02d.%03d",seconds / 3600, (seconds % 3600) / 60, seconds % 60, msPart);

            System.out.println("[FfmpegProcess] 获取到视频分辨率: " + videoMeta.resolutionX() + "x" + videoMeta.resolutionY());

            List<String> command = new ArrayList<>();
            command.add("ffmpeg");
            command.add("-ss");
            command.add(timeArg);  // 添加开始时间参数
            command.add("-re");
            command.add("-i");
            command.add(sanitizedUrl);
            command.add("-f");
            command.add("image2pipe");
            command.add("-vcodec");
            command.add("rawvideo");
            command.add("-pix_fmt");
            command.add("rgba");
            command.add("-vf");
            command.add("scale=" + targetWidthPx + ":" + targetHeightPx + ":force_original_aspect_ratio=decrease");
            command.add("-");

            process = new ProcessBuilder(command).start();

            outputReader = new Thread(() -> {// 启动线程读取标准输出
                try {
                    int frameSize = targetWidthPx * targetHeightPx * 4;
                    byte[] frameBuffer = new byte[frameSize];
                    int offset = 0;
                    int read;

                    while(
                        !Thread.currentThread().isInterrupted() &&
                        (read = process.getInputStream().read(frameBuffer, offset, frameSize - offset)) != -1
                    ){
                        offset += read;
                        if (offset == frameSize) {
                            frameNumber++;
                            frameHandle.frameHandle(Arrays.copyOf(frameBuffer, frameSize),frameNumber,videoMeta);
                            offset = 0;
                            Thread.sleep(10);// 添加暂停以控制帧率 // 减少CPU占用
                        }
                    }
                } catch (IOException e) {
                    System.out.println("[FfmpegProcess] Ffmpeg输出读取错误: " + e.getMessage());
                } catch (InterruptedException e){
                    System.out.println("[FfmpegProcess] Ffmpeg线程状态异常: " + e.getMessage());
                } finally {
                    if (onExit != null) {// 进程结束时触发回调
                        onExit.run();
                    }
                }
            });
            outputReader.start();

            errorReader = new Thread(() -> {
                try (BufferedReader br =
                             new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int idx = line.indexOf("time=");// 例：time=00:01:23.45
                        if (idx >= 0) {
                            String t = line.substring(idx + 5, idx + 5 + 11); // "00:01:23.45"
                            try {
                                String[] parts = t.split(":");
                                double sec = Double.parseDouble(parts[2]);
                                long ms = (long)(Integer.parseInt(parts[0]) * 3600_000L +
                                        Integer.parseInt(parts[1]) * 60_000L +
                                        sec * 1000);
                                currentPtsMs.set(ms);
                            } catch (Exception ignore) {}
                        }
                    }
                } catch (IOException ignored) {}
            });
            errorReader.start();

        }
        catch (IOException e) {
            System.out.println("[FfmpegProcess] 启动 Ffmpeg 失败: " + e.getMessage());
        }
    }

    public void start(String url, FrameHandle frameHandle, int targetWidthPx, int targetHeightPx) {
        start(url, frameHandle, targetWidthPx, targetHeightPx, 0);
    }

    public long getCurrentMs() {
        return currentPtsMs.get();
    }

    public void setOnExit(Runnable onExit) {
        this.onExit = onExit;
    }

    public int getResolutionX() {
        return resolutionX;
    }

    private void setResolutionX(int resolutionX) {
        this.resolutionX = resolutionX;
    }

    public int getResolutionY() {
        return resolutionY;
    }

    private void setResolutionY(int resolutionY) {
        this.resolutionY = resolutionY;
    }

    public void stop() {
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (outputReader != null && outputReader.isAlive()) {
            outputReader.interrupt();
        }
        if (errorReader != null && errorReader.isAlive()) {
            errorReader.interrupt();
        }
    }
}

interface FrameHandle {
    void frameHandle(byte[] frameData, int frameNumber, VideoMeta meta); // 修改接口以传递帧序号
}

/**
 * @param duration 秒
 * @param url      方便调试
 */
record VideoMeta(
        int resolutionX,
        int resolutionY,
        byte FPS,
        float duration,
        String url
){}