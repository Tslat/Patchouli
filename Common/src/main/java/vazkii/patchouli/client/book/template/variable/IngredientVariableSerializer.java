package vazkii.patchouli.client.book.template.variable;

import com.google.gson.JsonElement;

import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Ingredient;

import vazkii.patchouli.api.IVariableSerializer;
import vazkii.patchouli.common.util.ItemStackUtil;

public class IngredientVariableSerializer implements IVariableSerializer<Ingredient> {
	@Override
	public Ingredient fromJson(JsonElement json) {
		return (json.isJsonPrimitive()) ? ItemStackUtil.loadIngredientFromString(json.getAsString()) : Ingredient.CODEC.decode(JsonOps.INSTANCE, json).get().left().get().getFirst();
	}

	@Override
	public JsonElement toJson(Ingredient stack) {
		return Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, stack).get().left().get();
	}
}
