package com.megaworld.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Житель Грокса.
 *  - боится игроков с оружием (меч, топор, лук, арбалет, трезубец) и убегает;
 *  - после удара кричит, убегает и плачет;
 *  - идёт за игроком, если тот держит изумруд;
 *  - Shift + ПКМ пустой рукой: «стой здесь» / «можно ходить».
 */
public class GroxDwellerEntity extends PathAwareEntity {
    private boolean staying = false;
    private boolean wasScared = false;
    private int cryingTicks = 0;

    public GroxDwellerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.5D));
        goalSelector.add(2, new FearWeaponGoal(this));
        goalSelector.add(3, new StayGoal(this));
        goalSelector.add(4, new TemptGoal(this, 1.1D, Ingredient.ofItems(Items.EMERALD), false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8D));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D);
    }

    /** Оружие в руках пугает жителя. */
    public static boolean isScary(LivingEntity entity) {
        return isWeapon(entity.getMainHandStack()) || isWeapon(entity.getOffHandStack());
    }

    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof RangedWeaponItem || item instanceof TridentItem;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        if (age % 10 == 0) {
            PlayerEntity player = getWorld().getClosestPlayer(this, 10.0D);
            boolean scary = player != null && isScary(player);
            if (scary && !wasScared) {
                playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            }
            wasScared = scary;
        }
        if (cryingTicks > 0) {
            cryingTicks--;
            if (cryingTicks % 20 == 0 && getWorld() instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.FALLING_WATER, getX(), getEyeY(), getZ(), 4, 0.2D, 0.1D, 0.2D, 0.0D);
                playSound(SoundEvents.ENTITY_VILLAGER_HURT, 0.6F, 1.4F);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !getWorld().isClient) {
            cryingTicks = 160;
            staying = false;
        }
        return hurt;
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || isScary(player)) {
            return ActionResult.PASS;
        }
        if (!getWorld().isClient) {
            if (player.isSneaking()) {
                staying = !staying;
                getNavigation().stop();
                player.sendMessage(Text.translatable(staying
                        ? "entity.megaworld.grox_dweller.stay"
                        : "entity.megaworld.grox_dweller.free"), true);
                playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, 1.0F);
            } else {
                player.sendMessage(Text.translatable("entity.megaworld.grox_dweller.greeting"), true);
                playSound(SoundEvents.ENTITY_VILLAGER_TRADE, 1.0F, 1.0F);
            }
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false; // не исчезает, когда ты уходишь далеко
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Staying", staying);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        staying = nbt.getBoolean("Staying");
    }

    /** Убегает от игрока с оружием в руках. */
    private static class FearWeaponGoal extends Goal {
        private final GroxDwellerEntity mob;
        private PlayerEntity scarer;

        FearWeaponGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            PlayerEntity player = mob.getWorld().getClosestPlayer(mob, 12.0D);
            if (player != null && isScary(player)) {
                scarer = player;
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return scarer != null && scarer.isAlive() && isScary(scarer)
                    && mob.squaredDistanceTo(scarer) < 18.0D * 18.0D;
        }

        @Override
        public void start() {
            flee();
        }

        @Override
        public void tick() {
            if (mob.getNavigation().isIdle()) {
                flee();
            }
        }

        @Override
        public void stop() {
            scarer = null;
            mob.getNavigation().stop();
        }

        private void flee() {
            if (scarer == null) {
                return;
            }
            Vec3d away = mob.getPos().subtract(scarer.getPos());
            if (away.lengthSquared() < 0.01D) {
                away = new Vec3d(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize().multiply(10.0D).add(mob.getPos());
            mob.getNavigation().startMovingTo(away.x, away.y, away.z, 1.3D);
        }
    }

    /** Стоит на месте, пока игрок не разрешит ходить. */
    private static class StayGoal extends Goal {
        private final GroxDwellerEntity mob;

        StayGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP));
        }

        @Override
        public boolean canStart() {
            return mob.staying && mob.isOnGround();
        }

        @Override
        public boolean shouldContinue() {
            return mob.staying;
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }
    }
}
