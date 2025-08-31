package com.myacghome.mcvan;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.NotNull;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**CLASS
 * 一些工具
 */
public final class ToolVan {
    private ToolVan() {}

    /**
     * 将消息输出到玩家聊天栏（仅客户端生效）
     * @param message 要显示的字符串消息
     */
    public static void logToChat(String message) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientHandler.sendToChat(message);
        }
    }

    /**
     * 发送带换行的消息
     **/
    public static void sendMultiLineMessage(CommandSourceStack source, String... lines) {
        MutableComponent message = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            message = message.append(Component.literal(lines[i]));
            if (i < lines.length - 1) {
                message = message.append(Component.literal("\n"));
            }
        }
        source.sendSystemMessage(message);
    }

    /**
     * 向所有玩家发送消息的实用方法
     **/
    public static void broadcastMessage(@NotNull Level level, String message) {
        if (!level.isClientSide && level.getServer() != null) {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(message),
                    false
            );
        }
    }

    /**function
     * 服务器发送消息
     */
    private static class ClientHandler {
        private static void sendToChat(String message) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.literal(message),
                        false
                );
            }
        }
    }

    /**function
     * 提取url中的资源名称
     */
    public static String extractResourceName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        int lastSlashIndex = url.lastIndexOf('/');// 查找最后一个'/'的位置
        if (lastSlashIndex == -1) {
            return url;// 如果没有找到斜杠，则返回整个字符串（或根据需求处理）
        }

        String lastPart = url.substring(lastSlashIndex + 1);// 截取最后一个'/'之后的部分

        if (lastPart.isEmpty()) {// 如果截取后为空字符串，直接返回
            return "";
        }

        int lastDotIndex = lastPart.lastIndexOf('.');// 查找最后一个'.'的位置
        if (lastDotIndex == -1) {
            return lastPart;// 如果没有扩展名，则返回整个部分
        }

        return lastPart.substring(0, lastDotIndex);// 截取最后一个'.'之前的部分（资源名称）
    }

    /**
     * 测试字符串是否匹配给定的正则表达式
     * @param inputString 要测试的字符串
     * @param regex 正则表达式
     * @return 如果字符串匹配正则表达式则返回true，否则返回false
     */
    public static boolean testRegex(String inputString, String regex) {
        Pattern pattern = Pattern.compile(regex);// 编译正则表达式
        Matcher matcher = pattern.matcher(inputString);// 创建匹配器
        return matcher.matches();// 返回匹配结果
    }

    public static boolean testRegex(String inputString, Pattern pattern) {
        Matcher matcher = pattern.matcher(inputString);// 创建匹配器
        return matcher.matches();// 返回匹配结果
    }

    /**
     * 测试字符串是否包含与正则表达式匹配的子串
     * @param inputString 要测试的字符串
     * @param regex 正则表达式
     * @return 如果字符串包含匹配的子串则返回true，否则返回false
     */
    public static boolean containsRegex(String inputString, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find();
    }

    public static boolean containsRegex(String inputString, Pattern pattern) {
        Matcher matcher = pattern.matcher(inputString);
        return matcher.find();
    }
}
/*
正则表达式示例用法
//public static void main(String[] args) {
//    // 示例用法
//    String testString = "Hello123";
//    String regex = "^[A-Za-z]+\\d+$"; // 匹配字母开头后跟数字
//
//    boolean isMatch = testRegex(testString, regex);
//    System.out.println("完全匹配: " + isMatch);
//
//    boolean containsMatch = containsRegex(testString, "\\d+");
//    System.out.println("包含数字: " + containsMatch);
//}
*/