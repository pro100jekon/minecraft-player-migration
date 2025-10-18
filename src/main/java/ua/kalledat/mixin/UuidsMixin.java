package ua.kalledat.mixin;

import net.minecraft.util.Uuids;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ua.kalledat.PlayerMigration;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

@Mixin(Uuids.class)
public class UuidsMixin {

    @Redirect(method = "getOfflinePlayerUuid", at = @At(value = "INVOKE", target = "Ljava/util/UUID;nameUUIDFromBytes([B)Ljava/util/UUID;"))
    private static UUID test(byte[] md) {
        var nickname = StringUtils.substringAfter(new String(md), "OfflinePlayer:");
        return Optional.ofNullable(PlayerMigration.playerMigrationRepo)
                .map(repo -> repo.getOriginalNickname(nickname))
                .flatMap(Function.identity())
                .map(oldNickname -> UUID.nameUUIDFromBytes(("OfflinePlayer:" + oldNickname).getBytes(StandardCharsets.UTF_8)))
                .orElse(UUID.nameUUIDFromBytes(md));
    }
}
