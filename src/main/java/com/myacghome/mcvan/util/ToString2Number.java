package com.myacghome.mcvan.util;

import java.util.regex.Pattern;

public class ToString2Number{

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?");// 正则表达式验证数字格式
    public static Number ToString2Number(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null; // 空值处理
        }

        String trimmed = str.trim();


        if (!NUMBER_PATTERN.matcher(trimmed).matches()) {// 检查是否为有效数字格式
            return null; // 非数字格式
        }

        try {

            if (trimmed.contains(".") || trimmed.toLowerCase().contains("e")) {// 尝试转换为整数
                return Double.parseDouble(trimmed); // 浮点数或科学计数法
            } else {

                long longValue = Long.parseLong(trimmed);// 检查是否在整数范围内
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue; // 返回整数
                }
                return longValue; // 返回长整数
            }
        } catch (NumberFormatException e) {
            return null; // 转换失败
        }
    }
    public void test(String[] args) {// 测试用例
        String[] testCases = {
                "123",         // 整数
                "-456",        // 负整数
                "789.012",    // 浮点数
                "3.14159e2",  // 科学计数法 (314.159)
                "0xFF",       // 十六进制 (不可转换)
                "123abc",      // 包含字母 (不可转换)
                "  123  ",     // 带空格
                "",           // 空字符串
                null          // null
        };
        for (String test : testCases) {
            Number result = ToString2Number(test);
            System.out.printf("输入: '%s' \t 结果: %s \t 类型: %s%n",
                    test,
                    result,
                    (result != null) ? result.getClass().getSimpleName() : "null");
        }
    }
}