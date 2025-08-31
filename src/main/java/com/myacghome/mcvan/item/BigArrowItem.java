package com.myacghome.mcvan.item;

import com.myacghome.mcvan.entity.BigArrowEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.SoundAction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.function.BiPredicate;

/**CLASS
 * describe: 极致箭类
 * notepad: 无
 */
@SuppressWarnings("all")
public class BigArrowItem extends ArrowItem {
    public BigArrowItem(Properties props) {
        super(props);
    }
    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter) {
        return new BigArrowEntity(level, shooter) {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
                return super.getCapability(cap);
            }

            @Override
            public void deserializeNBT(CompoundTag nbt) {
                super.deserializeNBT(nbt);
            }

            @Override
            public CompoundTag serializeNBT() {
                return super.serializeNBT();
            }

            @Override
            public boolean shouldRiderSit() {
                return super.shouldRiderSit();
            }

            @Override
            public ItemStack getPickedResult(HitResult target) {
                return super.getPickedResult(target);
            }

            @Override
            public boolean canRiderInteract() {
                return super.canRiderInteract();
            }

            @Override
            public boolean canBeRiddenUnderFluidType(FluidType type, Entity rider) {
                return super.canBeRiddenUnderFluidType(type, rider);
            }

            @Override
            public MobCategory getClassification(boolean forSpawnCount) {
                return super.getClassification(forSpawnCount);
            }

            @Override
            public boolean isMultipartEntity() {
                return super.isMultipartEntity();
            }

            @Override
            public @Nullable PartEntity<?>[] getParts() {
                return super.getParts();
            }

            @Override
            public float getStepHeight() {
                return super.getStepHeight();
            }

            @Override
            public boolean isInFluidType(FluidState state) {
                return super.isInFluidType(state);
            }

            @Override
            public boolean isInFluidType(FluidType type) {
                return super.isInFluidType(type);
            }

            @Override
            public boolean isInFluidType(BiPredicate<FluidType, Double> predicate) {
                return super.isInFluidType(predicate);
            }

            @Override
            public boolean isEyeInFluidType(FluidType type) {
                return super.isEyeInFluidType(type);
            }

            @Override
            public boolean canStartSwimming() {
                return super.canStartSwimming();
            }

            @Override
            public double getFluidMotionScale(FluidType type) {
                return super.getFluidMotionScale(type);
            }

            @Override
            public boolean isPushedByFluid(FluidType type) {
                return super.isPushedByFluid(type);
            }

            @Override
            public boolean canSwimInFluidType(FluidType type) {
                return super.canSwimInFluidType(type);
            }

            @Override
            public boolean canFluidExtinguish(FluidType type) {
                return super.canFluidExtinguish(type);
            }

            @Override
            public float getFluidFallDistanceModifier(FluidType type) {
                return super.getFluidFallDistanceModifier(type);
            }

            @Override
            public boolean canHydrateInFluidType(FluidType type) {
                return super.canHydrateInFluidType(type);
            }

            @Override
            public @Nullable SoundEvent getSoundFromFluidType(FluidType type, SoundAction action) {
                return super.getSoundFromFluidType(type, action);
            }

            @Override
            public boolean hasCustomOutlineRendering(Player player) {
                return super.hasCustomOutlineRendering(player);
            }

            @Override
            public float getEyeHeightForge(Pose pose, EntityDimensions size) {
                return super.getEyeHeightForge(pose, size);
            }

            @Override
            public boolean shouldUpdateFluidWhileBoating(FluidState state, Boat boat) {
                return super.shouldUpdateFluidWhileBoating(state, boat);
            }

            @Override
            public boolean alwaysAccepts() {
                return super.alwaysAccepts();
            }

            @Override
            protected ItemStack getPickupItem() {
                return null;
            }
        };
    }
}
