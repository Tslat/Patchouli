package vazkii.patchouli.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.client.base.BookModel;
import vazkii.patchouli.client.base.ClientAdvancements;
import vazkii.patchouli.client.base.ClientTicker;
import vazkii.patchouli.client.base.PersistentData;
import vazkii.patchouli.client.book.BookContentResourceListenerLoader;
import vazkii.patchouli.client.book.ClientBookRegistry;
import vazkii.patchouli.client.handler.BookRightClickHandler;
import vazkii.patchouli.client.handler.MultiblockVisualizationHandler;
import vazkii.patchouli.client.handler.TooltipHandler;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.ItemModBook;
import vazkii.patchouli.common.item.PatchouliItems;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mod.EventBusSubscriber(modid = PatchouliAPI.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeClientInitializer {
	/**
	 * Why are these necessary?
	 * BookRegistry.init is called from CommonSetupEvent. We need the models to be known in ModelRegistryEvent.
	 * However, there is no defined ordering for those events. They all run concurrently during the initial resource
	 * reload.
	 * We need a way of waiting for the books to become known.
	 * Another critical point to note is that loading runs on a fixed-size ForkJoinPool.
	 * Blocking the thread can starve loading completely.
	 * Fortunately, the implementation of Condition.await for ReentrantLock uses ForkJoinPool.managedBlock,
	 * which is aware of potentially blocking operations and can resize the pool accordingly.
	 * If parallel mod loading didn't exist we wouldn't need any of this, but here we are :))))
	 */
	private static final Lock BOOK_LOAD_LOCK = new ReentrantLock();
	private static final Condition BOOK_LOAD_CONDITION = BOOK_LOAD_LOCK.newCondition();
	private static boolean booksLoaded = false;

	public static void signalBooksLoaded() {
		BOOK_LOAD_LOCK.lock();
		booksLoaded = true;
		BOOK_LOAD_CONDITION.signalAll();
		BOOK_LOAD_LOCK.unlock();
	}

	private static List<ResourceLocation> getBookModels() {
		BOOK_LOAD_LOCK.lock();
		try {
			while (!booksLoaded) {
				BOOK_LOAD_CONDITION.awaitUninterruptibly();
			}
			return BookRegistry.INSTANCE.books.values().stream().map(b -> b.model).toList();
		} finally {
			BOOK_LOAD_LOCK.unlock();
		}
	}

	@SubscribeEvent
	public static void modelRegistry(ModelEvent.RegisterAdditional e) {
		getBookModels()
				.stream()
				.map(model -> new ModelResourceLocation(model, "inventory"))
				.forEach(e::register);

		ItemPropertyFunction prop = (stack, world, entity, seed) -> ItemModBook.getCompletion(stack);
		ItemProperties.register(PatchouliItems.BOOK, new ResourceLocation(PatchouliAPI.MOD_ID, "completion"), prop);
	}

	@SubscribeEvent
	public static void registerReloadListeners(RegisterClientReloadListenersEvent e) {
		e.registerReloadListener(BookContentResourceListenerLoader.INSTANCE);

		e.registerReloadListener((ResourceManagerReloadListener) manager -> {
			if (Minecraft.getInstance().level != null) {
				PatchouliAPI.LOGGER.info("Reloading resource pack-based books");
				ClientBookRegistry.INSTANCE.reload();
			} else {
				PatchouliAPI.LOGGER.debug("Not reloading resource pack-based books as client world is missing");
			}
		});
	}

	@SubscribeEvent
	public static void registerOverlays(RegisterGuiOverlaysEvent evt) {
		evt.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), new ResourceLocation(PatchouliAPI.MOD_ID, "book_right_click"),
				(gui, poseStack, partialTick, width, height) -> BookRightClickHandler.onRenderHUD(poseStack, partialTick)
		);
		evt.registerBelow(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), new ResourceLocation(PatchouliAPI.MOD_ID, "multiblock_progress"),
				(gui, poseStack, partialTick, width, height) -> MultiblockVisualizationHandler.onRenderHUD(poseStack, partialTick)
		);
	}

	@SubscribeEvent
	public static void onInitializeClient(FMLClientSetupEvent evt) {
		ClientBookRegistry.INSTANCE.init();
		PersistentData.setup();
		NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
			if (e.phase == TickEvent.Phase.END) {
				ClientTicker.endClientTick(Minecraft.getInstance());
			}
		});
		NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> BookRightClickHandler.onRightClick(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec()));
		NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
			InteractionResult result = MultiblockVisualizationHandler.onPlayerInteract(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());
			if (result.consumesAction()) {
				e.setCanceled(true);
				e.setCancellationResult(result);
			}
		});
		NeoForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
			if (e.phase == TickEvent.Phase.END) {
				MultiblockVisualizationHandler.onClientTick(Minecraft.getInstance());
			}
		});

		NeoForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent e) -> {
			if (e.phase == TickEvent.Phase.START) {
				ClientTicker.renderTickStart(e.renderTickTime);
			} else {
				ClientTicker.renderTickEnd();
			}
		});

		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
			ClientAdvancements.playerLogout();
		});

		NeoForge.EVENT_BUS.addListener((RenderTooltipEvent.Pre e) -> {
			TooltipHandler.onTooltip(e.getGraphics(), e.getItemStack(), e.getX(), e.getY());
		});
	}

	@SubscribeEvent
	public static void replaceBookModel(ModelEvent.ModifyBakingResult evt) {
		ModelResourceLocation key = new ModelResourceLocation(PatchouliItems.BOOK_ID, "inventory");
		evt.getModels().computeIfPresent(key, (k, oldModel) -> new BookModel(oldModel, evt.getModelBakery()));
	}
}
