package com.megaworld.entity;

import com.megaworld.ModItems;
import com.megaworld.ai.GroxBrain;
import com.megaworld.ai.GroxCommands;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
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
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * Житель Грокса.
 *  - Эмоции (радость, грусть, злость, страх), лицо меняется.
 *  - Боится оружия, после ударов грустит и злится.
 *  - Понимает команды из чата (см. GroxCommands): идти за тобой, стоять, прийти, идти вперёд/назад/влево/вправо,
 *    собирать и приносить предметы, отдавать и выбрасывать, идти домой, танцевать, махать рукой,
 *    крутиться, прыгать, бежать, светиться, прятаться, петь, шутить, говорить время, погоду, координаты.
 *  - Свой рюкзак на 9 ячеек (сохраняется, выпадает при смерти).
 */
public class GroxDwellerEntity extends PathAwareEntity {
    private static final TrackedData<Integer> EMOTION =
            DataTracker.registerData(GroxDwellerEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> GESTURE =
            DataTracker.registerData(GroxDwellerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final TrackedData<Integer> VARIANT =
            DataTracker.registerData(GroxDwellerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private enum OrderKind { COME, GO, HOME, GIVE }

    private static final String[] JOKES = {
            "Почему крипер не ходит в гости? Он всегда уходит со взрывом!",
            "Знаешь, чем житель отличается от стены? Стена хотя бы не говорит «хмм».",
            "Алмазная кирка зашла в бар. Бармен: «Ты тут ради руды?»",
            "Зомби постучал в дверь. Я говорю: «Кто там?» Он: «Мозг». Я: «Мозг дома нет».",
            "Мой друг построил дом из земли. Теперь он землевладелец!",
            "Что сказал скелет, когда его не позвали на вечеринку? «Мне нечего надеть, кроме костей».",
    };
    private static final String[] SONGS = {
            "Ла-ла-ла, я житель Грокса, хожу, смотрю по сторонам!",
            "Бла-бла-бла, иду по травке, а в рюкзаке у меня кристалл!",
            "Тум-тум-тум, изумруд найду, тебе покажу, а потом сам съем!",
    };

    private final DefaultedList<ItemStack> backpack = DefaultedList.ofSize(9, ItemStack.EMPTY);

    private boolean staying = false;
    private boolean wasScared = false;
    private int emotionTicks = 0;
    private int followTicks = 0;
    private int hurtCount = 0;
    private int hurtTimer = 0;
    private int actionTicks = 0;

    private OrderKind orderKind = null;
    private Vec3d orderTarget = null;
    private PlayerEntity orderPlayer = null;
    private int orderTicks = 0;

    private String[] collectFilter = null;
    private int collectTicks = 0;
    private boolean giveAfterCollect = false;
    private PlayerEntity fetchPlayer = null;
    private String[] giveFilter = null;

    private BlockPos home = null;

    public GroxDwellerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    // ---------- синхронизируемые данные ----------
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(EMOTION, Emotion.NEUTRAL.ordinal());
        this.dataTracker.startTracking(GESTURE, Gesture.NONE.ordinal());
        this.dataTracker.startTracking(VARIANT, Variant.NORMAL.ordinal());
    }

    public Emotion getEmotion() {
        return Emotion.byOrdinal(this.dataTracker.get(EMOTION));
    }

    public Gesture getGesture() {
        return Gesture.byOrdinal(this.dataTracker.get(GESTURE));
    }

    public Variant getVariant() {
        return Variant.byOrdinal(this.dataTracker.get(VARIANT));
    }

    /** Сменить тип жителя: меняются лицо, голос, скорость и характер. */
    public void setVariant(Variant variant) {
        this.dataTracker.set(VARIANT, variant.ordinal());
        EntityAttributeInstance speed = getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.5D * variant.speed);
        }
        if (emotionTicks <= 0) {
            this.dataTracker.set(EMOTION, variant.baseline.ordinal());
        }
    }

    /** Напуган «по-настоящему» (а не просто показывает испуганное лицо по характеру). */
    private boolean isFrightened() {
        return emotionTicks > 0 && getEmotion() == Emotion.FEAR;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 EntityData entityData, NbtCompound entityNbt) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
        setVariant(Variant.random(getRandom()));
        return data;
    }

    private void setGesture(Gesture gesture, int ticks) {
        this.dataTracker.set(GESTURE, gesture.ordinal());
        this.actionTicks = gesture == Gesture.NONE ? 0 : ticks;
    }

    /** Принудительно задать эмоцию на ticks тиков (потом вернётся к спокойной). */
    public void setEmotion(Emotion emotion, int ticks) {
        if (emotion == Emotion.NEUTRAL) {
            this.dataTracker.set(EMOTION, getVariant().baseline.ordinal());
            this.emotionTicks = 0;
        } else {
            this.dataTracker.set(EMOTION, emotion.ordinal());
            this.emotionTicks = ticks;
        }
    }

    private void raise(Emotion emotion, int ticks) {
        if (emotionTicks <= 0 || emotion.priority >= getEmotion().priority) {
            setEmotion(emotion, ticks);
        }
    }

    // ---------- цели (поведение) ----------
    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.5D));
        goalSelector.add(2, new FearGoal(this));
        goalSelector.add(3, new OrderGoal(this));
        goalSelector.add(4, new StayGoal(this));
        goalSelector.add(5, new CollectGoal(this));
        goalSelector.add(6, new FollowSpeakerGoal(this));
        goalSelector.add(7, new TemptGoal(this, 1.1D, Ingredient.ofItems(Items.EMERALD, ModItems.GROX_CRYSTAL), false));
        goalSelector.add(8, new WanderAroundFarGoal(this, 0.8D));
        goalSelector.add(9, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        goalSelector.add(10, new LookAroundGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.5D);
    }

    // ---------- оружие, друзья ----------
    public static boolean isScary(LivingEntity entity) {
        return isWeapon(entity.getMainHandStack()) || isWeapon(entity.getOffHandStack());
    }

    private static boolean isWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof RangedWeaponItem || item instanceof TridentItem;
    }

    private static boolean isFriendly(PlayerEntity player) {
        return player.getMainHandStack().isOf(Items.EMERALD) || player.getMainHandStack().isOf(ModItems.GROX_CRYSTAL);
    }

    // ---------- рюкзак ----------
    private static boolean matches(ItemStack stack, String[] filter) {
        if (stack.isEmpty()) {
            return false;
        }
        if (filter == null) {
            return true;
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        for (String f : filter) {
            if (f.equals("@food")) {
                if (stack.getItem().isFood()) {
                    return true;
                }
            } else if (f.startsWith("=")) {
                if (path.equals(f.substring(1))) {
                    return true;
                }
            } else if (path.contains(f)) {
                return true;
            }
        }
        return false;
    }

    /** Кладёт стак в рюкзак, возвращает то, что не поместилось. */
    private ItemStack store(ItemStack stack) {
        for (int i = 0; i < backpack.size() && !stack.isEmpty(); i++) {
            ItemStack slot = backpack.get(i);
            if (!slot.isEmpty() && ItemStack.canCombine(slot, stack)) {
                int move = Math.min(slot.getMaxCount() - slot.getCount(), stack.getCount());
                if (move > 0) {
                    slot.increment(move);
                    stack.decrement(move);
                }
            }
        }
        for (int i = 0; i < backpack.size() && !stack.isEmpty(); i++) {
            if (backpack.get(i).isEmpty()) {
                backpack.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }

    private ItemEntity findItem() {
        Box box = getBoundingBox().expand(16.0D, 6.0D, 16.0D);
        ItemEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (ItemEntity e : getWorld().getEntitiesByClass(ItemEntity.class, box,
                ie -> ie.isAlive() && matches(ie.getStack(), collectFilter))) {
            double d = squaredDistanceTo(e);
            if (d < bestDist) {
                bestDist = d;
                best = e;
            }
        }
        return best;
    }

    private void pickUp(ItemEntity entity) {
        ItemStack stack = entity.getStack();
        int before = stack.getCount();
        ItemStack left = store(stack.copy());
        if (left.isEmpty()) {
            entity.discard();
        } else {
            entity.setStack(left);
        }
        if (left.getCount() != before) {
            playSound(SoundEvents.ENTITY_ITEM_PICKUP, 0.3F, 1.0F);
        } else {
            collectTicks = 0; // рюкзак полон
        }
    }

    /** Отдаёт предметы по фильтру: бросает их в сторону игрока (или просто на землю). Возвращает число штук. */
    private int giveItems(String[] filter, PlayerEntity toward) {
        int total = 0;
        for (int i = 0; i < backpack.size(); i++) {
            ItemStack stack = backpack.get(i);
            if (!matches(stack, filter)) {
                continue;
            }
            total += stack.getCount();
            ItemEntity dropped = dropStack(stack.copy());
            if (dropped != null && toward != null) {
                Vec3d dir = toward.getPos().add(0.0D, 1.0D, 0.0D).subtract(getPos().add(0.0D, 1.0D, 0.0D));
                if (dir.lengthSquared() > 0.01D) {
                    dropped.setVelocity(dir.normalize().multiply(0.25D));
                }
            }
            backpack.set(i, ItemStack.EMPTY);
        }
        return total;
    }

    private int countItems(String[] filter) {
        int total = 0;
        for (ItemStack stack : backpack) {
            if (matches(stack, filter)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    protected void drop(DamageSource source) {
        super.drop(source);
        giveItems(null, null);
    }

    // ---------- речь и команды ----------
    private String pick(String... options) {
        return options[getRandom().nextInt(options.length)];
    }

    private void say(ServerPlayerEntity player, String text, Emotion emotion) {
        setEmotion(emotion, 120);
        player.sendMessage(Text.literal("<Житель Грокса> ").formatted(Formatting.YELLOW)
                .append(Text.literal(text)), false);
        SoundEvent sound = emotion == Emotion.JOY ? getVariant().yes
                : (emotion == Emotion.NEUTRAL ? getVariant().ambient : getVariant().no);
        playSound(sound, 1.0F, 1.0F);
    }

    /** Обычная речь: ответ из GroxBrain или ИИ. */
    public void hear(ServerPlayerEntity player, GroxBrain.Reply reply) {
        setEmotion(reply.emotion(), 200);
        switch (reply.action()) {
            case FOLLOW -> {
                staying = false;
                followTicks = 1200;
            }
            case STAY -> {
                staying = true;
                followTicks = 0;
                getNavigation().stop();
            }
            default -> {
            }
        }
        player.sendMessage(Text.literal("<Житель Грокса> ").formatted(Formatting.YELLOW)
                .append(Text.literal(reply.text())), false);
        playSound(reply.emotion() == Emotion.JOY ? getVariant().yes : getVariant().no,
                1.0F, 1.0F);
    }

    private void clearOrders() {
        orderKind = null;
        orderTarget = null;
        orderPlayer = null;
        orderTicks = 0;
        collectTicks = 0;
        giveAfterCollect = false;
        fetchPlayer = null;
        followTicks = 0;
        setGesture(Gesture.NONE, 0);
        getNavigation().stop();
    }

    private static String dirWord(GroxCommands.Dir dir) {
        return switch (dir) {
            case FORWARD -> "вперёд";
            case BACK -> "назад";
            case LEFT -> "влево";
            case RIGHT -> "вправо";
        };
    }

    /** Выполнить команду игрока (вызывается на сервере из GroxChat). */
    public void execute(ServerPlayerEntity player, GroxCommands.Command cmd) {
        if (getVariant() == Variant.DUMB && cmd.type() != GroxCommands.Type.STOP && getRandom().nextInt(4) == 0) {
            silly(player);
            return;
        }
        switch (cmd.type()) {
            case STOP -> {
                clearOrders();
                say(player, pick("Ладно, остановился.", "Всё, прекращаю!"), Emotion.NEUTRAL);
            }
            case FREE -> {
                clearOrders();
                staying = false;
                say(player, pick("Свобода! Пойду погуляю.", "Ура, можно гулять!"), Emotion.JOY);
            }
            case STAY -> {
                clearOrders();
                staying = true;
                say(player, pick("Хорошо, стою здесь.", "Буду ждать на этом месте."), Emotion.NEUTRAL);
            }
            case FOLLOW -> {
                clearOrders();
                staying = false;
                followTicks = 2400;
                say(player, pick("Иду за тобой!", "Веди, я следом!"), Emotion.JOY);
            }
            case COME -> {
                clearOrders();
                staying = false;
                orderKind = OrderKind.COME;
                orderPlayer = player;
                orderTicks = 400;
                say(player, pick("Бегу к тебе!", "Уже иду!"), Emotion.JOY);
            }
            case GO -> {
                clearOrders();
                staying = false;
                GroxCommands.Dir dir = cmd.dir();
                int blocks = Math.max(1, Math.min(64, cmd.amount()));
                Vec3d look = player.getRotationVec(1.0F);
                Vec3d flat = new Vec3d(look.x, 0.0D, look.z);
                flat = flat.lengthSquared() < 1.0E-4D ? new Vec3d(0.0D, 0.0D, 1.0D) : flat.normalize();
                Vec3d step = switch (dir) {
                    case FORWARD -> flat;
                    case BACK -> flat.multiply(-1.0D);
                    case LEFT -> new Vec3d(flat.z, 0.0D, -flat.x);
                    case RIGHT -> new Vec3d(-flat.z, 0.0D, flat.x);
                };
                orderKind = OrderKind.GO;
                orderTarget = getPos().add(step.multiply(blocks));
                orderTicks = 400;
                say(player, "Иду " + dirWord(dir) + " на " + blocks + " блоков.", Emotion.NEUTRAL);
            }
            case HOME_SET -> {
                home = player.getBlockPos();
                say(player, "Запомнил! Теперь здесь мой дом.", Emotion.JOY);
            }
            case HOME_GO -> {
                if (home == null) {
                    say(player, "У меня нет дома. Встань на место и скажи «это твой дом».", Emotion.NEUTRAL);
                } else {
                    clearOrders();
                    staying = false;
                    orderKind = OrderKind.HOME;
                    orderTarget = Vec3d.ofBottomCenter(home);
                    orderTicks = 1200;
                    say(player, "Иду домой!", Emotion.JOY);
                }
            }
            case COLLECT -> {
                clearOrders();
                staying = false;
                collectFilter = cmd.filter();
                collectTicks = 600;
                say(player, "Собираю: " + cmd.filterName() + ". Бросай предметы на землю рядом со мной!", Emotion.JOY);
            }
            case FETCH -> {
                clearOrders();
                staying = false;
                collectFilter = cmd.filter();
                giveFilter = cmd.filter();
                collectTicks = 300;
                giveAfterCollect = true;
                fetchPlayer = player;
                say(player, "Ищу: " + cmd.filterName() + ", потом принесу тебе!", Emotion.JOY);
            }
            case GIVE -> {
                if (countItems(cmd.filter()) <= 0) {
                    say(player, "У меня нет такого в рюкзаке.", Emotion.SADNESS);
                } else {
                    clearOrders();
                    staying = false;
                    giveFilter = cmd.filter();
                    orderKind = OrderKind.GIVE;
                    orderPlayer = player;
                    orderTicks = 400;
                    say(player, "Несу тебе: " + cmd.filterName() + "!", Emotion.JOY);
                }
            }
            case DROP -> {
                int n = giveItems(cmd.filter(), null);
                say(player, n > 0 ? "Выбросил: " + n + " шт." : "Выбрасывать нечего.", Emotion.NEUTRAL);
            }
            case STATUS -> {
                StringBuilder sb = new StringBuilder();
                int shown = 0;
                for (ItemStack stack : backpack) {
                    if (!stack.isEmpty() && shown < 5) {
                        if (shown > 0) {
                            sb.append(", ");
                        }
                        sb.append(stack.getName().getString()).append(" x").append(stack.getCount());
                        shown++;
                    }
                }
                say(player, shown == 0 ? "Мой рюкзак пуст." : "В рюкзаке: " + sb, Emotion.NEUTRAL);
            }
            case DANCE -> {
                setGesture(Gesture.DANCE, 200);
                say(player, pick("Танцую! Смотри!", "Эх, раз, два, три!"), Emotion.JOY);
            }
            case WAVE -> {
                setGesture(Gesture.WAVE, 80);
                say(player, pick("Привет-привет!", "Машу тебе!"), Emotion.JOY);
            }
            case SPIN -> {
                setGesture(Gesture.SPIN, 60);
                say(player, "Кручусь, кручусь!", Emotion.JOY);
            }
            case JUMP -> {
                setGesture(Gesture.JUMP, 100);
                say(player, "Прыг-скок!", Emotion.JOY);
            }
            case SPEED -> {
                addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 600, 1));
                say(player, "Быстрее ветра!", Emotion.JOY);
            }
            case GLOW -> {
                addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 1200));
                say(player, "Свечусь!", Emotion.JOY);
            }
            case HIDE -> {
                clearOrders();
                staying = false;
                say(player, "Прячусь!", Emotion.FEAR);
                setEmotion(Emotion.FEAR, 300);
            }
            case SING -> {
                setGesture(Gesture.DANCE, 120);
                say(player, pick(SONGS), Emotion.JOY);
            }
            case JOKE -> say(player, pick(JOKES), Emotion.JOY);
            case TIME -> {
                long t = getWorld().getTimeOfDay() % 24000L;
                int hour = (int) ((t / 1000L + 6L) % 24L);
                int minute = (int) ((t % 1000L) * 60L / 1000L);
                say(player, String.format("Сейчас примерно %02d:%02d.", hour, minute), Emotion.NEUTRAL);
            }
            case WEATHER -> {
                String weather = getWorld().isThundering() ? "Гроза! Страшновато..."
                        : (getWorld().isRaining() ? "Идёт дождь." : "На небе ни облачка.");
                say(player, weather, Emotion.NEUTRAL);
            }
            case COORDS -> say(player, "Мы тут: " + getBlockX() + ", " + getBlockY() + ", " + getBlockZ() + ".", Emotion.NEUTRAL);
            default -> {
            }
        }
    }

    /** Житель-придурок перепутал команду и сделал что-то своё. */
    private void silly(ServerPlayerEntity player) {
        clearOrders();
        Gesture gesture = switch (getRandom().nextInt(3)) {
            case 0 -> Gesture.DANCE;
            case 1 -> Gesture.SPIN;
            default -> Gesture.JUMP;
        };
        setGesture(gesture, 120);
        say(player, pick("А? Чего? Я не понял... ну ладно, танцую!", "Хи-хи, я перепутал! Кручусь!",
                "Ага-ага... а что надо было? Прыгаю!", "Мне послышалось «танцуй»!"), Emotion.JOY);
    }

    /** Дошёл до цели приказа. */
    private void finishOrder() {
        OrderKind kind = orderKind;
        PlayerEntity target = orderPlayer;
        orderKind = null;
        orderTarget = null;
        orderPlayer = null;
        orderTicks = 0;
        getNavigation().stop();
        if (kind == null) {
            return;
        }
        switch (kind) {
            case COME -> setEmotion(Emotion.JOY, 60);
            case HOME -> {
                staying = true;
                setEmotion(Emotion.JOY, 80);
            }
            case GIVE -> {
                int n = giveItems(giveFilter, target);
                if (target instanceof ServerPlayerEntity sp) {
                    say(sp, n > 0 ? "Держи! Отдал: " + n + " шт." : "Мне нечего тебе отдать.", n > 0 ? Emotion.JOY : Emotion.SADNESS);
                }
            }
            default -> {
            }
        }
    }

    // ---------- тик ----------
    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            return;
        }
        if (hurtTimer > 0) {
            hurtTimer--;
            if (hurtTimer == 0) {
                hurtCount = 0;
            }
        }
        if (followTicks > 0) {
            followTicks--;
        }

        if (collectTicks > 0) {
            collectTicks--;
            if (collectTicks == 0 && giveAfterCollect) {
                giveAfterCollect = false;
                if (fetchPlayer != null && fetchPlayer.isAlive()) {
                    orderKind = OrderKind.GIVE;
                    orderPlayer = fetchPlayer;
                    orderTicks = 400;
                }
                fetchPlayer = null;
            }
        }
        if (orderTicks > 0) {
            orderTicks--;
            if (orderTicks == 0) {
                orderKind = null;
                orderTarget = null;
                orderPlayer = null;
                getNavigation().stop();
            }
        }

        if (actionTicks == 0 && !staying && orderTicks == 0 && age % 100 == 0) {
            Variant v = getVariant();
            if ((v == Variant.DUMB && getRandom().nextInt(4) == 0) || (v == Variant.JOLLY && getRandom().nextInt(6) == 0)) {
                if (v == Variant.DUMB) {
                    setGesture(getRandom().nextBoolean() ? Gesture.SPIN : Gesture.JUMP, 60);
                } else {
                    setGesture(Gesture.DANCE, 100);
                }
                playSound(v.yes, 1.0F, 1.0F);
            }
        }
        if (actionTicks > 0) {
            actionTicks--;
            switch (getGesture()) {
                case DANCE -> {
                    getNavigation().stop();
                    if (age % 14 == 0 && isOnGround()) {
                        jump();
                    }
                    setYaw(getYaw() + (float) Math.sin(age * 0.3D) * 6.0F);
                }
                case SPIN -> {
                    getNavigation().stop();
                    float yaw = getYaw() + 30.0F;
                    setYaw(yaw);
                    setBodyYaw(yaw);
                    setHeadYaw(yaw);
                }
                case JUMP -> {
                    if (age % 10 == 0 && isOnGround()) {
                        jump();
                    }
                }
                default -> {
                }
            }
            if (actionTicks == 0) {
                this.dataTracker.set(GESTURE, Gesture.NONE.ordinal());
            }
        }

        if (age % 10 == 0) {
            PlayerEntity player = getWorld().getClosestPlayer(this, 10.0D);
            boolean scary = player != null && isScary(player);
            if (scary) {
                if (!wasScared) {
                    playSound(getVariant().no, 1.0F, 1.0F);
                }
                raise(Emotion.FEAR, 40);
            } else if (player != null && isFriendly(player)) {
                raise(Emotion.JOY, 40);
            }
            wasScared = scary;
        }
        if (emotionTicks > 0) {
            emotionTicks--;
            if (emotionTicks == 0) {
                this.dataTracker.set(EMOTION, getVariant().baseline.ordinal());
            }
        }
        if (getEmotion() == Emotion.SADNESS && age % 20 == 0 && getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.FALLING_WATER, getX(), getEyeY(), getZ(), 4, 0.2D, 0.1D, 0.2D, 0.0D);
            if (emotionTicks > 0) {
                playSound(getVariant().hurt, 0.6F, 1.4F);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean hurt = super.damage(source, amount);
        if (hurt && !getWorld().isClient) {
            staying = false;
            followTicks = 0;
            hurtCount++;
            hurtTimer = 600;
            Variant v = getVariant();
            int angerAt = v == Variant.GRUMPY ? 1 : (v == Variant.CRYBABY ? 99 : 3);
            if (v == Variant.COWARD) {
                setEmotion(Emotion.FEAR, 240);
            } else if (hurtCount >= angerAt) {
                setEmotion(Emotion.ANGER, 200);
                hurtCount = 0;
                playSound(getVariant().no, 1.0F, 0.6F);
            } else {
                setEmotion(Emotion.SADNESS, v == Variant.CRYBABY ? 400 : 160);
            }
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
                clearOrders();
                player.sendMessage(Text.translatable(staying
                        ? "entity.megaworld.grox_dweller.stay"
                        : "entity.megaworld.grox_dweller.free"), true);
                playSound(getVariant().yes, 1.0F, 1.0F);
            } else {
                setEmotion(Emotion.JOY, 60);
                player.sendMessage(Text.translatable("entity.megaworld.grox_dweller.greeting"), true);
                playSound(getVariant().yes, 1.0F, 1.0F);
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
        return getVariant().ambient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return getVariant().hurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return getVariant().death;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putBoolean("Staying", staying);
        nbt.putInt("Variant", getVariant().ordinal());
        NbtCompound bag = new NbtCompound();
        Inventories.writeNbt(bag, backpack);
        nbt.put("Backpack", bag);
        if (home != null) {
            nbt.putBoolean("HasHome", true);
            nbt.putInt("HomeX", home.getX());
            nbt.putInt("HomeY", home.getY());
            nbt.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        staying = nbt.getBoolean("Staying");
        setVariant(Variant.byOrdinal(nbt.getInt("Variant")));
        Inventories.readNbt(nbt.getCompound("Backpack"), backpack);
        if (nbt.getBoolean("HasHome")) {
            home = new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ"));
        }
    }

    // ---------- цели ----------
    /** Убегает от игрока с оружием или когда напуган. */
    private static class FearGoal extends Goal {
        private final GroxDwellerEntity mob;
        private PlayerEntity scarer;

        FearGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        private boolean afraid(PlayerEntity player) {
            return isScary(player) || mob.isFrightened();
        }

        @Override
        public boolean canStart() {
            PlayerEntity player = mob.getWorld().getClosestPlayer(mob, 12.0D);
            if (player != null && afraid(player)) {
                scarer = player;
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return scarer != null && scarer.isAlive() && afraid(scarer)
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

    /** Выполняет приказы с целью: прийти, идти в точку, домой, отдать предметы игроку. */
    private static class OrderGoal extends Goal {
        private final GroxDwellerEntity mob;

        OrderGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            return mob.orderTicks > 0 && mob.orderKind != null && (mob.orderTarget != null || mob.orderPlayer != null);
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public void tick() {
            Vec3d target = (mob.orderPlayer != null && mob.orderPlayer.isAlive()) ? mob.orderPlayer.getPos() : mob.orderTarget;
            if (target == null) {
                mob.orderTicks = 0;
                return;
            }
            double distSq = mob.squaredDistanceTo(target.x, target.y, target.z);
            double arriveSq = mob.orderPlayer != null ? 6.0D : 2.25D;
            if (distSq <= arriveSq) {
                mob.finishOrder();
                return;
            }
            if (mob.age % 10 == 0 || mob.getNavigation().isIdle()) {
                mob.getNavigation().startMovingTo(target.x, target.y, target.z, 1.1D);
            }
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
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

    /** Подбирает предметы на земле по фильтру (команды «собери ...» и «принеси ...»). */
    private static class CollectGoal extends Goal {
        private final GroxDwellerEntity mob;
        private ItemEntity target;

        CollectGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (mob.collectTicks <= 0) {
                return false;
            }
            target = mob.findItem();
            return target != null;
        }

        @Override
        public boolean shouldContinue() {
            return mob.collectTicks > 0 && target != null && target.isAlive();
        }

        @Override
        public void tick() {
            if (target == null) {
                return;
            }
            if (mob.squaredDistanceTo(target) < 1.6D * 1.6D) {
                mob.pickUp(target);
                target = mob.findItem();
            } else if (mob.age % 10 == 0 || mob.getNavigation().isIdle()) {
                mob.getNavigation().startMovingTo(target, 1.15D);
            }
        }

        @Override
        public void stop() {
            target = null;
            mob.getNavigation().stop();
        }
    }

    /** Идёт за игроком после команды «за мной» (около двух минут). */
    private static class FollowSpeakerGoal extends Goal {
        private final GroxDwellerEntity mob;
        private PlayerEntity target;

        FollowSpeakerGoal(GroxDwellerEntity mob) {
            this.mob = mob;
            setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            if (mob.followTicks <= 0 || mob.staying) {
                return false;
            }
            target = mob.getWorld().getClosestPlayer(mob, 24.0D);
            return target != null;
        }

        @Override
        public boolean shouldContinue() {
            return mob.followTicks > 0 && !mob.staying && target != null && target.isAlive()
                    && mob.squaredDistanceTo(target) < 28.0D * 28.0D;
        }

        @Override
        public void tick() {
            if (mob.squaredDistanceTo(target) > 9.0D) {
                mob.getNavigation().startMovingTo(target, 1.1D);
            } else {
                mob.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            target = null;
            mob.getNavigation().stop();
        }
    }
}
