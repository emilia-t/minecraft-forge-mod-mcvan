package com.myacghome.mcvan.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import net.minecraftforge.event.ForgeEventFactory;

/**CLASS
 * describe: 极致弓类
 * notepad: 无
 */
public class BigBowItem extends BowItem {
    /**
     * describe: 构造器
     * notepad: 无
     */
    public BigBowItem(Properties props) {
        super(props);
    }
    /**
     * describe: 拉弓事件处理
     * notepad: 无
     */
    @Override
    public void releaseUsing(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull LivingEntity pEntityLiving, int pTimeLeft){
        if (pEntityLiving instanceof Player player){
            Item BIG_ARROW_ITEM = ModItems.BIG_ARROW.get();// 直接引用已注册的物品
            ItemStack itemStack = player.getProjectile(pStack);// 获取玩家物品栏对象
            //boolean flag = player.getAbilities().instabuild;// 用于判断玩家是否启用了创造模式
            boolean flag = true;// 这里先暂时设为true
            boolean hasAmmo = true;
            boolean stackIsEmpty = itemStack.isEmpty();
            int charge = ForgeEventFactory.onArrowLoose(pStack, pLevel, player, (72000 - pTimeLeft), hasAmmo);
            if (charge < 0) {return;}// 判断拉弓的时长，如果太短则取消
            if (stackIsEmpty) {itemStack = new ItemStack(BIG_ARROW_ITEM);}// 当物品栏为空时设置物品栏为含有一个放大箭的物品栏
            float power = getPowerForTimeVan(charge);// 返回值(0,1]
            if ((double)power < 0.1D){return;}// 力量太弱
            boolean flag1 = player.getAbilities().instabuild ||  //判断是否为创造模式
                    itemStack.getItem() instanceof BigArrowItem; //判断是否有对应的箭
            if (!pLevel.isClientSide){// 服务端专用运行代码
                BigArrowItem arrowitem = (BigArrowItem)(itemStack.getItem() instanceof BigArrowItem ? itemStack.getItem() : BIG_ARROW_ITEM);// 从背包中取出箭
                AbstractArrow abstractarrow = arrowitem.createArrow(pLevel, itemStack, player);// 抽象箭转换为箭矢(实体)
                abstractarrow = customArrow(abstractarrow);
                abstractarrow.shootFromRotation(
                    player,               // 玩家实体
                    player.getXRot(),     // 玩家的X旋转角度 东西方向  决定了射击方向
                    player.getYRot(),     // 玩家的Y旋转角度 垂直方向  决定了射击方向
                    0.0F,
                    power * 3.0F,// 射速
                    1.0F                  // 最大偏移误差
                );
                if (power == 1.0F) {
                    abstractarrow.setCritArrow(true);// 设置重击
                }
//                力量附魔相关
//                int j = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, pStack);
//                if (j > 0) {
//                    abstractarrow.setBaseDamage(abstractarrow.getBaseDamage() + (double)j * 0.5D + 0.5D);
//                }
//                击退附魔相关
//                int k = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, pStack);
//                if (k > 0) {
//                    abstractarrow.setKnockback(k);
//                }
//                火焰附加
//                if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, pStack) > 0) {
//                    abstractarrow.setSecondsOnFire(100);
//                }
                pStack.hurtAndBreak(1, player, (p_289501_) -> {
                    p_289501_.broadcastBreakEvent(player.getUsedItemHand());
                });
//                创造模式拾取相关
//                if (flag1 || player.getAbilities().instabuild && (itemStack.is(Items.SPECTRAL_ARROW) || itemStack.is(Items.TIPPED_ARROW))) {
//                    abstractarrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
//                }
                pLevel.addFreshEntity(abstractarrow);//召唤实体箭矢
            }
            pLevel.playSound(//播放射击音效
                (Player)null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ARROW_SHOOT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F / (pLevel.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F
            );
            if (!flag1 && !player.getAbilities().instabuild){
                itemStack.shrink(1);
                if (itemStack.isEmpty()) {
                    player.getInventory().removeItem(itemStack);
                }
            }
            //player.awardStat(Stats.ITEM_USED.get(this));
        }
    }
    /**
      * @param pCharge 拉弓的时长
      * @return 返回力量数值 数值范围 (0,1]
      */
    public static float getPowerForTimeVan(int pCharge){
        float f = (float)pCharge / 20.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }
        return f;
    }
}
