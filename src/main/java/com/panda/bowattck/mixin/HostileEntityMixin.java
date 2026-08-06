package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1. 将注入目标改为包含 initGoals 方法的父类 MobEntity
@Mixin(MobEntity.class)
public abstract class HostileEntityMixin {

    // 2. 使用 @Shadow 抓取 MobEntity 中受保护的 goalSelector 字段
    @Shadow protected GoalSelector goalSelector;

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void injectBowAttackGoal(CallbackInfo ci) {
        // 3. 判断当前生物到底是不是敌对生物 (防止给猪、牛、羊也发弓箭)
        if (((Object) this) instanceof HostileEntity mob) {
            
            // 装备弓箭
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

            // 清理近战 AI 并注入我们的虚弱弓箭 AI
            this.goalSelector.clear(goal -> goal instanceof MeleeAttackGoal);
            this.goalSelector.add(2, new UniversalBowAttackGoal(mob, 1.0D, 16.0F));
        }
    }
}
