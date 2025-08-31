package com.myacghome.mcvan.network;

import com.myacghome.mcvan.util.VideoData;
import net.minecraft.core.BlockPos;
import com.google.gson.JsonObject;
/**
 * 指令JSON创建器
 * 示例JSON格式:
 * {
 *   "type": "command",
 *   "name": "play_video",
 *   "parameters": "--url https://example.com/video.mp4"
 * }
 *
 * 此处创建后需要在InstructPacket中的handleServerCommand或handleClientCommand中添加处理逻辑
 *
 */
public class InstructCreate {
    private final String type;
    private final String name;
    private final String parameters;
    private final BlockPos pos;
    private static final String p_type = "type";
    private static final String p_command = "command";
    private static final String p_name = "name";
    private static final String p_parameters = "parameters";
    public static final String load_video = "load_video";
    public static final String get_video_frame_count = "get_video_frame_count";

    private InstructCreate(String type, String name, String parameters, BlockPos pos) {
        this.type = type;
        this.name = name;
        this.parameters = parameters;
        this.pos = pos;
    }
    /*
     客户端向服务端发送的指令 （c2s:Client to server）
     加载视频
     checked
     */
    public static String c2sLoadVideo(String url, int posX , int posY, int posZ){
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty(p_type, p_command);
        jsonObj.addProperty(p_name, load_video);
        jsonObj.addProperty(
                p_parameters,
                "--url "+url+" --posX "+posX+" --posY "+posY+" --posZ "+posZ
        );
        return jsonObj.toString();
    }
    /*
     客户端向服务端发送的指令 （c2s:Client to server）
     获取某视频总帧数（从store)
     checked
     */
    public static String c2sGetVideoFrameCount(String url){
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty(p_type, p_command);
        jsonObj.addProperty(p_name, get_video_frame_count);
        jsonObj.addProperty(
                p_parameters,
                "--url "+url
        );
        return jsonObj.toString();
    }
    /*
     服务端向客户端发送的指令 （s2c:Server to client）
     获取某视频总帧数（从store)
     */
    public static String s2cGetVideoFrameCount(String url){
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty(p_type, p_command);
        jsonObj.addProperty(p_name, get_video_frame_count);
        jsonObj.addProperty(
                p_parameters,
                "--url "+url
        );
        return jsonObj.toString();
    }
}
