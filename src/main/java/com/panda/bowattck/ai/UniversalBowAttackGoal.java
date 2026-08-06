package com.panda.bowattck.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class UniversalBowAttackGoal extends Goal {
    // 更改为更通用的 MobEntity
    private final MobEntity mob;
    private final double speed;
    private final float squaredRange;
    private LivingEntity target;
    private int attackTime = -1;
    private int seeTime = 0;

    public UniversalBowAttackGoal(MobEntity mob, double speed, float range) {
        this.mob = mob;
        this.speed = speed;
        this.squaredRange = range * range;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null && livingEntity.isAlive()) {
            this.target = livingEntity;
            return this.mob.getMainHandStack().isOf(Items.BOW);
        }
        return false;
    }

    @Override
    public void start() {
        super.start();
        this.mob.setAttacking(true);
    }

    @Override
    public void stop() {
        super.stop();
        this.mob.setAttacking(false);
        this.target = null;
        this.seeTime = 0;
        this.attackTime = -1;
        this.mob.clearActiveItem();
    }

    @Override
    public void tick() {
        if (this.target == null) return;

        double distanceSq = this.mob.squaredDistanceTo(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean canSee = this.mob.getVisibilityCache().canSee(this.target);
        
        if (canSee) {
            this.seeTime++;
        } else {
            this.seeTime = 0;
        }

        if (distanceSq > this.squaredRange || this.seeTime < 5) {
            this.mob.getNavigation().startMovingTo(this.target, this.speed);
        } else {
            // 核心修改：如果是苦力怕，不停止移动，继续死磕贴脸，从而触发原版自爆 AI
            if (this.mob instanceof CreeperEntity) {
                this.mob.getNavigation().startMovingTo(this.target, this.speed);
            } else {
                this.mob.getNavigation().stop();
            }
        }

        this.mob.getLookControl().lookAt(this.target, 30.0F, 30.0F);

        if (--this.attackTime <= 0) {
            if (!canSee) return;
            shootWeaknessArrow();
            this.attackTime = 30 + this.mob.getRandom().nextInt(20); 
        }
    }

    private void shootWeaknessArrow() {
        ItemStack bow = this.mob.getMainHandStack();
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        
        PersistentProjectileEntity projectile = ProjectileUtil.createArrowProjectile(this.mob, arrowStack, 1.0F, bow);
        
        if (projectile instanceof ArrowEntity arrowEntity) {
            arrowEntity.addEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0));
        }
        
        double d = this.target.getX() - this.mob.getX();
        double e = this.target.getBodyY(0.333333D) - projectile.getY();
        double f = this.target.getZ() - this.mob.getZ();
        double g = Math.sqrt(d * d + f * f);
        
        projectile.setVelocity(d, e + g * 0.2D, f, 1.6F, (float)(14 - this.mob.getWorld().getDifficulty().getId() * 4));
        
        // 动物射箭时，声音的来源判定使用 NEUTRAL（中立），敌对生物使用 HOSTILE
        SoundCategory category = (this.mob instanceof net.minecraft.entity.mob.HostileEntity) ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL;
        
        this.mob.getWorld().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), 
                SoundEvents.ENTITY_SKELETON_SHOOT, category, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        
        this.mob.getWorld().spawnEntity(projectile);
    }
}
