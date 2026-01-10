package ua.kalledat.mixin;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import one.oktw.VelocityLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VelocityLib.class)
@Pseudo
public class VelocityLoginMixin {

    @ModifyVariable(method = "createProfile", at = @At(value = "HEAD"), argsOnly = true, name = "arg0")
    private static FriendlyByteBuf changeUuid(FriendlyByteBuf buf) {
        var readerIndex = buf.readerIndex();
        var writerIndex = buf.writerIndex();
        // shifting index to read nickname
        buf.readUUID();
        var uuid = UUIDUtil.createOfflinePlayerUUID(buf.readUtf(16));
        return buf
                .setLong(readerIndex, uuid.getMostSignificantBits())
                .setLong(readerIndex + 8, uuid.getLeastSignificantBits())
                .setIndex(readerIndex, writerIndex);
    }
}
