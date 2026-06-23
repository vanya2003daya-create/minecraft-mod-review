package com.adam.firemagic.events;

import com.adam.firemagic.FireMagicMod;
import com.adam.firemagic.mana.ManaManager;
import com.adam.firemagic.mana.PlayerManaData;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.advancement.Advancement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PlayerDeathHandler implements ServerPlayerEvents.AfterRespawn {
    @Override
    public void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer, boolean alive) {
        // Получаем данные маны старого игрока (до смерти)
        PlayerManaData oldManaData = ManaManager.getServerData(oldPlayer);
        boolean hadSchool = oldManaData.hasMagicSchool();

        // Сбрасываем школу магии для нового игрока
        PlayerManaData newManaData = ManaManager.getServerData(newPlayer);
        newManaData.resetOnDeath();
        ManaManager.setServerData(newPlayer, newManaData);

        // Если у игрока была школа - удаляем кирку и отзываем достижение
        if (hadSchool) {

            // Отзыв достижения
            Advancement advancement = newPlayer.getServer().getAdvancementLoader().get(
                    new Identifier(FireMagicMod.MOD_ID, "learn_miner_magic")
            );
            if (advancement != null) {
                newPlayer.getAdvancementTracker().revokeCriterion(advancement, "learned_magic");
            }
        }
    }

}