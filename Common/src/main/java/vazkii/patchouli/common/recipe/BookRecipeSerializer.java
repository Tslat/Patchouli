package vazkii.patchouli.common.recipe;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public record BookRecipeSerializer<T extends Recipe<?>, U extends T> (RecipeSerializer<T> compose, Codec<U> codec, BiFunction<T, @Nullable ResourceLocation, U> converter) implements RecipeSerializer<U> {
	@Override
	public Codec<U> codec() {
		return codec;
	}

	@Override
	@NotNull
	public U fromNetwork(@NotNull FriendlyByteBuf buf) {
		T recipe = compose().fromNetwork(buf);

		return converter().apply(recipe, null);
	}

	@Override
	public void toNetwork(@NotNull FriendlyByteBuf buf, @NotNull U recipe) {
		compose().toNetwork(buf, recipe);
	}
}
