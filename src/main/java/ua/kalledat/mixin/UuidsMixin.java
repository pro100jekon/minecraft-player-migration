package ua.kalledat.mixin;

import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ua.kalledat.PlayerMigration;

import java.util.Optional;
import java.util.function.Function;

@Mixin(Uuids.class)
public class UuidsMixin {

    @ModifyVariable(method = "getOfflinePlayerUuid", at = @At(value = "HEAD"), argsOnly = true)
    private static String modifyNickname(String nickname) {
        return Optional.ofNullable(PlayerMigration.playerMigrationRepo)
                .map(repo -> repo.getOriginalNickname(nickname))
                .flatMap(Function.identity())
                .orElse(nickname);
    }
}
