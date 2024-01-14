package vazkii.patchouli.common.recipe;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.ItemModBook;
import vazkii.patchouli.common.item.PatchouliItems;
import vazkii.patchouli.mixin.AccessorShapedRecipe;

/**
 * Recipe type for shaped book recipes.
 * The format is the same as vanilla shaped recipes, but the
 * "result" object is replaced by a "book" string for the book ID.
 */
public class ShapedBookRecipe extends ShapedRecipe {
	public static final Codec<ShapedBookRecipe> CODEC = RecordCodecBuilder.create(builder -> builder.group(
					builder.group(
									ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapedRecipe::getGroup),
									CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
									ShapedRecipePattern.MAP_CODEC.forGetter(shapedRecipe -> ((AccessorShapedRecipe)shapedRecipe).getPattern()),
									ExtraCodecs.strictOptionalField(ItemStack.ITEM_WITH_COUNT_CODEC, "result", PatchouliItems.BOOK.getDefaultInstance()).forGetter(shapedRecipe -> shapedRecipe.getResultItem(RegistryAccess.EMPTY)),
									ExtraCodecs.strictOptionalField(Codec.BOOL, "show_notification", true).forGetter(ShapedRecipe::showNotification))
							.apply(builder, ShapedRecipe::new),
					ResourceLocation.CODEC.fieldOf("book").flatXmap(id -> {
						if (!BookRegistry.INSTANCE.books.containsKey(id))
							PatchouliAPI.LOGGER.warn("Book {} in recipe does not exist!", id);

						return DataResult.success(id);
					}, DataResult::success).forGetter(shapedRecipe -> ItemModBook.getBookId(shapedRecipe.getResultItem(RegistryAccess.EMPTY))))
			.apply(builder, ShapedBookRecipe::new));
	public static final RecipeSerializer<ShapedBookRecipe> SERIALIZER = new BookRecipeSerializer<>(RecipeSerializer.SHAPED_RECIPE, CODEC, ShapedBookRecipe::new);

	public ShapedBookRecipe(ShapedRecipe compose, @Nullable ResourceLocation outputBook) {
		super(compose.getGroup(), CraftingBookCategory.MISC, ((AccessorShapedRecipe)compose).getPattern(), getOutputBook(compose, outputBook));
	}

	private static ItemStack getOutputBook(ShapedRecipe compose, @Nullable ResourceLocation outputBook) {
		Preconditions.checkArgument(compose.getClass() == ShapedRecipe.class, "Must be exactly ShapedRecipe");
		if (outputBook != null) {
			return PatchouliAPI.get().getBookStack(outputBook);
		}
		// The vanilla ShapedRecipe implementation never uses the passed RegistryAccess, so this is ok.
		return compose.getResultItem(RegistryAccess.EMPTY);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
