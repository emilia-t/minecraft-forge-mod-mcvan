package com.myacghome.mcvan.block;

import com.myacghome.mcvan.IndexMod;
import com.myacghome.mcvan.ToolVan;
import com.myacghome.mcvan.network.InstructPacket;
import com.myacghome.mcvan.util.VideoData;
import com.myacghome.mcvan.util.VideoDataNull;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.myacghome.mcvan.network.NetworkHandler;
import net.minecraft.world.item.ItemStack;
import com.myacghome.mcvan.network.InstructCreate;
import net.minecraftforge.network.PacketDistributor;
import org.checkerframework.checker.index.qual.PolyUpperBound;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.myacghome.mcvan.IndexMod.store;
import static com.myacghome.mcvan.util.VideoData.FPS;

/**
 * VideoBlock控制菜单
 */
public class VideoControlMenu extends AbstractContainerMenu {
    private static final AtomicBoolean       isPlaying = new AtomicBoolean(false);// 添加播放状态控制变量
    private static ScheduledExecutorService  scheduler;
    public static String                     url = "";//当前操作的视频 url
    private final BlockPos                   pos;//创建菜单时的BlockPos
    public VideoControlMenu(int id, Inventory inv, FriendlyByteBuf data) {
        super(IndexMod.VIDEO_CONTROL_MENU.get(), id); // 使用注册的MenuType
        this.pos = data.readBlockPos(); // 从数据包读取BlockPos
    }

    public String getUrl(){
        System.out.println("[VideoControlMenu]getUrl: " + url);
        return url;
    }

    public void setUrl(String url) {
        VideoControlMenu.url = url;
        ToolVan.logToChat("[VideoControlMenu]setUrl: " + VideoControlMenu.url);
        System.out.println("[VideoControlMenu]setUrl: " + VideoControlMenu.url);
    }

    /**
     * c2s
     * 将要播放的视频链接发送至服务端
     * @param url
     */
    public void sendUrlToS(String url) {
        this.setUrl(url);
        NetworkHandler.INSTANCE.sendToServer(new VideoBlockUpdateUrlPacket(pos, url)); // 传递BlockPos和URL
    }

    /**
     * c2s
     * 向服务器发起视频数据拉取指令
     * @param url
     */
    public void sendLoadVideoInstructToS(String url,BlockPos pos) {
        this.setUrl(url);
        int px = pos.getX();
        int py = pos.getY();
        int pz = pos.getZ();
        String commandJson = InstructCreate.c2sLoadVideo(url,px,py,pz);// 创建指令
        NetworkHandler.INSTANCE.sendToServer(new InstructPacket(1, commandJson));
    }


    /**
     * c2s
     * 获取服务器的目标视频的帧总数
     * @param url
     */
    public void sendGetVideoFrameCountToS(String url) {
        String commandJson = InstructCreate.c2sGetVideoFrameCount(url);// 创建指令
        NetworkHandler.INSTANCE.sendToServer(new InstructPacket(1, commandJson));
    }

    /**
     * 检查视频是否存在(client store)
     */
    public void isExistsVideo(String url){
        VideoData videoData = store.getVideoData(url);
        if(videoData instanceof VideoDataNull){
            ToolVan.logToChat("[VideoControlMenu]视频暂未加入库中");
        }
        else {
            ToolVan.logToChat("[VideoControlMenu]视频已经加入库中");
        }
    }

    /**
     * 保存最后一帧图像到截图目录(client)
     */
    public void saveLastFrameToFile(String url){
        VideoData videoData = store.getVideoData(url);
        if(videoData instanceof VideoDataNull){
            ToolVan.logToChat("[VideoControlMenu]视频暂未加入库中");
        }
        else {
            videoData.saveLastFrameToFile();
        }
    }

    /**
     * 保存随机一帧图像到截图目录(client)
     */
    public void saveRandomFrameToFile(String url){
        VideoData videoData = store.getVideoData(url);
        if(videoData instanceof VideoDataNull){
            ToolVan.logToChat("[VideoControlMenu]视频暂未加入库中");
        }
        else {
            videoData.saveRandomFrameToFile();
        }
    }

    /**
     * 播放视频按钮(client 调用)
     */
    public void playVideo() {
        if (isPlaying.get()) {
            ToolVan.logToChat("[VideoControlMenu]视频已在播放中");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();// 创建单线程调度器
        isPlaying.set(true);

        long frameInterval = 1000 / FPS;// 计算每帧间隔时间（1000ms / 30fps ≈ 33.33ms）

        scheduler.scheduleAtFixedRate(() -> {// 启动定时任务
            if (isPlaying.get()) {
                renderFrameNext();
            }
        }, 0, frameInterval, TimeUnit.MILLISECONDS);

        ToolVan.logToChat("[VideoControlMenu]开始播放视频");
    }

    /**
     * 暂停播放按钮(client 调用)
     */
    public void pauseVideo() {
        if (!isPlaying.get()) {
            ToolVan.logToChat("[VideoControlMenu]视频未在播放");
            return;
        }

        if (scheduler != null) {// 停止调度器
            scheduler.shutdownNow();
            scheduler = null;
        }
        isPlaying.set(false);
        ToolVan.logToChat("[VideoControlMenu]暂停播放");
    }

    /**
     * 关闭菜单时的操作
     * @param player
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    /**
     * 上一帧按钮(client 调用)
     */
    public void renderFrameBack(){
        NetworkHandler.INSTANCE.sendToServer(
                new VideoBlockFrameIndexBackPacket(pos)
        );
    }

    /**
     * 下一帧按钮(client 调用)
     */
    public void renderFrameNext() {
        NetworkHandler.INSTANCE.sendToServer(
                new VideoBlockFrameIndexNextPacket(pos)
        );
    }

    /**
     * 重头开始播放按钮(client 调用)
     */
    public void renderFrameRestart() {
        NetworkHandler.INSTANCE.sendToServer(
                new VideoBlockFrameIndexRestartPacket(pos)
        );
    }
    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    public BlockPos getPos(){
        return this.pos;
    }
}
