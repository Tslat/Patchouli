package vazkii.patchouli.forge.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.book.ClientBookRegistry;


public class ForgeMessageOpenBookGui implements CustomPacketPayload {
	public static final ResourceLocation ID = new ResourceLocation(PatchouliAPI.MOD_ID, "open_book");

	private final ResourceLocation book;
	@Nullable private final ResourceLocation entry;
	private final int page;

	public ForgeMessageOpenBookGui(ResourceLocation book, @Nullable ResourceLocation entry, int page) {
		this.book = book;
		this.entry = entry;
		this.page = page;
	}

	@Override
	public ResourceLocation id() {
		return ID;
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeResourceLocation(book);
		buf.writeUtf(entry == null ? "" : entry.toString());
		buf.writeVarInt(page);
	}

	public static ForgeMessageOpenBookGui decode(FriendlyByteBuf buf) {
		ResourceLocation book = buf.readResourceLocation();
		ResourceLocation entry;
		String tmp = buf.readUtf();
		if (tmp.isEmpty()) {
			entry = null;
		} else {
			entry = ResourceLocation.tryParse(tmp);
		}

		int page = buf.readVarInt();
		return new ForgeMessageOpenBookGui(book, entry, page);
	}

	public static void send(ServerPlayer player, ResourceLocation book, @Nullable ResourceLocation entry, int page) {
		PacketDistributor.PLAYER.with(player).send(new ForgeMessageOpenBookGui(book, entry, page));
	}

	public void handle(PlayPayloadContext context) {
		context.workHandler().execute(() -> ClientBookRegistry.INSTANCE.displayBookGui(book, entry, page));
	}
}
