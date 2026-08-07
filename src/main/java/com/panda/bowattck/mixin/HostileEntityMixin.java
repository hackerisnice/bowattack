package com.panda.bowattck.mixin;

import com.panda.bowattck.ai.UniversalBowAttackGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.BowAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.effect.StatusEffects;
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

            if ("minecraft".equals(entityId.getNamespace())) {
                
                // --- 怪物逻辑 ---
                if (mob instanceof HostileEntity hostile) {
                    hostile.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    hostile.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    this.goalSelector.clear(goal -> 
                        goal instanceof MeleeAttackGoal || 
                        goal instanceof BowAttackGoal
                    );
                    
                    this.goalSelector.add(4, new UniversalBowAttackGoal(
                            hostile, 1.0D, 16.0F, 
                            30, 50,  
                            0.0F, // 自瞄级精准
                            StatusEffects.MINING_FATIGUE, // 挖掘疲劳
                            StatusEffects.WEAKNESS        // 叠加虚弱效果
                    ));
                } 
                // --- 动物与中立生物逻辑 ---
                else if (mob instanceof AnimalEntity animal) {
                    animal.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    animal.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    // .setGroupRevenge() 会自动呼叫周围同种类的生物（同为羊、同为牛等）一起攻击
                    this.targetSelector.add(1, new RevengeGoal(animal).setGroupRevenge());
                    
                    this.goalSelector.add(4, new UniversalBowAttackGoal(
                            animal, 1.0D, 16.0F, 
                            5, 10,   
                            0.0F, // 与怪物同等最高的零误差自瞄
                            StatusEffects.SLOWNESS // 缓慢效果
                    ));
                }
            }
        }
    }
}
