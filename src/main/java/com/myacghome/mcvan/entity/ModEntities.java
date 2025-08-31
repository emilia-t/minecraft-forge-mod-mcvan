package com.myacghome.mcvan.entity;

import com.myacghome.mcvan.IndexMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
/**CLASS
 describe: 全部实体的主注册类
 notepad: 无
 **/
@SuppressWarnings("all")
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, IndexMod.MOD_ID);

    /**
     * 注册实体--boss
     */
    public static final
    RegistryObject<EntityType<BossEntity>> BOSS = ENTITY_TYPES.register(
            "boss",
            ()->EntityType.Builder.<BossEntity>of(BossEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 3.0f) // 实体大小
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(IndexMod.MOD_ID, "boss").toString())
    );

    public static final
    RegistryObject<EntityType<BigArrowEntity>> BIG_ARROW_ENTITY = ENTITY_TYPES.register(
            "big_arrow",
            ()->EntityType.Builder.<BigArrowEntity>of(BigArrowEntity::new,MobCategory.MISC)
                    .sized(1.0F,1.0F)          // 长宽高各为 1 格
                    .clientTrackingRange(4)  // 客户端追踪距离
                    .updateInterval(20)          // 更新间隔
                    .build(new ResourceLocation(IndexMod.MODID,"big_arrow").toString())// 箭矢实体的注册注册名称需与参数一致
    );//注册实体箭矢的渲染器

    /**
     * 在根类中调用此方法进行所有实体的注册
     * @param eventBus
     */
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
