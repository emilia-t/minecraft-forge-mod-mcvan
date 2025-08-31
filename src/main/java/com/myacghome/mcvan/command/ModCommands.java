package com.myacghome.mcvan.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.myacghome.mcvan.IndexMod;
import com.myacghome.mcvan.wallpaper.WallpaperSystemClient;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.myacghome.mcvan.ToolVan;

import java.util.ArrayList;

import static com.myacghome.mcvan.wallpaper.WallpaperSystemClient.wallpaperSystemClient;

@Mod.EventBusSubscriber(modid = IndexMod.MODID)
public class ModCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        /*-- 帮助指令 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "Help")
                        .executes(context -> {
                            ToolVan.sendMultiLineMessage(context.getSource(),
                                    "===== mcvan 模组指令帮助 =====",
                                    "/mcvanVersion - 查看模组版本",
                                    "/mcvanWallpaperStatus - 查看壁纸系统状态",
                                    "/mcvanSetWallpaper <URL> - 设置新壁纸",
                                    "/mcvanRemoveWallpaper <wid> - 移除壁纸",
                                    "/mcvanReplaceWallpaper <wid> <newUrl> - 替换壁纸",
                                    "/mcvanListWallpapers - 列出所有壁纸",
                                    "/mcvanSaveRandomFrameForWid <wid> - 保存壁纸的随机一帧",
                                    "/mcvanFreezeWallpaperForWid <wid> - 冻结某壁纸",
                                    "/mcvanResumeWallpaperForWid <wid> - 恢复冻结的壁纸"
                            );
                            return 1;
                        })
        );

        /*-- 版本查询指令 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "Version")
                        .executes(context -> {
                            ToolVan.sendMultiLineMessage(context.getSource(),
                                    "===== mcvan 模组信息 =====",
                                    "模组版本: " + IndexMod.MOD_VERSION,
                                    "版权申明: All rights reserved."
                            );
                            return 1;
                        })
        );

        /*-- 壁纸系统状态 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "WallpaperStatus")
                        .executes(context -> {
                            ToolVan.sendMultiLineMessage(context.getSource(),
                                    "===== 壁纸系统状态 =====",
                                    "使用槽位: " + wallpaperSystemClient.WallpapersUsageCount() + "/" + WallpaperSystemClient.MAX_WALLPAPERS_COUNT,
                                    "输入 /mcvanHelp 获取帮助"
                            );
                            return 1;
                        })
        );

        /*-- 设置壁纸 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "SetWallpaper")
                        .then(Commands.argument("URL", StringArgumentType.string())
                                .executes(context -> {
                                    String url = StringArgumentType.getString(context, "URL");
                                    boolean success = wallpaperSystemClient.setWallpaper(url);

                                    if(success) {
                                        ToolVan.sendMultiLineMessage(context.getSource(),
                                                "壁纸设置成功！",
                                                "URL: " + url,
                                                "输入 /mcvanWallpaperStatus 查看状态"
                                        );
                                    } else {
                                        ToolVan.logToChat("壁纸设置失败：没有可用槽位！");
                                    }
                                    return 1;
                                })
                        )
        );

        /*-- 移除壁纸 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "RemoveWallpaper")
                        .then(Commands.argument("wid", IntegerArgumentType.integer(0, WallpaperSystemClient.MAX_WALLPAPERS_COUNT - 1))
                                .executes(context -> {
                                    int wid = IntegerArgumentType.getInteger(context, "wid");
                                    boolean success = wallpaperSystemClient.removeWallpaper(wid);

                                    if(success) {
                                        ToolVan.sendMultiLineMessage(context.getSource(),
                                                "壁纸移除成功！",
                                                "ID: " + wid,
                                                "输入 /mcvanListWallpapers 查看剩余壁纸"
                                        );
                                    } else {
                                        ToolVan.logToChat("壁纸移除失败：该槽位未使用或ID无效！");
                                    }
                                    return 1;
                                })
                        )
        );

        /*-- 替换壁纸的url --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "ReplaceWallpaper")
                        .then(Commands.argument("wid", IntegerArgumentType.integer(0, WallpaperSystemClient.MAX_WALLPAPERS_COUNT - 1))
                                .then(Commands.argument("newUrl", StringArgumentType.string())
                                        .executes(context -> {
                                            int wid = IntegerArgumentType.getInteger(context, "wid");
                                            String newUrl = StringArgumentType.getString(context, "newUrl");
                                            boolean success = wallpaperSystemClient.replaceWallpaper(wid, newUrl);

                                            if (success) {
                                                ToolVan.sendMultiLineMessage(context.getSource(),
                                                        "壁纸替换成功！",
                                                        "ID: " + wid,
                                                        "输入 /mcvanListWallpapers 查看剩余壁纸"
                                                );
                                            } else {
                                                ToolVan.logToChat("壁纸替换失败：该槽位未使用或ID无效！");
                                            }
                                            return 1;
                                        })
                                ))
        );

        /*-- 列出所有壁纸 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "ListWallpapers")
                        .executes(context -> {
                            ArrayList<String> wallpapers = wallpaperSystemClient.WallpapersToString();
                            String[] lines = new String[wallpapers.size() + 1];
                            lines[0] = "===== 所有壁纸列表 =====";

                            for(int i = 0; i < wallpapers.size(); i++) {
                                lines[i + 1] = wallpapers.get(i);
                            }

                            ToolVan.sendMultiLineMessage(context.getSource(), lines);
                            return 1;
                        })
        );

        /*-- 随机保存壁纸帧 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "SaveRandomFrameForWid")
                        .then(Commands.argument("wid", IntegerArgumentType.integer(0, WallpaperSystemClient.MAX_WALLPAPERS_COUNT - 1))
                                .executes(ctx -> {
                                    int wid = IntegerArgumentType.getInteger(ctx, "wid");
                                    boolean success = wallpaperSystemClient.saveRandomFrameForWid(wid);

                                    if (success) {
                                        ToolVan.sendMultiLineMessage(ctx.getSource(),
                                                "壁纸帧保存成功！",
                                                "槽位ID: " + wid,
                                                "截图已保存到 .minecraft/screenshots/wallpaper_frames/"
                                        );
                                    } else {
                                        ToolVan.logToChat("壁纸帧保存失败：无效ID或未加载壁纸！");
                                    }
                                    return 1;
                                })
                        )
        );

        /*-- 冻结某壁纸 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "FreezeWallpaperForWid")
                        .then(Commands.argument("wid", IntegerArgumentType.integer(0, WallpaperSystemClient.MAX_WALLPAPERS_COUNT - 1))
                                .executes(ctx -> {
                                    int wid = IntegerArgumentType.getInteger(ctx, "wid");
                                    boolean success = wallpaperSystemClient.freezeWallpaperForWid(wid);

                                    if (success) {
                                        ToolVan.sendMultiLineMessage(ctx.getSource(),
                                                "壁纸已冻结！",
                                                "槽位ID: " + wid
                                        );
                                    } else {
                                        ToolVan.logToChat("壁纸冻结失败：无效ID或未加载壁纸！");
                                    }
                                    return 1;
                                })
                        )
        );

        /*-- 恢复冻结的壁纸 --*/
        event.getDispatcher().register(
                Commands.literal(IndexMod.MODID + "ResumeWallpaperForWid")
                        .then(Commands.argument("wid", IntegerArgumentType.integer(0, WallpaperSystemClient.MAX_WALLPAPERS_COUNT - 1))
                                .executes(ctx -> {
                                    int wid = IntegerArgumentType.getInteger(ctx, "wid");
                                    boolean success = wallpaperSystemClient.resumeWallpaperForWid(wid);

                                    if (success) {
                                        ToolVan.sendMultiLineMessage(ctx.getSource(),
                                                "壁纸已恢复播放！",
                                                "槽位ID: " + wid
                                        );
                                    } else {
                                        ToolVan.logToChat("壁纸恢复失败：无效ID、未冻结或未加载壁纸！");
                                    }
                                    return 1;
                                })
                        )
        );
    }
}