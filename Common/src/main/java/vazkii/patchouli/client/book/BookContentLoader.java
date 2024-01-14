package vazkii.patchouli.client.book;

import com.google.gson.JsonElement;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public interface BookContentLoader {
	void findFiles(Book book, String dir, List<ResourceLocation> list);

	@Nullable
	LoadResult loadJson(Book book, ResourceLocation file);

	/**
	 * @param addedBy Opaque string to be shown to user in the book,
	 *                indicates that the thing in question was added by a "foreign" entity such
	 *                as a resource pack addon. Null if no message should be shown.
	 */
	record LoadResult(JsonElement json, @Nullable String addedBy) {
	}

	static JsonElement streamToJson(InputStream stream) throws IOException {
		try (Reader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			return GsonHelper.fromJson(BookRegistry.GSON, reader, JsonElement.class);
		}
	}
}
