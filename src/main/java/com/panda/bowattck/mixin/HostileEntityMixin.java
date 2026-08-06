package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
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
    
    // 新增：抓取目标选择器，用于给动物注入仇恨逻辑
    @Shadow @Final protected GoalSelector targetSelector;

    @Unique
    private boolean bowattck$initialized = false;

    protected HostileEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void injectBowAttackOnTick(CallbackInfo ci) {
        if (!this.getWorld().isClient && !this.bowattck$initialized) {
            this.bowattck$initialized = true;

            MobEntity mob = (MobEntity) (Object) this;
            Identifier entityId = Registries.ENTITY_TYPE.getId(mob.getType());

            // 严格验证：只对 Minecraft 原版生物生效
            if ("minecraft".equals(entityId.getNamespace())) {
                
                // 情况 1：原版敌对生物
                // 使用 instanceof 模式匹配，直接将强转后的对象命名为 hostile
                if (mob instanceof HostileEntity hostile) {
                    hostile.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    hostile.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    this.goalSelector.clear(goal -> 
                        goal instanceof MeleeAttackGoal || 
                        goal instanceof BowAttackGoal
                    );
                    
                    this.goalSelector.add(2, new UniversalBowAttackGoal(hostile, 1.0D, 16.0F));
                } 
                // 情况 2：原版动物生物 (羊、牛、猪、鸡等)
                // 使用 instanceof 模式匹配，直接将强转后的对象命名为 animal
                else if (mob instanceof AnimalEntity animal) {
                    animal.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    animal.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    // AnimalEntity 继承自 PathAwareEntity，所以直接传入 animal 完美符合语法要求
                    this.targetSelector.add(1, new RevengeGoal(animal));
                    
                    this.goalSelector.add(2, new UniversalBowAttackGoal(animal, 1.0D, 16.0F));
                }
            }
        }
    }

}
