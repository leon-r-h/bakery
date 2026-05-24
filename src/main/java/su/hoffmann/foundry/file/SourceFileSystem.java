package su.hoffmann.foundry.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public final class SourceFileSystem implements Closeable {

	private final ArrayList<SearchPath> searchPaths;
	private SearchPath temporaryMount;

	public SourceFileSystem() {
		searchPaths = new ArrayList<>(32);
	}

	public void addSearchPath(final SearchPath path) {
		searchPaths.add(path);
	}

	public void mountTemporary(final SearchPath path) {
		temporaryMount = path;
	}

	public void unmountTemporary() throws IOException {
		if (temporaryMount != null) {
			temporaryMount.close();
			temporaryMount = null;
		}
	}

	public ByteBuffer readTemporary(final String path) throws IOException {
		return temporaryMount != null ? temporaryMount.read(path) : null;
	}

	public ByteBuffer readGlobal(final String path) throws IOException {
		for (final SearchPath searchPath : searchPaths) {
			final ByteBuffer result = searchPath.read(path);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	@Override
	public void close() throws IOException {
		unmountTemporary();
		for (final SearchPath searchPath : searchPaths) {
			searchPath.close();
		}
	}
}