package ua.kalledat.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Uuids;
import one.oktw.VelocityLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(VelocityLib.class)
@Pseudo
public class VelocityLoginMixin {

    @ModifyVariable(method = "createProfile", at = @At(value = "HEAD"), argsOnly = true)
    private static PacketByteBuf changeUuid(PacketByteBuf buf) {
        var readerIndex = buf.readerIndex();
        var writerIndex = buf.writerIndex();
        var newPacket = new PacketByteBuf(buf.copy())
                .setIndex(readerIndex, writerIndex);
        // shifting index to read nickname
        buf.readUuid();
        newPacket
                .writeUuid(Uuids.getOfflinePlayerUuid(buf.readString(16)))
                .setIndex(readerIndex, writerIndex);
        return newPacket;
    }
}
