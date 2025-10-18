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
        var newPacket = new PacketByteBuf(buf.copy())
                .setIndex(buf.readerIndex(), buf.writerIndex());
        // shifting index to read nickname
        newPacket.readUuid();
        var uuid = Uuids.getOfflinePlayerUuid(buf.readString(16));
        newPacket
                .setIndex(buf.readerIndex(), buf.writerIndex())
                .writeUuid(uuid)
                .setIndex(buf.readerIndex(), buf.writerIndex());
        return newPacket;
    }
}
