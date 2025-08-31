package com.myacghome.mcvan.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import com.myacghome.mcvan.ToolVan;
import java.util.function.Supplier;

/**
 * 用于同步VideoBlock的url(服务端)
 */
public class VideoBlockUpdateUrlPacket {
    private final BlockPos pos;
    private final String url;

    /**
     * 更新VideoScreenBlock的实体数据
     * @param pos
     * @param url
     */
    public VideoBlockUpdateUrlPacket(BlockPos pos, String url) {
        this.url = url;
        this.pos = pos;
    }

    /**
     * 反序列化构造器
     * 从字节流重建数据包对象（通过 FriendlyByteBuf 读取数据）
     * 更新VideoScreenBlock的实体数据
     * @param buf
     */
    public VideoBlockUpdateUrlPacket(FriendlyByteBuf buf) {
        this.url = buf.readUtf();
        this.pos = buf.readBlockPos();
    }

    /**
     * 序列化方法
     * 将数据包对象转换为字节流（通过 FriendlyByteBuf 写入网络）
     * @param buf
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(url);
        buf.writeBlockPos(pos);
    }

    /**
     * 接收到数据包后的处理逻辑（如修改方块的 URL）
     * 线程安全：必须通过 enqueueWork() 将操作提交到主游戏线程执行（网络线程与游戏线程分离）
     * 更新实体方块的数据
     * @param ctx
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof VideoScreenBlockEntity) {
                    ((VideoScreenBlockEntity) be).setUrl(url);
                    be.setChanged(); // 标记方块实体数据已更改
                    ToolVan.logToChat("[VideoBlockUpdateUrlPacket]服务器成功接收 URL: "+url);
                }
                else if(be instanceof VideoScreenBlockEntity1609){
                    ((VideoScreenBlockEntity1609) be).setUrl(url);
                    be.setChanged(); // 标记方块实体数据已更改
                    ToolVan.logToChat("[VideoBlockUpdateUrlPacket]服务器成功接收 URL: "+url);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}