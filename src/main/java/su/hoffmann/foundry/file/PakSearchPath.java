package su.hoffmann.foundry.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

public final class PakSearchPath implements SearchPath {

	private final ZipFile zipFile;

	public PakSearchPath(final ZipFile zipFile) {
		this.zipFile = zipFile;
	}

	@Override
	public ByteBuffer read(final String path) throws IOException {
		if (zipFile == null) {
			return null;
		}

		final ZipArchiveEntry entry = zipFile.getEntry(path);
		if (entry == null) {
			return null;
		}

		try (InputStream inputStream = zipFile.getInputStream(entry)) {
			return ByteBuffer.wrap(inputStream.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
		}
	}

	@Override
	public boolean exists(final String path) {
		return (zipFile != null) && (zipFile.getEntry(path) != null);
	}

	@Override
	public void close() throws IOException {
		if (zipFile != null) {
			zipFile.close();
		}
	}
}