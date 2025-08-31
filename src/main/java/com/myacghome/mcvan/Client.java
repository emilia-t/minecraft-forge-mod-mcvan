package com.myacghome.mcvan;

import com.google.errorprone.annotations.CompileTimeConstant;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.myacghome.mcvan.block.*;
import com.myacghome.mcvan.entity.BossModel;
import com.myacghome.mcvan.entity.ModEntities;
import com.myacghome.mcvan.item.BigBowItem;
import com.myacghome.mcvan.wallpaper.WallpaperSystemClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.myacghome.mcvan.entity.BossRenderer;


/**CLASS
 * describe: The client mod initializer.
 * notepad: 用于处理客户端的初始化工作
 */
@Mod.EventBusSubscriber(modid = IndexMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Client {

    /**Area start
     * 动态材质
     */
    private static ScreenDynamicTexture dynamicTexture;
    private static ScreenDynamicTexture1609 dynamicTexture1609;

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && dynamicTexture != null) {
            dynamicTexture.upload();
            dynamicTexture1609.upload();
        }
    }

    public static ResourceLocation getDynamicTexture() {
        return dynamicTexture.getTextureLocation();
    }

    public static ResourceLocation getDynamicTexture1609() {
        return dynamicTexture1609.getTextureLocation();
    }

    public static ScreenDynamicTexture getDynamicTextureSource() {
        return dynamicTexture;
    }

    public static ScreenDynamicTexture1609 getDynamicTextureSource1609() {
        return dynamicTexture1609;
    }

    /**Area end
     * 动态材质
     */


    @CompileTimeConstant
    static final int bowMaxUseDuration = 72000;// 弓可以使用的最长时间（以Tick数为单位）
    static final float bow_powerDiv = 6.0F;// 弓的最大射程除以最大射程，用于计算弓动画的缩放比例
    /**
     * describe: 初始化客户端
     * notepad: 注册ItemProperties，并注册事件监听器
     * param: event
     */
    @SuppressWarnings("all")
    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event){
        final Item[] bows = IndexMod.getAllBowItems();
        event.enqueueWork(() -> {
            dynamicTexture = new ScreenDynamicTexture("screen_dynamic_texture");
            dynamicTexture1609 = new ScreenDynamicTexture1609("screen_dynamic_texture_1609");
            MinecraftForge.EVENT_BUS.addListener(Client::onClientTick);
        });
        event.enqueueWork(()->{
            final ResourceLocation PULL = new ResourceLocation("pull");
            final ResourceLocation PULLING = new ResourceLocation("pulling");
            final ItemPropertyFunction PULL_PROVIDER = (stack, world, entity, unused) -> ((entity == null) || (entity.getUseItem() != stack) ? 0.0F : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / bow_powerDiv);
            final ItemPropertyFunction PULLING_PROVIDER = (stack, world, entity, unused) -> ((entity != null) && entity.isUsingItem() && (entity.getUseItem() == stack) ? 1.0F : 0.0F);
            for(final Item bow : bows){
                ItemProperties.register(bow, PULL, PULL_PROVIDER);
                ItemProperties.register(bow, PULLING, PULLING_PROVIDER);
            }
        });
        final Client clientListener = new Client();
        MinecraftForge.EVENT_BUS.addListener(clientListener::FOV);
        MinecraftForge.EVENT_BUS.addListener(clientListener::renderBow);
        MinecraftForge.EVENT_BUS.addListener(WallpaperSystemClient::todoInitTexture);
        /**
         * 一些自定义的实体渲染器
         */
        EntityRenderers.register(
                ModEntities.BOSS.get(),
                BossRenderer::new
        );
        MenuScreens.register(
                IndexMod.VIDEO_CONTROL_MENU.get(),
                VideoControlScreen::new
        );
        BlockEntityRenderers.register(
                ModBlockEntities.VIDEO_SCREEN.get(),
                context -> new VideoScreenRenderer(context, Client.getDynamicTexture())  // 添加动态纹理参数
        );
        BlockEntityRenderers.register(
                ModBlockEntities.VIDEO_SCREEN_1609.get(),
                context -> new VideoScreenRenderer1609(context, Client.getDynamicTexture1609())  // 添加动态纹理参数
        );
    }

    /**
     * describe: 注册自定义实体渲染器bossModel
     * @param event
     */
    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BossModel.LAYER_LOCATION, BossModel::createBodyLayer);
    }


    /**
     * describe: 处理FOV缩放，当绘制自定义弓时
     * param: event the FOVUpdateEvent
     */
    @SubscribeEvent
    public void FOV(ComputeFovModifierEvent event){
        final Player eventPlayer = event.getPlayer();
        final Item eventItem = eventPlayer.getUseItem().getItem();

        if(eventItem instanceof final BigBowItem bow){
            float finalFov = event.getNewFovModifier();
            float customBow = eventPlayer.getTicksUsingItem() / bow_powerDiv;
            if(customBow > 1.0F){
                customBow = 1.0F;
            }
            else{
                customBow *= customBow;
            }
            finalFov *= 1.0F - (customBow * 0.15F);
            event.setNewFovModifier(finalFov);
        }
    }

    /**
     * describe: 处理以第一人称绘制CustomBow时的渲染。
     * notepad: 这是使弓以正确的速度后退所必需的。
     * param: renderHandEvent the RenderHandEvent
     */
    @SubscribeEvent
    public void renderBow(RenderHandEvent renderHandEvent) {
        final Minecraft mc = Minecraft.getInstance();
        if(mc.options.getCameraType().isFirstPerson()){//仅当我们以第一人称并绘制CustomBow时才处理渲染。
            assert mc.player != null;//断言mc.player不为空
            if(  mc.player.isUsingItem() &&
                    (mc.player.getUsedItemHand() == renderHandEvent.getHand()) &&
                    (mc.player.getTicksUsingItem() > 0) &&
                    (renderHandEvent.getItemStack().getItem() instanceof final BigBowItem bow)
            ){
                renderHandEvent.setCanceled(true);//先取消渲染，以便我们后续进行渲染
                final PoseStack poseStack = renderHandEvent.getPoseStack();
                poseStack.pushPose();
                final boolean rightHanded=(
                        renderHandEvent.getHand()==InteractionHand.MAIN_HAND
                                ?
                                mc.player.getMainArm()
                                :
                                mc.player.getMainArm().getOpposite()
                ) == HumanoidArm.RIGHT;// 判断是否手持在右手
                final int handedSide = rightHanded ? 1 : -1;
                poseStack.translate(
                        handedSide * 0.2814318F,
                        -0.3365561F + (renderHandEvent.getEquipProgress() * -0.6F),
                        -0.5626847F
                );
                poseStack.mulPose(Axis.XP.rotationDegrees(-13.935F));// 手旋转角度
                poseStack.mulPose(Axis.YP.rotationDegrees(handedSide * 35.3F));// 手旋转角度
                poseStack.mulPose(Axis.ZP.rotationDegrees(handedSide * -9.785F));// 手旋转角度
                final float ticks = bowMaxUseDuration - ((mc.player.getUseItemRemainingTicks() - renderHandEvent.getPartialTick()) + 1.0F);
                float divTicks = ticks / bow_powerDiv;
                divTicks = ((divTicks * divTicks) + (divTicks * 2.0F)) / 3.0F;
                if (divTicks > 1.0F) {
                    divTicks = 1.0F;
                }
                if (divTicks > 0.1F) {// 弓动画和变换
                    poseStack.translate(0.0F, Mth.sin((ticks - 0.1F) * 1.3F) * (divTicks - 0.1F) * 0.004F, 0.0F);// 弓的摇晃
                }
                poseStack.translate(0.0F, 0.0F, divTicks * 0.04F);// 向后运动 ("draw back" 动效)
                poseStack.scale(1.0F, 1.0F, 1.0F + (divTicks * 0.2F));// 基于FOV原因的相对缩放
                poseStack.mulPose(Axis.YN.rotationDegrees(handedSide * 45.0F));// 根据手性旋转弓
                final ItemDisplayContext type = rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;// 让Minecraft完成剩余的物品渲染
                mc.getItemRenderer().renderStatic(
                        mc.player,
                        renderHandEvent.getItemStack(),
                        type,
                        !rightHanded,
                        poseStack,
                        renderHandEvent.getMultiBufferSource(),
                        mc.player.level(),
                        renderHandEvent.getPackedLight(),
                        OverlayTexture.NO_OVERLAY,
                        mc.player.getId() + type.ordinal()
                );
                poseStack.popPose();
            }
        }
    }
}
