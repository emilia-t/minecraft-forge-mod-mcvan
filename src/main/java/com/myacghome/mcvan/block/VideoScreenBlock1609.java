package com.myacghome.mcvan.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;


public class VideoScreenBlock1609 extends Block implements EntityBlock {//方块实体必须实现EntityBlock

    private static final VoxelShape SHAPE = Shapes.create(//碰撞箱
            0.0, //pMinX
            0.0, //pMinY
            0.0, //pMinZ
            1.0, //pMaxX
            9.0, //pMaxY
            16.0  //pMaxZ
    );

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * 构造函数
     * @param props
     */
    public VideoScreenBlock1609(Properties props) {
        super(props
                .noOcclusion()
                .noCollission()
                .lightLevel(state -> 0) // 确保不发光
                .isViewBlocking((state, world, pos) -> false)
                .noLootTable()
        );
    }

    /**
     * 添加碰撞箱方法
     * @param state
     * @param world
     * @param pos
     * @param context
     * @return
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        /*
         *return Shapes.empty();// 返回空形状（视觉上不可见）
         */
        return SHAPE;
    }

    /**
     * 添加遮挡形状
     * @param state
     * @param world
     * @param pos
     * @return
     */
    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        /*
         *return Shapes.empty();//返回空形状（不影响光照）
         */
        return SHAPE;
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.block();// 返回完整方块形状用于交互检测
    }

    /**
     * 创建实体方块(自动)
     * @param pos
     * @param state
     * @return
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VideoScreenBlockEntity1609(pos, state);
    }

    /**
     * 右键使用时展开菜单
     * @param state
     * @param level
     * @param pos
     * @param player
     * @param hand
     * @param hit
     * @return
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {//玩家右键方块时，服务端会向客户端发送一个打开菜单面板的指令
            MenuProvider screenHandlerFactory = getMenuProvider(state, level, pos); // 使用覆盖后的方法
            if (screenHandlerFactory != null) {
                NetworkHooks.openScreen((ServerPlayer) player, screenHandlerFactory, pos);//打开菜单
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * 获取菜单的名称(与注册时一致)
     * @param state
     * @param level
     * @param pos
     * @return
     */
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MenuProvider ? (MenuProvider) blockEntity : null;
    }
    public Direction getFacing(BlockState state) {
        return state.getValue(FACING);
    }
}
