package com.myacghome.mcvan.block;

import com.myacghome.mcvan.util.VideoData;
import com.myacghome.mcvan.util.VideoDataNull;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import static com.myacghome.mcvan.IndexMod.store;

public class VideoControlScreen extends AbstractContainerScreen<VideoControlMenu> {
    private EditBox urlField;
    private int leftMargin_l1 = leftPos + 10;//第一列左侧边距
    private int leftMargin_l2 = leftPos + 120;//第二列左侧边距
    private int leftMargin_l3 = leftPos + 230;
    private int topMargin = topPos + 10;//所有元素的顶部基础边距
    private String my_url = "UNKNOWN";
    private net.minecraft.client.gui.components.StringWidget frameCountWidget; // 帧数显示组件
    private net.minecraft.client.gui.components.StringWidget urlContentWidget; // 帧数显示组件

    public VideoControlScreen(VideoControlMenu menu, net.minecraft.world.entity.player.Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        super.init();

        Component button1Text = Component.translatable("gui.mcvan.video_control.set_url");
        Component button2Text = Component.translatable("gui.mcvan.video_control.load_video");
        Component button3Text = Component.translatable("gui.mcvan.video_control.check_video_exists");
        Component button4Text = Component.translatable("gui.mcvan.video_control.save_last_frame");
        Component button5Text = Component.translatable("gui.mcvan.video_control.save_random_frame");
        Component button6Text = Component.translatable("gui.mcvan.video_control.play_video");
        Component button7Text = Component.translatable("gui.mcvan.video_control.pause_video");
        Component button8Text = Component.translatable("gui.mcvan.video_control.render_frame_back");
        Component button9Text = Component.translatable("gui.mcvan.video_control.render_frame_next");
        Component button10Text = Component.translatable("gui.mcvan.video_control.render_frame_restart");
        Component title1Text = Component.translatable("gui.mcvan.video_control.current_frame_index");
        /*
         URL输入框
         */
        urlField = new EditBox(
                font,
                leftMargin_l1,
                topMargin,
                300,
                20,
                Component.translatable("gui.mcvan.video_control.url_prompt")
        );
        urlField.setMaxLength(Integer.MAX_VALUE);
        addRenderableWidget(urlField);
        /**第一列按钮**/
        /*
         设置url的按钮
         */
        addRenderableWidget(
                Button.builder(
                                button1Text, // 使用本地化文本
                                btn -> {
                                    menu.sendUrlToS(urlField.getValue());
                                }
                        )
                        .bounds(leftMargin_l1, topMargin + 30, 100, 20)
                        .build()
        );

        /*
         向服务器发送拉取视频数据的指令按钮
         */
        addRenderableWidget(
                Button.builder(
                                button2Text,
                                btn -> {
                                    menu.sendLoadVideoInstructToS(urlField.getValue(),menu.getPos());
                                }
                        )
                        .bounds(leftMargin_l1, topMargin + 60, 100, 20)
                        .build()
        );

        /*
         检查此视频(url)是否已经存在的按钮(在客户端线程中)
         */
        addRenderableWidget(
                Button.builder(
                                button3Text,
                                btn -> {
                                    menu.isExistsVideo(urlField.getValue());
                                }
                        )
                        .bounds(leftMargin_l1, topMargin + 90, 100, 20)
                        .build()
        );

        /*
         保存最后一帧图像到截图目录
         */
        addRenderableWidget(
                Button.builder(
                                button4Text,
                                btn -> {
                                    menu.saveLastFrameToFile(urlField.getValue());
                                }
                        )
                        .bounds(leftMargin_l1, topMargin + 120, 100, 20)
                        .build()
        );

        /*
         保存随机一帧图像到截图目录
         */
        addRenderableWidget(
                Button.builder(
                                button5Text,
                                btn -> {
                                    menu.saveRandomFrameToFile(urlField.getValue());
                                }
                        )
                        .bounds(leftMargin_l1, topMargin + 150, 100, 20)
                        .build()
        );

        /**第二列按钮**/

        /*
         播放视频按钮
         */
        addRenderableWidget(
                Button.builder(
                                button6Text,
                                btn -> {
                                    menu.playVideo();
                                }
                        )
                        .bounds(leftMargin_l2, topMargin + 30, 100, 20)
                        .build()
        );

        /*
         暂停播放按钮
         */
        addRenderableWidget(
                Button.builder(
                                button7Text,
                                btn -> {
                                    menu.pauseVideo();
                                }
                        )
                        .bounds(leftMargin_l2, topMargin + 60, 100, 20)
                        .build()
        );

        /*
         上一帧按钮
         */
        addRenderableWidget(
                Button.builder(
                                button8Text,
                                btn -> {
                                    menu.renderFrameBack();
                                }
                        )
                        .bounds(leftMargin_l2, topMargin + 90, 100, 20)
                        .build()
        );

        /*
         下一帧按钮
         */
        addRenderableWidget(
                Button.builder(
                                button9Text,
                                btn -> {
                                    menu.renderFrameNext();
                                }
                        )
                        .bounds(leftMargin_l2, topMargin + 120, 100, 20)
                        .build()
        );

        /*
         重头开始播放按钮
         */
        addRenderableWidget(
                Button.builder(
                                button10Text,
                                btn -> {
                                    menu.renderFrameRestart();
                                }
                        )
                        .bounds(leftMargin_l2, topMargin + 150, 100, 20)
                        .build()
        );

        /**第三列按钮**/


        Component frameCountTitle = Component.translatable("gui.mcvan.video_control.frame_count_local");// 帧数标题
        addRenderableWidget(new StringWidget(
                leftMargin_l3,
                topMargin+30,
                100,
                20,
                frameCountTitle,
                font
        ));


        Component initialFrameCount = Component.literal("0");// 帧数值显示
        frameCountWidget = new StringWidget(
                leftMargin_l3+60,
                topMargin+30 ,
                100,
                20,
                initialFrameCount,
                font
        );
        frameCountWidget.setColor(0xFFFFFF); // 白色文本
        addRenderableWidget(frameCountWidget);


        Component currentUrlTitle = Component.translatable("gui.mcvan.video_control.current_url");// 菜单当前的url-----静态标题
        addRenderableWidget(new StringWidget(
                leftMargin_l3,
                topMargin+30+15,
                60,
                20,
                currentUrlTitle,
                font
        ));


        urlContentWidget = new StringWidget(// 菜单当前的url-----动态显示内容
                leftMargin_l3+60,
                topMargin+30+30 ,
                100,
                20,
                Component.literal("null"),
                font
        );
        urlContentWidget.setColor(0xFFFFFF); // 白色文本
        addRenderableWidget(urlContentWidget);


        addRenderableWidget(// 获取当前的menu的url
                Button.builder(
                                Component.literal("Refresh url"),
                                btn -> {
                                    this.my_url = menu.getUrl();
                                    urlContentWidget.setMessage(Component.literal(this.my_url));
                                }
                        )
                        .bounds(leftMargin_l3, topMargin+30+30+30, 100, 20)
                        .build()
        );


        Component refreshButtonText2 = Component.translatable("gui.mcvan.video_control.get_frame_count_local");// 获取帧数按钮(本地
        addRenderableWidget(
                Button.builder(
                                refreshButtonText2,
                                btn -> {
                                    // 点击时从客户端获取最新帧数(结果输出在菜单内)
                                    int count = 0;
                                    VideoData videoData = store.getVideoData(menu.getUrl());
                                    if(videoData instanceof VideoDataNull){
                                        count = -1;
                                    }
                                    else{
                                        count = videoData.getFrameCount();
                                    }
                                    frameCountWidget.setMessage(Component.literal(String.valueOf(count)));
                                }
                        )
                        .bounds(leftMargin_l3, topMargin+30+30+30+30, 100, 20)
                        .build()
        );


        Component refreshButtonText3 = Component.translatable("gui.mcvan.video_control.get_frame_count_server");// 获取帧数按钮
        addRenderableWidget(
                Button.builder(
                                refreshButtonText3,
                                btn -> {
                                    // 点击时从服务端获取最新帧数(结果输出在服务端的控制台)
                                    menu.sendGetVideoFrameCountToS(menu.getUrl());
                                }
                        )
                        .bounds(leftMargin_l3, topMargin+30+30+30+30+30, 100, 20)
                        .build()
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        /**
         * 背景渲染逻辑
         */
    }

    @Override
    public void containerTick() {
        super.containerTick();
        /**
         * 定期执行的一些逻辑
         */
    }
}