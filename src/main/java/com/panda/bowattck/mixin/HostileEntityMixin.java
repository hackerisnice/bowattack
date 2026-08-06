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
                
                // --- 怪物逻辑 (极高准度，附加挖掘疲劳) ---
                if (mob instanceof HostileEntity hostile) {
                    hostile.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    hostile.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    this.goalSelector.clear(goal -> 
                        goal instanceof MeleeAttackGoal || 
                        goal instanceof BowAttackGoal
                    );
                    
                    // 优先级设为 4：为原版高优先级 AI(如苦力怕自爆) 让路
                    this.goalSelector.add(4, new UniversalBowAttackGoal(
                            hostile, 1.0D, 16.0F, 
                            30, 50,  // 普通攻击间隔
                            0.0F,    // 0 误差，自瞄级精准
                            StatusEffects.MINING_FATIGUE // 挖掘疲劳
                    ));
                } 
                // --- 动物逻辑 (极短间隔，附加缓慢效果) ---
                else if (mob instanceof AnimalEntity animal) {
                    animal.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
                    animal.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

                    this.targetSelector.add(1, new RevengeGoal(animal));
                    
                    // 准度受游戏难度影响 (0 到 14 左右)
                    float animalDivergence = 1.0F;
                    
                    this.goalSelector.add(4, new UniversalBowAttackGoal(
                            animal, 1.0D, 16.0F, 
                            5, 10,   // 疯狂连射 (1秒2到4箭)
                            animalDivergence, 
                            StatusEffects.SLOWNESS // 缓慢效果
                    ));
                }
                // 注意：铁傀儡属于 GolemEntity，既不是 HostileEntity 也不是 AnimalEntity，
                // 所以它会直接跳过这部分逻辑，完美保留其原版的近战 AI。
            }
        }
    }
}
