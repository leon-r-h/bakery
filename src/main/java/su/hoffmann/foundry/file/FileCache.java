package su.hoffmann.foundry.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class FileCache<T> {

	private final SourceFileSystem fileSystem;
	private final Parser<T> parser;
	private final Map<String, T> cache;

	public FileCache(final SourceFileSystem fileSystem, final Parser<T> parser) {
		this.fileSystem = fileSystem;
		this.parser = parser;
		cache = HashMap.newHashMap(32768);
	}

	public T get(final String path) throws IOException {
		final ByteBuffer temporaryBuffer = fileSystem.readTemporary(path);
		if (temporaryBuffer != null) {
			return parser.parse(temporaryBuffer);
		}

		final T cached = cache.get(path);
		if (cached != null) {
			return cached;
		}

		final ByteBuffer globalBuffer = fileSystem.readGlobal(path);
		if (globalBuffer != null) {
			final T parsed = parser.parse(globalBuffer);
			cache.put(path, parsed);
			return parsed;
		}

		return null;
	}

	public void clear() {
		cache.clear();
	}

	// TODO: REMOVE AFTER A WHILE?! DEBUG SHIT
	@Deprecated
	public void statistics() {

	}

}
