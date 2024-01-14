package vazkii.patchouli.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.PacketDistributor;
import vazkii.patchouli.client.book.ClientBookRegistry;

public class ForgeMessageReloadBookContents {
	public ForgeMessageReloadBookContents() {}

	public static void sendToAll(MinecraftServer server) {
		ForgeNetworkHandler.CHANNEL.send(new ForgeMessageReloadBookContents(), PacketDistributor.ALL.noArg());
	}

	public void encode(FriendlyByteBuf buf) {}

	public static ForgeMessageReloadBookContents decode(FriendlyByteBuf buf) {
		return new ForgeMessageReloadBookContents();
	}

	public void handle(CustomPayloadEvent.Context ctx) {
		ctx.enqueueWork(ClientBookRegistry.INSTANCE::reload);
		ctx.setPacketHandled(true);
	}
}
