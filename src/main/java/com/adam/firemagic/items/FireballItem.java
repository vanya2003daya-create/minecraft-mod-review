package com.adam.firemagic.items;

import com.adam.firemagic.entities.CustomFireballEntity;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class FireballItem extends Item {
    public FireballItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) user;

        PlayerManaData manaData = ManaManager.getServerData(serverPlayer);

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            user.sendMessage(Text.translatable("message.firemagic.cooldown"), true);
            return TypedActionResult.fail(stack);
        }

        if (manaData.consumeMana(1)) {
            ManaManager.setServerData(serverPlayer, manaData);

            user.getItemCooldownManager().set(this, 1);

            // ✅ ИСПРАВЛЕНИЕ: Спавним файрболл ПРЯМО В ГЛАЗАХ игрока
            CustomFireballEntity fireball = new CustomFireballEntity(world, serverPlayer);

            // 1. СПАВН ПРЯМО В ГЛАЗАХ ИГРОКА (без смещения вперед)
            Vec3d spawnPos = user.getEyePos(); // ТОЧНО в глазах

            // 2. Проверяем, нет ли блока прямо перед лицом
            Vec3d lookVec = user.getRotationVec(1.0F).normalize();
            double checkDistance = 0.5; // Проверяем на 0.5 блоков вперед

            Vec3d checkPos = spawnPos.add(lookVec.multiply(checkDistance));

            // Если есть блок прямо перед лицом - спавним ПЕРЕД блоком
            if (world.getWorldBorder().contains(BlockPos.ofFloored(checkPos)) &&
                    !world.getBlockState(BlockPos.ofFloored(checkPos)).isAir()) {
                // Блок прямо перед лицом - спавним чуть ближе
                spawnPos = spawnPos.add(lookVec.multiply(0.2)); // Всего 0.2 блока вперед
            } else {
                // Нет блока - спавним как обычно
                spawnPos = spawnPos.add(lookVec.multiply(0.5)); // 0.5 блока вперед
            }

            fireball.setPosition(spawnPos);

            // 3. Направление с хорошей скоростью
            Vec3d lookDir = user.getRotationVec(1.0F);
            fireball.setVelocity(lookDir.x, lookDir.y, lookDir.z, 1.8f, 1.0f);

            fireball.setNoGravity(true);

            world.spawnEntity(fireball);

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);

            return TypedActionResult.success(stack);
        } else {
            user.sendMessage(Text.translatable("message.firemagic.insufficient_mana"), true);
            return TypedActionResult.fail(stack);
        }
    }
}