package com.myacghome.mcvan.entity;

import com.google.common.collect.Lists;
import com.myacghome.mcvan.ToolVan;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**CLASS
 describe: 自定义箭矢实体类||Custom arrow entity class
 notepad: 箭矢实体类继承自 AbstractArrow，并重写了 onHitEntity() 方法，用于处理箭矢击中实体时的事件。
**/
public class BigArrowEntity extends AbstractArrow {
    private static final double HEAD_DISTANCE=0.5D;
    private static final float DAMAGE=10.0F;
    @Nullable
    private IntOpenHashSet piercingIgnoreEntityIds;
    @Nullable
    private List<Entity> piercedAndKilledEntities;
    private final ItemStack tridentItem = new ItemStack(Items.TRIDENT);
    /**
     * describe: 主构造函数（由弓生成时调用）
     */
    public BigArrowEntity(Level level, LivingEntity shooter) {
        this(ModEntities.BIG_ARROW_ENTITY.get(), level);// 使用自定义 EntityType
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY(), shooter.getZ());
    }
    /**
     * describe: 必须的构造函数（用于实体加载）
     */
    public BigArrowEntity(EntityType<? extends BigArrowEntity> entityType, Level level) {
        super(entityType, level);
        this.setBaseDamage(5.0);// 设置伤害值为 5 颗心
        this.setNoGravity(false);// 受重力影响
        //this.setPierceLevel((byte) 127); // 防止穿透消失
        this.setShotFromCrossbow(true); // 标记为射出自弓
    }

    @Override
    protected void tickDespawn(){
        super.tickDespawn();
    }

    /**
     * describe: 捡起箭矢时触发的事件||The event triggered when the arrow is picked up
     */
    @Override
    protected ItemStack getPickupItem() {
        return this.tridentItem.copy();
    }
    /**
     * describe: 箭实体的主循环
     */
    @Override
    public void tick(){
        if(this.getPersistentData().getBoolean("ShouldSpawnNewArrow")){
            UUID targetUUID = this.getPersistentData().getUUID("ShouldSpawnNewArrowUUID");//受击目标id
            ServerLevel serverLevel = (ServerLevel) this.level();
            Entity target = serverLevel.getEntity(targetUUID);//获取受击实体
            Entity ownerEntity = this.getOwner();//获取玩家
            if(target instanceof LivingEntity livingEntity){// 检查目标是存活
                if(livingEntity.getHealth()<=0){
                    this.discard();
                    return;
                }
            }
            if (target == null || target.isRemoved() || !target.isAlive()) {// 检查目标是否有效
                this.discard();
                return; // 直接返回，避免后续逻辑执行
            }
            this.setPos(target.getX(), target.getY(), target.getZ());// 同步位置和旋转
            this.setYRot(target.getYRot());
            this.setXRot(target.getXRot());
            if (ownerEntity instanceof LivingEntity) {
                LivingEntity owner = (LivingEntity) ownerEntity;
                if (target != null && target.isAlive() && !target.isRemoved()) {
                    BigArrowEntity newArrow = new BigArrowEntity(serverLevel, owner);// 在目标头上n处生成新箭矢
                    double yPos = target.getY() + target.getBbHeight() + HEAD_DISTANCE;
                    newArrow.setPos(target.getX(), yPos, target.getZ());
                    serverLevel.addFreshEntity(newArrow);// 生成新箭矢的逻辑
                    ////////看看灵梦是什么类型
                    ToolVan.logToChat("[BigArrowEntity]实体类型：");
                    ToolVan.logToChat(target.getClass().toString());
                }
            }
            this.getPersistentData().putBoolean("ShouldSpawnNewArrow", false);//移除属性
        }
        super.tick();
    }
    /**FUNCTION
     * describe: 箭矢击中实体时触发的事件||The event triggered when an arrow hits a solid object
     * param: result: EntityHitResult 实体击中结果||Entity hit result
     * return: void
     */
    @Override
    protected void onHitEntity(EntityHitResult pResult){
        EntityHitResult result = pResult;//名称有点区别
        Entity target = result.getEntity();//受击目标
        Entity owner = this.getOwner();//玩家
        DamageSource genericKillSource;// 创建genericKill伤害源
        if(owner==null){
            genericKillSource = this.damageSources().genericKill();
        }
        else{
            genericKillSource = this.damageSources().indirectMagic(this, owner);
        }
        if(target instanceof LivingEntity){
            if (target.hurt(genericKillSource, DAMAGE)){// 应用genericKill伤害
                /**
                 * describe: 将箭矢绑定到目标实体
                 * notepad: 箭矢实体的绑定逻辑，将箭矢绑定到目标实体，并禁用物理、重力、移动，并同步位置、旋转。
                 */
                this.setNoPhysics(true); // 禁用物理
                this.setNoGravity(true); // 禁用重力
                this.setDeltaMovement(Vec3.ZERO); // 停止移动
                this.setPos(target.getX(), target.getY(), target.getZ()); // 初始位置同步
                this.setYRot(target.getYRot()); // 同步旋转（可选）
                this.setXRot(target.getXRot());//
                /**
                 * describe: 标记箭矢需要后续增加箭矢
                 * notepad: 标记箭矢要后续增加箭矢，并记录绑定目标的UUID。
                 */
                this.getPersistentData().putBoolean("ShouldSpawnNewArrow", true);
                this.getPersistentData().putUUID("ShouldSpawnNewArrowUUID", target.getUUID());
            }
            String infoStr="[GenericKill伤害调试] 由 {";
            infoStr+=(owner != null) ? owner.getName().getString() : "未知来源";
            infoStr+="} 发射的箭矢在 ";
            infoStr+=target.getName().getString();
            infoStr+=" 造成了伤害:";
            infoStr+=DAMAGE;
            System.out.println(infoStr);
        }
        /**
         * 下面是原版的逻辑代码
         */
        Entity entity = pResult.getEntity();
        float f = (float)this.getDeltaMovement().length();
        int i = Mth.ceil(Mth.clamp((double)f * this.getBaseDamage(), 0.0D, (double)Integer.MAX_VALUE));
        if (this.getPierceLevel() > 0) {
            if (this.piercingIgnoreEntityIds == null) {
                this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
            }

            if (this.piercedAndKilledEntities == null) {
                this.piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
            }

            if (this.piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
                //this.discard();
                return;
            }

            this.piercingIgnoreEntityIds.add(entity.getId());
        }

        if (this.isCritArrow()) {
            long j = (long)this.random.nextInt(i / 2 + 2);
            i = (int)Math.min(j + (long)i, 2147483647L);
        }

        Entity entity1 = this.getOwner();
        DamageSource damagesource;
        if (entity1 == null) {
            damagesource = this.damageSources().arrow(this, this);
        } else {
            damagesource = this.damageSources().arrow(this, entity1);
            if (entity1 instanceof LivingEntity) {
                ((LivingEntity)entity1).setLastHurtMob(entity);
            }
        }

        boolean flag = entity.getType() == EntityType.ENDERMAN;
        int k = entity.getRemainingFireTicks();
        if (this.isOnFire() && !flag) {
            entity.setSecondsOnFire(5);
        }

        if (entity.hurt(damagesource, (float)i)) {
            if (flag) {
                return;
            }

            if (entity instanceof LivingEntity) {
                LivingEntity livingentity = (LivingEntity)entity;
                if (!this.level().isClientSide && this.getPierceLevel() <= 0) {
                    livingentity.setArrowCount(livingentity.getArrowCount() + 1);
                }

                if (this.getKnockback() > 0) {
                    double d0 = Math.max(0.0D, 1.0D - livingentity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    Vec3 vec3 = this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)this.getKnockback() * 0.6D * d0);
                    if (vec3.lengthSqr() > 0.0D) {
                        livingentity.push(vec3.x, 0.1D, vec3.z);
                    }
                }

                if (!this.level().isClientSide && entity1 instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(livingentity, entity1);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity)entity1, livingentity);
                }

                this.doPostHurtEffects(livingentity);
                if (entity1 != null && livingentity != entity1 && livingentity instanceof Player && entity1 instanceof ServerPlayer && !this.isSilent()) {
                    ((ServerPlayer)entity1).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                }

                if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
                    this.piercedAndKilledEntities.add(livingentity);
                }

                if (!this.level().isClientSide && entity1 instanceof ServerPlayer) {
                    ServerPlayer serverplayer = (ServerPlayer)entity1;
                    if (this.piercedAndKilledEntities != null && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, this.piercedAndKilledEntities);
                    } else if (!entity.isAlive() && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, Arrays.asList(entity));
                    }
                }
            }

            this.playSound(this.getHitGroundSoundEvent(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (this.getPierceLevel() <= 0) {
                //this.discard();
            }
        } else {
            entity.setRemainingFireTicks(k);
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1D));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;
            if (!this.level().isClientSide && this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                if (this.pickup == AbstractArrow.Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }

                //this.discard();
            }
        }
    }

    /**CLASS
     * describe: 极致箭矢渲染器
     * notepad: 无
     */
    @SuppressWarnings("all")
    public static class BigArrowRenderer extends ArrowRenderer<BigArrowEntity> {
        public static final ResourceLocation TEXTURE = new ResourceLocation("minecraft","textures/entity/projectiles/arrow.png");//只会影响输出去的实体箭矢的材质
        public BigArrowRenderer(EntityRendererProvider.Context context) {
            super(context);
        }
        @Override
        public ResourceLocation getTextureLocation(BigArrowEntity entity) {
            return TEXTURE;//返回自定义纹理路径
        }
    }
}
