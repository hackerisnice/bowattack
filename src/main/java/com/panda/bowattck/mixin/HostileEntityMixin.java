package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public abstract class HostileEntityMixin extends LivingEntity {

    @Shadow @Final protected GoalSelector goalSelector;

    // 增加一个标记，确保对每只怪物只修改一次，避免每 tick 都执行导致卡顿
    @Unique
    private boolean bowattck$initialized = false;

    protected HostileEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void injectBowAttackOnTick(CallbackInfo ci) {
        // 确保在服务端运行且仅初始化一次
        if (!this.getWorld().isClient && !this.bowattck$initialized) {
            this.bowattck$initialized = true;

            // 1. 判断是否是敌对生物 (HostileEntity)
            if (((Object) this) instanceof HostileEntity mob) {
                
                // 2. 获取该生物的注册 ID，判断是否为 Minecraft 原版生物
                Identifier entityId = Registries.ENTITY_TYPE.getId(mob.getType());
                
                // "minecraft" 是原版生物的命名空间，其他模组的生物命名空间是模组的 ID
                if ("minecraft".equals(entityId.getNamespace())) {
                    
                    // 强制给原版怪物主手拿弓，并阻止掉落
                    mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    mob.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    // 清除原版的近战 AI 以及原版的弓箭 AI（为了让小白也发射我们的虚弱箭矢）
                    this.goalSelector.clear(goal -> 
                        goal instanceof MeleeAttackGoal || 
                        goal instanceof BowAttackGoal
                    );
                    
                    // 注入我们编写的带有虚弱效果的通用弓箭 AI
                    this.goalSelector.add(2, new UniversalBowAttackGoal(mob, 1.0D, 16.0F));
                }
            }
        }
    }
}
