package com.myacghome.mcvan.util;
/*
file name 包含以下字段：
1.原始分辨率x（整数）
2.原始分辨率y（整数）
3.url（url中不得使用unicode以外的字符，例如中文)
4.方块宽度width（整数）
5.方块高度height（整数）

*注意:每个字段使用&符号隔开*
*注意:如果要增加新字段必须在末尾增加*

以下是特殊字符对应的转义字符串（使用1个倒V形的插入符加上1个小写字母来表示）
^ --> ^a
& --> ^b
\ --> ^c
/ --> ^d
: --> ^e
* --> ^f
? --> ^g
" --> ^h
< --> ^i
> --> ^j
| --> ^k
 */
public class ToVideoProperty2FileName {
    // 特殊字符转义映射
    private static final String[][] ESCAPE_MAP = {
            {"^", "^a"},
            {"&", "^b"},
            {"\\","^c"},
            {"/", "^d"},
            {":", "^e"},
            {"*", "^f"},
            {"?", "^g"},
            {"\"","^h"},
            {"<", "^i"},
            {">", "^j"},
            {"|", "^k"}
    };
    /**
     * 将视频属性转换为合法的文件名
     *
     * @param url 输入的 URL
     * @return 转换后的文件名
     * 返回值示例:256&144&https^e^d^dxxx.com^dvideo_name.mp4&4&3
     */
    public static String VideoProperty2FileName(int resolutionX,int resolutionY,String url,byte widthBlocks,byte heightBlocks) {

        String[] parts = {// 转换整数参数为字符串
                String.valueOf(resolutionX),
                String.valueOf(resolutionY),
                url,
                String.valueOf(widthBlocks),
                String.valueOf(heightBlocks)
        };


        for (int i = 0; i < parts.length; i++) {// 对每个部分进行转义处理
            parts[i] = escapeString(parts[i]);
        }


        return String.join("&", parts) + ".vd";// 用&符号连接所有部分
    }
    /**
     * 从文件名还原视频属性
     *
     * @param fileName 文件名
     * @return video property array 整数会变为字符串类型的数字
     * 返回值示例:["256","144","https://xxx.com/video_name.mp4","4","3"]
     */
    public static String[] FileName2VideoProperty(String fileName) {

        if (fileName.endsWith(".vd")) {// 检查并移除.vd后缀
            fileName = fileName.substring(0, fileName.length() - 3);
        } else {
            throw new IllegalArgumentException("Invalid file name format: missing .vd extension");
        }
        String[] parts = fileName.split("&"); // 限制分割5部分

        for (int i = 0; i < parts.length; i++) {// 对每个部分进行反转义处理
            parts[i] = unescapeString(parts[i]);
        }
        return parts;
    }

    private static String escapeString(String input) {// 转义字符串中的特殊字符
        for (String[] mapping : ESCAPE_MAP) {
            input = input.replace(mapping[0], mapping[1]);
        }
        return input;
    }


    private static String unescapeString(String input) {// 反转义字符串中的特殊序列
        for (int i = ESCAPE_MAP.length - 1; i >= 0; i--) {// 倒序遍历映射表，优先处理长的转义序列
            input = input.replace(ESCAPE_MAP[i][1], ESCAPE_MAP[i][0]);
        }
        return input;
    }
}