package vazkii.patchouli.common.recipe;

import com.google.common.base.Preconditions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.ItemModBook;

/**
 * Recipe type for shapeless book recipes.
 * The format is the same as vanilla shapeless recipes, but the
 * "result" object is replaced by a "book" string for the book ID.
 */
public class ShapelessBookRecipe extends ShapelessRecipe {
	public static final Codec<ShapelessBookRecipe> CODEC = RecordCodecBuilder.create(builder -> builder.group(
					builder.group(
									ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(ShapelessRecipe::getGroup),
									CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
									ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(shapelessRecipe -> shapelessRecipe.getResultItem(RegistryAccess.EMPTY)),
									Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(ingredientList -> {
										Ingredient[] ingredients = ingredientList.stream().filter(ingredient -> !ingredient.isEmpty()).toArray(Ingredient[]::new);

										if (ingredients.length == 0)
											return DataResult.error(() -> "No ingredients for shapeless recipe");

										return ingredients.length > 9 ? DataResult.error(() -> "Too many ingredients for shapeless recipe") : DataResult.success(NonNullList.of(Ingredient.EMPTY, ingredients));
										}, DataResult::success).forGetter(ShapelessRecipe::getIngredients))
							.apply(builder, ShapelessRecipe::new),
					ResourceLocation.CODEC.fieldOf("book").flatXmap(id -> {
						if (!BookRegistry.INSTANCE.books.containsKey(id))
							PatchouliAPI.LOGGER.warn("Book {} in recipe does not exist!", id);

						return DataResult.success(id);
					}, DataResult::success).forGetter(shapelessRecipe -> ItemModBook.getBookId(shapelessRecipe.getResultItem(RegistryAccess.EMPTY))))
			.apply(builder, ShapelessBookRecipe::new));
	public static final RecipeSerializer<ShapelessBookRecipe> SERIALIZER = new BookRecipeSerializer<>(RecipeSerializer.SHAPELESS_RECIPE, CODEC, ShapelessBookRecipe::new);

	public ShapelessBookRecipe(ShapelessRecipe compose, @Nullable ResourceLocation outputBook) {
		super(compose.getGroup(), CraftingBookCategory.MISC, getOutputBook(compose, outputBook), compose.getIngredients());
	}

	private static ItemStack getOutputBook(ShapelessRecipe compose, @Nullable ResourceLocation outputBook) {
		Preconditions.checkArgument(compose.getClass() == ShapelessRecipe.class, "Must be exactly ShapelessRecipe");
		if (outputBook != null) {
			return PatchouliAPI.get().getBookStack(outputBook);
		}
		// The vanilla ShapelessRecipe implementation never uses the passed RegistryAccess, so this is ok.
		return compose.getResultItem(RegistryAccess.EMPTY);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
}
