package com.myacghome.mcvan.block;

import com.myacghome.mcvan.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * c2s 客户端发送请求
 */
public class VideoBlockFrameIndexRestartPacket {
    private final BlockPos pos;

    public VideoBlockFrameIndexRestartPacket(BlockPos pos) {
        this.pos = pos;
    }

    public VideoBlockFrameIndexRestartPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    /**
     * 服务端处理
     * @param ctx
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                BlockEntity be = player.level().getBlockEntity(pos);
                if (be instanceof VideoScreenBlockEntity) {
                    String url = ((VideoScreenBlockEntity) be).getUrl();
                    ((VideoScreenBlockEntity) be).setCurrentFrameIndex(0);
                    be.setChanged(); // 标记方块实体数据已更改

                    LevelChunk chunk = (LevelChunk) Objects.requireNonNull(be.getLevel()).getChunk(be.getBlockPos());// 获取方块实体所在的区块

                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.TRACKING_CHUNK.with(() -> chunk), // 发送给加载此区块及其附近8个区块内的玩家
                            new VideoBlockFrameIndexRenderPacket(pos, url,0,0)
                    );
                }
                else if(be instanceof VideoScreenBlockEntity1609){
                    String url = ((VideoScreenBlockEntity1609) be).getUrl();
                    ((VideoScreenBlockEntity1609) be).setCurrentFrameIndex(0);
                    be.setChanged(); // 标记方块实体数据已更改

                    LevelChunk chunk = (LevelChunk) Objects.requireNonNull(be.getLevel()).getChunk(be.getBlockPos());// 获取方块实体所在的区块

                    NetworkHandler.INSTANCE.send(
                            PacketDistributor.TRACKING_CHUNK.with(() -> chunk), // 发送给加载此区块及其附近8个区块内的玩家
                            new VideoBlockFrameIndexRenderPacket(pos, url,0,1609)
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}