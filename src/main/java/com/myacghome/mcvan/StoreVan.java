package com.myacghome.mcvan;

import com.myacghome.mcvan.block.VideoBlockUpdateVideoChunkPacket;
import com.myacghome.mcvan.block.VideoBlockUpdateVideoPacket;
import com.myacghome.mcvan.network.NetworkHandler;
import com.myacghome.mcvan.util.*;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.lang.ref.WeakReference;
import java.nio.file.Path;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import static com.myacghome.mcvan.IndexMod.store;

/**CLASS
 * 线程安全的状态管理容器，支持属性订阅/发布模式
 *
 * 特性：
 * 1. 线程安全的单例实现
 * 2. 值变化检查避免无效通知
 * 3. 异步通知机制避免阻塞
 * 4. 弱引用订阅避免内存泄漏
 * 5. 泛型支持增强类型安全
 */
public class StoreVan {
    private static final            StoreVan INSTANCE = new StoreVan();// 单例实例（枚举方式实现绝对线程安全）
    private final                   ConcurrentHashMap<String, Object> state;// 状态存储（线程安全Map）
    private final                   ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<Object>>> subscribers;// 订阅者列表（线程安全且适合读多写少场景）
    private final                   ConcurrentHashMap<String, CopyOnWriteArrayList<WeakReference<Consumer<Object>>>> weakSubscribers;// 弱引用订阅者列表
    private Executor                notificationExecutor;// 异步通知用的线程池（可自定义）
    private final ConcurrentHashMap<String, VideoData> videoDataStore = new ConcurrentHashMap<>();// 视频名称和视频数据
    private final VideoDataNull videoDataNull = new VideoDataNull();
    private Path storeVanDir;
    private FFmpegProcess ffmpegProcess;//外部FFmpeg进程
    /**本地数据与自动保存**/

    private StoreVan() {
        state = new ConcurrentHashMap<>();
        subscribers = new ConcurrentHashMap<>();
        weakSubscribers = new ConcurrentHashMap<>();
        notificationExecutor = ForkJoinPool.commonPool(); // 默认使用公共线程池
    }

    /**
     * 获取单例实例
     */
    public static StoreVan getInstance() {
        return INSTANCE;
    }

    /**
     * 设置自定义通知线程池（可选）
     */
    public void setNotificationExecutor(Executor executor) {
        this.notificationExecutor = Objects.requireNonNull(executor);
    }

    /**
     * 获取状态值（泛型方法自动转换类型）
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) state.get(key);
    }

    /**
     * 设置状态值（只有值真正变化时才通知订阅者）
     */
    public void set(String key, Object value) {
        Object oldValue = state.put(key, value);
        if (!Objects.equals(oldValue, value)) {
            notifySubscribers(key, value);
        }
    }

    /**
     * 订阅状态变化（强引用，需手动取消订阅）
     */
    public void subscribe(String key, Consumer<Object> callback) {
        subscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                .add(callback);
    }

    /**
     * 订阅状态变化（弱引用，自动回收无需取消订阅）
     */
    public void subscribeWeak(String key, Consumer<Object> callback) {
        weakSubscribers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                .add(new WeakReference<>(callback));
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String key, Consumer<Object> callback) {
        CopyOnWriteArrayList<Consumer<Object>> list = subscribers.get(key);
        if (list != null) {
            list.remove(callback);
        }
    }

    /**
     * 通知所有订阅者（异步执行）
     */
    private void notifySubscribers(String key, Object value) {
        notificationExecutor.execute(() -> {
            CopyOnWriteArrayList<Consumer<Object>> strongSubs = subscribers.get(key);// 通知强引用订阅者
            if (strongSubs != null) {
                strongSubs.forEach(callback -> {
                    try {
                        callback.accept(value);
                    } catch (Exception e) {
                        System.err.println("Subscriber callback failed: " + e.getMessage());// 防止个别回调异常影响其他订阅者
                    }
                });
            }

            CopyOnWriteArrayList<WeakReference<Consumer<Object>>> weakSubs = weakSubscribers.get(key);// 通知弱引用订阅者并清理已回收的引用
            if (weakSubs != null) {
                weakSubs.removeIf(ref -> {
                    Consumer<Object> callback = ref.get();
                    if (callback != null) {
                        try {
                            callback.accept(value);
                        } catch (Exception e) {
                            System.err.println("Weak subscriber callback failed: " + e.getMessage());
                        }
                        return false;
                    }
                    return true; // 引用已被GC，移除该条目
                });
            }
        });
    }


    /**=====================VideoScreenBlock====================**/

    /**
     * 获取视频数据，如果存在则返回视频数据，如果不存在则返回空视频数据(VideoDataNull)
     * @param url
     * @return
     */
    public VideoData getVideoData(String url){
        String name = ToolVan.extractResourceName(url);
        if(videoDataStore.get(name) == null){
            return this.videoDataNull;
        }
        else{
            return videoDataStore.get(name);
        }
    }

    /**
     * 添加一个视频数据，如果已经存在则无法添加返回false，反之true
     * @param url
     * @param videoData
     * @return
     */
    public boolean setVideoData(String url, VideoData videoData){
        boolean isChange;
        String name = ToolVan.extractResourceName(url);
        if(videoDataStore.get(name) != null){
            isChange = false;
        }
        else{
            videoDataStore.put(name,videoData);
            isChange = true;
        }
        return isChange;
    }

    /**
     * 删除一个视频数据，如果不存在则返回false，否则执行删除并返回true
     * @param url
     * @return
     */
    public boolean delVideoData(String url){
        boolean isChange;
        String name = ToolVan.extractResourceName(url);
        if(videoDataStore.get(name) != null){
            videoDataStore.remove(name);
            isChange = true;
        }
        else{
            isChange = false;
        }
        return isChange;
    }
    public ConcurrentHashMap<String, VideoData> getAllVideoData(){
        return videoDataStore;
    }
    /**=====================VideoScreenBlock end================**/


    /**===================== 本地数据相关方法 =====================**/
    /**
     * 初始化创建storeVan文件夹
     * @param worldPath 游戏存档路径
     */
    public void initLocalData(Path worldPath) {
        storeVanDir = worldPath.resolve("storeVan");
        try {
            Files.createDirectories(storeVanDir);
            System.out.println("[StoreVan]创建storeVan目录: " + storeVanDir);
            loadFromLocalData(); // 加载本地数据
        } catch (IOException e) {
            System.err.println("[StoreVan]创建storeVan目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从本地存储加载所有.vdt文件
     */
    private void loadFromLocalData() {
        if (storeVanDir == null || !Files.exists(storeVanDir)) {
            System.err.println("loadFromLocalData: storeVan目录不存在");
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storeVanDir, "*.vd")) {
            for (Path file : stream) {
                if (Files.isRegularFile(file)) { // 确保是普通文件
                    String fileName = file.getFileName().toString();
                    System.out.println("[StoreVan]发现视频文件: " + fileName);
                    try {

                        String[] properties = ToVideoProperty2FileName.FileName2VideoProperty(fileName);// 解析文件名获取视频属性

                        if (properties.length != 5) {// 验证解析结果
                            System.err.println("[StoreVan]文件属性数量错误，跳过: " + fileName);
                            continue;
                        }
                        System.out.println("[StoreVan]文件属性:"+Arrays.toString(properties));

                        int resolutionX = Integer.parseInt(properties[0]);// 提取视频属性
                        int resolutionY = Integer.parseInt(properties[1]);
                        String url = properties[2];
                        byte widthBlocks = Byte.parseByte(properties[3]);
                        byte heightBlocks = Byte.parseByte(properties[4]);
                        int viewResolutionX = widthBlocks*VideoData.blockPixelDefault;

                        ffmpegProcess = new FFmpegProcess();// 创建视频数据对象
                        ffmpegProcess.start(
                                file,
                                (FFmpegProcess.FrameHandle) (frameData, frameNumber)->{

                                    int maxChunkSize = 18432; // 18432 pixel 分块
                                    int[] tempPixels = ToRgbData2Pixels.ToRgbData2Pixels(frameData, viewResolutionX);
                                    int totalSize = tempPixels.length;

                                    for (int offset = 0; offset < totalSize; offset += maxChunkSize) {
                                        // 发送分块数据包
                                        NetworkHandler.INSTANCE.send(
                                                PacketDistributor.ALL.noArg(),
                                                new VideoBlockUpdateVideoChunkPacket(
                                                        Arrays.copyOfRange(tempPixels,offset, Math.min(offset + maxChunkSize, totalSize)),
                                                        url,
                                                        resolutionX,
                                                        resolutionY,
                                                        frameNumber,
                                                        offset,
                                                        totalSize
                                                )
                                        );
                                    }

                                    if(tempPixels.length <= 1){
                                        return;
                                    }
                                    else{

                                        VideoData isExists = store.getVideoData(url); // 将帧数据保存在服务端的storeVan内以备用
                                        if (isExists instanceof VideoDataNull) { // 视频数据还未创建
                                            VideoData videoData = new VideoData(url, viewResolutionX, VideoData.resolutionYDefault);
                                            store.setVideoData(url, videoData);
                                        } else { // 视频数据已创建
                                            store.getVideoData(url).addFrame(tempPixels);
                                        }
                                        //System.out.println("[服务端InstructPacket]已发送视频帧数据包");
                                    }
                                },
                                widthBlocks,
                                heightBlocks
                        );

                        System.out.println("[StoreVan]成功加载视频: " + url +
                                " | 分辨率: " + resolutionX + "x" + resolutionY +
                                " | 方块: " + widthBlocks + "x" + heightBlocks);

                    }
                    catch (Exception e) {
                        System.err.println("解析文件 " + fileName + " 失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("遍历storeVan目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**===================== 本地数据相关方法end =====================**/

    /**===================== 序列化工具方法，现已改为在拉取视频阶段就会备份这个视频源文件，后续直接使用ffmpeg加载此文件 =====================**/
    /* 序列化VideoData对象 */
    private byte[] serializeVideoData(VideoData videoData) {
        List<int[]> frames = videoData.getFrames();
        String url = videoData.getUrl();
        int resolutionX = videoData.getResolutionX();
        int resolutionY = videoData.getResolutionY();
        int frameRate = videoData.getFrameRate();
        float videoDuration = videoData.getVideoDuration();
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {

            dos.writeInt(resolutionX);// 写入元数据
            dos.writeInt(resolutionY);
            dos.writeInt(frameRate);
            dos.writeFloat(videoDuration);
            dos.writeUTF(url);


            int frameCount = frames.size();// 写入帧数据
            int pixelLength = frameCount > 0 ? frames.get(0).length : 0;
            dos.writeInt(frameCount);//帧总数
            dos.writeInt(pixelLength);//单帧像素长度
            for (int[] frame : frames) {
                for (int pixel : frame) {
                    /* pixel to rgb
                     * pixel 是一个 32 bit 即 4 字节的数据
                     * 其中每个字节存储1个值 分别存储 A R G B 四个值
                     * 在 minecraft 中 A 值 即透明度固定为 FF 即 256
                     */
                    dos.writeByte((byte) ((pixel >> 16) & 0b11111111));//r
                    dos.writeByte((byte) ((pixel >> 8) & 0b11111111));//g
                    dos.writeByte((byte) (pixel & 0b11111111));//b
                }
            }
            return bos.toByteArray();
        } catch (IOException e) {
            System.err.println("视频数据序列化失败: " + e.getMessage());
            return new byte[0];
        }
    }

    /* 反序列化视频数据 */
    private VideoData deserializeFrames(byte[] data) {
        VideoDataNull videoDataNull = new VideoDataNull();
        if (data == null || data.length == 0) {
            return videoDataNull;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             DataInputStream dis = new DataInputStream(bis)) {

            int resolutionX = dis.readInt();// 读取元数据
            int resolutionY = dis.readInt();
            int frameRate = dis.readInt();
            float videoDuration = dis.readFloat();
            String url = dis.readUTF();


            VideoData videoData = new VideoData(url, resolutionX, resolutionY);// 创建VideoData对象
            videoData.setFrameRate(frameRate);
            videoData.setVideoDuration(videoDuration);

            int frameCount = dis.readInt();//读取帧总数
            int pixelLength = dis.readInt();//读取单帧像素长度
            for (int i = 0; i < frameCount; i++) {
                int[] frame = new int[pixelLength];
                for (int j = 0; j < pixelLength; j++) {

                    int r = dis.readByte() & 0xFF; // // 读取字节并转换为无符号整数 转换为 0-255
                    int g = dis.readByte() & 0xFF;
                    int b = dis.readByte() & 0xFF;

                    frame[j] = 0xFF000000 | (r << 16) | (g << 8) | b;// 组合为 ARGB 像素（Alpha 固定为 0xFF）
                }
                videoData.addFrame(frame);
            }

            return videoData;
        } catch (IOException e) {
            System.err.println("视频数据反序列化失败: " + e.getMessage());
        }
        return videoDataNull;
    }
    /**===================== 序列化工具方法end =====================**/

    /**
     * 清除所有状态和订阅者（测试用）
     */
    public void clear() {
        state.clear();
        subscribers.clear();
        weakSubscribers.clear();
    }

    /**
     * 获取当前状态快照（线程安全的不可变副本）
     */
    public Map<String, Object> getStateSnapshot() {
        return new ConcurrentHashMap<>(state);
    }
}

/** 以下是如何使用此Store类: **/
/**
 class StoreDemo {
 public static void main(String[] args) {
 StoreVan store = StoreVan.getInstance();

 // 强引用订阅（需手动取消）
 store.subscribe("count", newValue -> {
 System.out.println("[强引用]Count更新: " + newValue);
 });

 // 弱引用订阅（自动回收）
 store.subscribeWeak("user", newValue -> {
 System.out.println("[弱引用]User更新: " + newValue);
 });

 // 设置初始状态
 store.set("count", 0);
 store.set("user", null);

 // 触发更新
 store.set("count", 1);  // 会触发通知
 store.set("count", 1);  // 值相同，不会触发通知
 store.set("user", "Alice");

 // 获取当前状态
 System.out.println("当前状态: " + store.getStateSnapshot());
 }
 }
 **/
