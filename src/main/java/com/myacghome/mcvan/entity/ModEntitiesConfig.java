package com.myacghome.mcvan.entity;

import java.util.HashMap;
/**CLASS
 describe: 实体的可配置参数
 notepad: 无
 **/
public class ModEntitiesConfig {
    public static HashMap<String, Object> BOSS_INFO = new HashMap<>();
    static {
        BOSS_INFO.put("health", Float.MAX_VALUE);//生命值
        BOSS_INFO.put("attackDamage", 100.0F);//攻击力
        BOSS_INFO.put("movementSpeed", 0.25F);//移动速度
        BOSS_INFO.put("followRange", 40.0F);//跟随距离、反击距离
        BOSS_INFO.put("armor", 5.0F);// 盔甲值
        BOSS_INFO.put("width", 1.0F);// 宽度
        BOSS_INFO.put("height", 2.0F);// 高度
        BOSS_INFO.put("knockbackResistance", 5.0F);//抗冲击性
    }
}