package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HostileEntity.class)
public abstract class HostileEntityMixin {

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void injectBowAttackGoal(CallbackInfo ci) {
        HostileEntity mob = (HostileEntity) (Object) this;

        // 强制主手拿弓，并设置掉落率为 0 以免破坏原版经济系统
        mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f); 

        // 移除原版近战 AI，防止发生贴脸时怪物发呆或行为冲突
        mob.goalSelector.clear(goal -> goal.getGoal() instanceof MeleeAttackGoal);

        // 注入我们编写的弓箭 AI
        mob.goalSelector.add(2, new UniversalBowAttackGoal(mob, 1.0D, 16.0F));
    }
}
