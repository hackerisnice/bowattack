package com.panda.bowattck.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.EnumSet;

public class UniversalBowAttackGoal extends Goal {
    private final MobEntity mob;
    private final double speed;
    private final float squaredRange;
    
    // 动态属性配置
    private final int minInterval;
    private final int maxInterval;
    private final float divergence;
    private final RegistryEntry<StatusEffect> effect;
    
    private LivingEntity target;
    private int attackTime = -1;
    private int seeTime = 0;

    public UniversalBowAttackGoal(MobEntity mob, double speed, float range, int minInterval, int maxInterval, float divergence, RegistryEntry<StatusEffect> effect) {
        this.mob = mob;
        this.speed = speed;
        this.squaredRange = range * range;
        this.minInterval = minInterval;
        this.maxInterval = maxInterval;
        this.divergence = divergence;
        this.effect = effect;
        // 声明控制权：接管移动和视角
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
            // 到达射程后停下脚步
            // （因为该 AI 优先级已设为 4，苦力怕的高优先级自爆 AI 会自然接管并继续寻路）
            this.mob.getNavigation().stop();
        }

        this.mob.getLookControl().lookAt(this.target, 30.0F, 30.0F);

        if (--this.attackTime <= 0) {
            if (!canSee) return;
            shootCustomArrow();
            // 在最小值和最大值之间随机生成下一次的攻击间隔
            this.attackTime = this.minInterval + this.mob.getRandom().nextInt(this.maxInterval - this.minInterval + 1); 
        }
    }

    private void shootCustomArrow() {
        ItemStack bow = this.mob.getMainHandStack();
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        
        PersistentProjectileEntity projectile = ProjectileUtil.createArrowProjectile(this.mob, arrowStack, 1.0F, bow);
        
        // 附加定制的药水效果，持续 10 秒
        if (projectile instanceof ArrowEntity arrowEntity) {
            arrowEntity.addEffect(new StatusEffectInstance(this.effect, 200, 0));
        }
        
        double d = this.target.getX() - this.mob.getX();
        double e = this.target.getBodyY(0.333333D) - projectile.getY();
        double f = this.target.getZ() - this.mob.getZ();
        double g = Math.sqrt(d * d + f * f);
        
        // 传入 divergence 决定准星误差
        projectile.setVelocity(d, e + g * 0.2D, f, 1.6F, this.divergence);
        
        SoundCategory category = (this.mob instanceof net.minecraft.entity.mob.HostileEntity) ? SoundCategory.HOSTILE : SoundCategory.NEUTRAL;
        
        this.mob.getWorld().playSound(null, this.mob.getX(), this.mob.getY(), this.mob.getZ(), 
                SoundEvents.ENTITY_SKELETON_SHOOT, category, 1.0F, 1.0F / (this.mob.getRandom().nextFloat() * 0.4F + 0.8F));
        
        this.mob.getWorld().spawnEntity(projectile);
    }
}
