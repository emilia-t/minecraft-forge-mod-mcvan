package com.myacghome.mcvan.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.BlockPos;

/**
 * 指令JSON解析器
 * 示例JSON格式:
 * {
 *   "type": "command",
 *   "name": "play_video",
 *   "parameters": "--url https://example.com/video.mp4"
 * }
 */
public class InstructParser {
    private final String type;
    private final String name;
    private final String parameters;
    private final BlockPos pos;

    private InstructParser(String type, String name, String parameters, BlockPos pos) {
        this.type = type;
        this.name = name;
        this.parameters = parameters;
        this.pos = pos;
    }

    /**
     * 从JSON字符串创建指令对象
     * @param json JSON格式的指令字符串
     * @return 解析后的指令对象
     * @throws JsonSyntaxException 如果JSON格式无效
     * @throws IllegalArgumentException 如果缺少必需字段
     */
    public static InstructParser fromJson(String json) {
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();

            if (!jsonObject.has("type")) {// 验证必需字段
                throw new IllegalArgumentException("Missing required field: type");
            }
            if (!jsonObject.has("name")) {
                throw new IllegalArgumentException("Missing required field: name");
            }


            String type = jsonObject.get("type").getAsString();// 提取字段
            String name = jsonObject.get("name").getAsString();


            String parameters = jsonObject.has("parameters")// 参数字段可选
                    ? jsonObject.get("parameters").getAsString()
                    : "";

            return new InstructParser(type, name, parameters, null);
        } catch (JsonSyntaxException e) {
            throw new JsonSyntaxException("Invalid JSON format for command: " + json, e);
        }
    }

    /* ========== 字段访问器 ========== */

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getParameters() {
        return parameters;
    }

    /* ========== 参数解析方法 ========== */

    /**
     * 获取特定参数的短格式值（如 --url 值）
     * @param paramName 参数名（如 "url"）
     * @return 参数值，如果不存在则返回 null
     */
    public String getParamValue(String paramName) {
        return getParamValue(paramName, null);
    }

    /**
     * 获取特定参数的短格式值（如 --url 值）
     * @param paramName 参数名（如 "url"）
     * @param defaultValue 默认值
     * @return 参数值，如果不存在则返回默认值
     */
    public String getParamValue(String paramName, String defaultValue) {
        String prefix = "--" + paramName;
        if (parameters.contains(prefix)) {
            String[] parts = parameters.split("\\s+");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals(prefix) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }
        return defaultValue;
    }

    /**
     * 获取所有参数作为键值对
     * @return 参数键值对数组
     */
    public String[] getParamPairs() {
        return parameters.split("\\s+");
    }

    /**
     * 检查是否包含特定参数
     * @param paramName 参数名
     * @return 是否包含
     */
    public boolean hasParam(String paramName) {
        return parameters.contains("--" + paramName);
    }

    @Override
    public String toString() {
        return String.format("Command [type=%s, name=%s, parameters=%s]", type, name, parameters);
    }
}