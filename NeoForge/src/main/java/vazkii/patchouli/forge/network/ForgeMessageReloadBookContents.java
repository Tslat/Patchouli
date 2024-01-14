package vazkii.patchouli.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.ClientBookRegistry;

public class ForgeMessageReloadBookContents implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(PatchouliAPI.MOD_ID, "reload_books");

	public ForgeMessageReloadBookContents() {}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	public static void sendToAll(MinecraftServer server) {
		PacketDistributor.ALL.noArg().send(new ForgeMessageReloadBookContents());
	}

	@Override
	public void write(FriendlyByteBuf buf) {}

	public static ForgeMessageReloadBookContents decode(FriendlyByteBuf buf) {
		return new ForgeMessageReloadBookContents();
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(ClientBookRegistry.INSTANCE::reload);
	}
}
