package ua.kalledat.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ua.kalledat.PlayerMigration;

import java.util.Optional;
import java.util.function.Function;

@Mixin(ServerLoginNetworkHandler.class)
public class LoginHelloPacketMixin {

    @ModifyArg(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginNetworkHandler;startVerify(Lcom/mojang/authlib/GameProfile;)V"), index = 0)
    private GameProfile modifyPacket(GameProfile gameProfile) {
        return Optional.ofNullable(PlayerMigration.jsonFileRepository)
                .map(repo -> repo.getOriginalNickname(gameProfile.getName()))
                .flatMap(Function.identity())
                .map(nickname -> new GameProfile(Uuids.getOfflinePlayerUuid(nickname), gameProfile.getName()))
                .orElse(gameProfile);
    }
}
