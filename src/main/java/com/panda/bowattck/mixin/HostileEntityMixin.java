package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 1. 继承 PathAwareEntity (HostileEntity 的父类)，这样就能直接访问 protected 字段
@Mixin(HostileEntity.class)
public abstract class HostileEntityMixin extends PathAwareEntity {

    // 2. Mixin 需要一个伪造的构造函数来满足 Java 编译器的父类继承规则（这部分在运行时会被忽略）
    protected HostileEntityMixin(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initGoals", at = @At("TAIL"))
    private void injectBowAttackGoal(CallbackInfo ci) {
        // 直接使用 this.equipStack，因为我们已经继承了该类
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f); 

        // 3. 修复 1.21 的语法：goal 直接就是目标实例，不需要 getGoal()
        this.goalSelector.clear(goal -> goal instanceof MeleeAttackGoal);

        // 4. 将 this 强转为 HostileEntity 并传入我们的弓箭 AI 中
        this.goalSelector.add(2, new UniversalBowAttackGoal((HostileEntity) (Object) this, 1.0D, 16.0F));
    }
}
