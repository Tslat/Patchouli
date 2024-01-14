package vazkii.patchouli.forge.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import vazkii.patchouli.api.PatchouliAPI;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NeoForgeNetworkHandler {
	@SubscribeEvent
	private static void registerPackets(RegisterPayloadHandlerEvent ev) {
		final IPayloadRegistrar registrar = ev.registrar(PatchouliAPI.MOD_ID);

		registrar.play(ForgeMessageOpenBookGui.ID, ForgeMessageOpenBookGui::decode, ForgeMessageOpenBookGui::handle);
		registrar.play(ForgeMessageReloadBookContents.ID, ForgeMessageReloadBookContents::decode, ForgeMessageReloadBookContents::handle);
	}
}
