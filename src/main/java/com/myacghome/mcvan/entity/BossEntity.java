package com.myacghome.mcvan.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.entity.LivingEntity;
import static com.myacghome.mcvan.entity.ModEntitiesConfig.BOSS_INFO;

/**CLASS
 describe: boss实体类
 notepad: 无
 **/
public class BossEntity extends Monster {
    public BossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setHealth((float)BOSS_INFO.get("health")); // 初始血量
        this.refreshDimensions(); // 确保尺寸更新

    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, (float)BOSS_INFO.get("health"))
            .add(Attributes.ATTACK_DAMAGE, (float)BOSS_INFO.get("attackDamage"))
            .add(Attributes.MOVEMENT_SPEED, (float)BOSS_INFO.get("movementSpeed"))
            .add(Attributes.FOLLOW_RANGE, (float)BOSS_INFO.get("followRange"))
            .add(Attributes.ARMOR, (float)BOSS_INFO.get("armor"))
            .add(Attributes.KNOCKBACK_RESISTANCE, (float)BOSS_INFO.get("knockbackResistance"))
            ;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(// 从配置读取尺寸（需先在ModEntitiesConfig添加width/height）
                (float) BOSS_INFO.get("width"),
                (float) BOSS_INFO.get("height")
        );
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean success = target.hurt(
                this.damageSources().genericKill(),
                (float)BOSS_INFO.get("attackDamage")
        );
        if (success) {
            this.doEnchantDamageEffects(this, target);
        }
        return success;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, LivingEntity.class, 10.0f));//寻找玩家
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8));
//        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this)); // 优先响应被攻击事件
//        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true)); // 追踪玩家
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                LivingEntity.class, // 目标改为所有活体实体
                10,                  // 检测范围（与followRange一致）
                true,               // 必须可见
                false,              // 不检查友好性
                entity ->
                        !(entity instanceof BossEntity) // 排除自身类型
                                && entity.isAlive()             // 只攻击存活目标
        ));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD; // 可选类型
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
}

