package com.myacghome.mcvan.block;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.myacghome.mcvan.StoreVan;
import com.myacghome.mcvan.util.VideoData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

@SuppressWarnings("all")
public class VideoScreenRenderer1609 implements BlockEntityRenderer<VideoScreenBlockEntity1609> {
    StoreVan store = StoreVan.getInstance();// 创建一个全局的Store对象
    private final Minecraft mc = Minecraft.getInstance();
    private final ResourceLocation dynamicTextureId;
    private final DynamicTexture dynamicTexture;
    private static final float BORDER_WIDTH = 0.0625f;//屏幕面黑边厚度

    private static final int BORDER_COLOR = 0xFFC0C0C0; // 银灰色边框 (RGB: 192,192,192)
    private static final int BORDER_SILVER_R = 192;
    private static final int BORDER_SILVER_G = 192;
    private static final int BORDER_SILVER_B = 192;
    private static final int BORDER_SILVER_A = 255;

    private static final int TEXTURE_WIDTH = 1024;  // 16 blocks * 64px
    private static final int TEXTURE_HEIGHT = 576; // 9 blocks * 64px

    private static final float RENDER_WIDTH = 16.0f;  // 16 blocks wide
    private static final float RENDER_HEIGHT = 9.0f; // 9 blocks tall

    public VideoScreenRenderer1609(BlockEntityRendererProvider.Context context, ResourceLocation dynamicTextureId) {
        this.dynamicTextureId = dynamicTextureId; // 直接使用传入的 ResourceLocation
        this.dynamicTexture = (DynamicTexture) mc.getTextureManager().getTexture(dynamicTextureId);
    }

    @Override
    public void render(
            VideoScreenBlockEntity1609 be,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay){
        try {
            poseStack.pushPose();

            Direction dir = be.getBlockState().getValue(VideoScreenBlock1609.FACING);// 调整位置和旋转
            switch (dir) {
                case NORTH -> {}
                case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
                case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270));
            }
            poseStack.translate(0.0, 0.0, 1.0); // 将面朝前

            RenderSystem.setShaderTexture(0, dynamicTextureId);// 绑定动态纹理并渲染四边形

            VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutout(dynamicTextureId));// 获取 VertexConsumer
            Matrix4f matrix = poseStack.last().pose();

            this.renderScreen(be, matrix, bufferSource.getBuffer(RenderType.entityCutout(dynamicTextureId)));// 渲染屏幕画面

            float viewScreenX = (float) ScreenDynamicTexture1609.getViewScreenX();// 渲染边框
            float viewScreenY = (float) ScreenDynamicTexture1609.getViewScreenY();
            if(viewScreenX == 0 || viewScreenY==0){//材质未更新
                return;
            }
            else{
                float videoWidthBlocks = viewScreenX / VideoData.blockPixelDefaultF;// 计算视频在方块坐标系中的位置和尺寸
                float videoHeightBlocks = viewScreenY / VideoData.blockPixelDefaultF;
                float startX = (RENDER_WIDTH - videoWidthBlocks) / 2.0f;
                float startY = (RENDER_HEIGHT - videoHeightBlocks) / 2.0f;
                float endX = startX + videoWidthBlocks;
                float endY = startY + videoHeightBlocks;//制高点
                this.renderBorder(be, matrix, bufferSource, startX, startY, endX, endY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * 判断是否全黑图片(快速九点采样模式)
     **/
    private boolean isAllBlackFast(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[][] samplePoints = {
                {0, 0}, {width / 2, 0}, {width - 1, 0},
                {0, height / 2}, {width / 2, height / 2}, {width - 1, height / 2},
                {0, height - 1}, {width / 2, height - 1}, {width - 1, height - 1}
        };

        for (int[] point : samplePoints) {
            int color = image.getPixelRGBA(point[0], point[1]);
            if ((color & 0x00FFFFFF) != 0) {
                return false; // 如果RGB部分有非黑像素
            }
        }

        return true;
    }

    /**
     * 是否渲染在屏幕外
     * @param entity
     * @return
     */
    @Override
    public boolean shouldRenderOffScreen(VideoScreenBlockEntity1609 entity) {
        return true; // 始终渲染，避免距离剔除
    }

    /**
     * 渲染屏幕边框（基于视频实际显示区域）videoStartY在下面
     */
    private void renderBorder(
            VideoScreenBlockEntity1609 pBlockEntity,
            Matrix4f pPose,
            MultiBufferSource bufferSource,
            float videoStartX,
            float videoStartY,
            float videoEndX,
            float videoEndY
    ) {

        VertexConsumer borderVc = bufferSource.getBuffer(RenderType.entitySolid(new ResourceLocation("textures/block/iron_block.png")));// 使用黑色纯色渲染边框

        renderFaceBoard(pPose, borderVc,//上
                1.1f,
                1.1f,
                videoEndY + BORDER_WIDTH,
                videoEndY,
                -1.0f - BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderFaceBoard(pPose, borderVc,//右
                1.1f,
                1.1f,
                videoEndY,
                videoStartY,
                -1.0f - BORDER_WIDTH,
                -1.0f,
                -1.0f,
                -1.0f - BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderFaceBoard(pPose, borderVc,//下
                1.1f,
                1.1f,
                videoStartY,
                videoStartY - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderFaceBoard(pPose, borderVc,//左
                1.1f,
                1.1f,
                videoEndY,
                videoStartY,
                15.0f,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderScreenBoard(pPose, borderVc,//顶侧
                1.0f,
                1.1f,
                videoEndY + BORDER_WIDTH,
                videoEndY + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderScreenBoard(pPose, borderVc,//右侧
                1.1f,
                1.0f,
                videoStartY - BORDER_WIDTH,
                videoEndY + BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderScreenBoard(pPose, borderVc,//底侧
                1.0f,
                1.1f,
                videoStartY - BORDER_WIDTH,
                videoStartY - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderScreenBoard(pPose, borderVc,//左侧
                1.0f,
                1.1f,
                videoStartY - BORDER_WIDTH,
                videoEndY + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );

        renderScreenBoard(pPose, borderVc,//背侧
                1.0f,
                1.0f,
                videoEndY + BORDER_WIDTH,
                videoStartY - BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                -1.0f - BORDER_WIDTH,
                15.0f + BORDER_WIDTH,
                BORDER_SILVER_R, BORDER_SILVER_G, BORDER_SILVER_B, BORDER_SILVER_A
        );
    }

    /**
     * 渲染屏幕上下左右黑边
     */
    private void renderFaceBoard(Matrix4f pPose, VertexConsumer pConsumer,
                                float pX0,
                                float pX1,
                                float pY0,
                                float pY1,
                                float pZ0,
                                float pZ1,
                                float pZ2,
                                float pZ3,
                                 int r, int g, int b, int a){
        pConsumer.vertex(pPose, pX0, pY0, pZ0)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX1, pY0, pZ1)
                .color(r, g, b, a)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX1, pY1, pZ2)
                .color(r, g, b, a)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX0, pY1, pZ3)
                .color(r, g, b, a)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
    }

    /**
     * 渲染屏幕边框
     */
    private void renderScreenBoard(Matrix4f pPose, VertexConsumer pConsumer,
                                 float pX0,
                                 float pX1,
                                 float pY0,
                                 float pY1,
                                 float pZ0,
                                 float pZ1,
                                 float pZ2,
                                 float pZ3,
                                 int r, int g, int b, int a){
        pConsumer.vertex(pPose, pX0, pY0, pZ0)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX1, pY0, pZ1)
                .color(r, g, b, a)
                .uv(1, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX1, pY1, pZ2)
                .color(r, g, b, a)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
        pConsumer.vertex(pPose, pX0, pY1, pZ3)
                .color(r, g, b, a)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(15728880)
                .normal(Direction.EAST.getStepX(), Direction.EAST.getStepY(), Direction.EAST.getStepZ())
                .endVertex();
    }

    /**
     * 渲染屏幕画面
     * @param pBlockEntity
     * @param pPose
     * @param pConsumer
     */
    private void renderScreen(VideoScreenBlockEntity1609 pBlockEntity, Matrix4f pPose, VertexConsumer pConsumer) {
        this.renderFace(pBlockEntity, pPose, pConsumer,
                1.1F,
                1.1F,
                9.0F,
                0.0F,
                -1.0F,
                15.0F,
                15.0F,
                -1.0F,
                Direction.EAST);//渲染屏幕面的显示区域
    }

    /**
     * 渲染某一面
     * @param pBlockEntity
     * @param pPose
     * @param pConsumer
     * @param pX0
     * @param pX1
     * @param pY0
     * @param pY1
     * @param pZ0
     * @param pZ1
     * @param pZ2
     * @param pZ3
     * @param pDirection
     */
    private void renderFace(VideoScreenBlockEntity1609 pBlockEntity, Matrix4f pPose, VertexConsumer pConsumer,
                            float pX0,
                            float pX1,
                            float pY0,
                            float pY1,
                            float pZ0,
                            float pZ1,
                            float pZ2,
                            float pZ3,
                            Direction pDirection) {
        if (pBlockEntity.shouldRenderFace(pDirection)) {
            pConsumer.vertex(pPose, pX0, pY0, pZ0)
                    .color(255, 255, 255, 255)
                    .uv(0, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ())
                    .endVertex();
            pConsumer.vertex(pPose, pX1, pY0, pZ1)
                    .color(255, 255, 255, 255)
                    .uv(1, 0)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ())
                    .endVertex();
            pConsumer.vertex(pPose, pX1, pY1, pZ2)
                    .color(255, 255, 255, 255)
                    .uv(1, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ())
                    .endVertex();
            pConsumer.vertex(pPose, pX0, pY1, pZ3)
                    .color(255, 255, 255, 255)
                    .uv(0, 1)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(15728880)
                    .normal(pDirection.getStepX(), pDirection.getStepY(), pDirection.getStepZ())
                    .endVertex();
        }
    }
}
