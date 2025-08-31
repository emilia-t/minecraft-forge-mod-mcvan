package com.myacghome.mcvan.block;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public class VideoScreenBlockEntity1609 extends BlockEntity implements MenuProvider {
    private String                  url = "";
    private int                     currentFrameIndex = 0;
    /**
     * 主构造器
     * @param pos
     * @param state
     */
    public VideoScreenBlockEntity1609(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VIDEO_SCREEN_1609.get(), pos, state); // 使用 BlockEntityType
    }

    /**
     * 是否应该渲染某个面（仅限X轴）垂直于基岩的
     * @param pFace
     * @return
     */
    public boolean shouldRenderFace(Direction pFace) {
        return pFace.getAxis() == Direction.Axis.X;
    }

    static private void debugPixels(int[] pixels, int rowMaxSize) {
        if(pixels == null || rowMaxSize <= 0) {
            return;
        }
        final class RowPrint {
            public RowPrint(int[] row, int maxPerLine) {
                StringBuilder temp = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    if(i > 0 && i % maxPerLine == 0) {
                        System.out.println(temp.toString());
                        temp = new StringBuilder();
                    }
                    temp.append(row[i]).append(" ");
                }
                if(!temp.isEmpty()) {
                    System.out.println(temp.toString());
                }
            }
        }
        new RowPrint(pixels, rowMaxSize);
    }

    /**
     * 注意，这个函数只会被服务端线程调用
     * @param url
     */
    public void setUrl(String url) {
        if (!Objects.equals(this.url, url)) {// 检查URL是否实际发生变化
            this.url = url;
            setChanged(); // 标记数据需要保存
        }
    }

    public String getUrl(){
        return this.url;
    }

    /**
     * 这里会在 /data block 中显示方块实体的部分数据
     * @param tag
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        url = tag.getString("VideoURL");
        currentFrameIndex = tag.getInt("currentFrameIndex");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putString("VideoURL", url);
        tag.putInt("currentFrameIndex", currentFrameIndex);
        super.saveAdditional(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mcvan.video_control_menu"); // 提供菜单名称
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());// 创建数据包并写入 BlockPos
        buf.writeBlockPos(this.getBlockPos());
        return new VideoControlMenu(containerId, playerInventory, buf);
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public void setCurrentFrameIndex(int currentFrameIndex) {
        this.currentFrameIndex = currentFrameIndex;
    }
}
