package su.hoffmann.foundry.file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

import su.hoffmann.foundry.vpk.VpkFile;

public final class VpkSearchPath implements SearchPath {

	private final VpkFile vpkFile;

	public VpkSearchPath(final Path vpkDirFilePath) throws IOException {
		vpkFile = new VpkFile(vpkDirFilePath);
	}

	@Override
	public ByteBuffer read(final String path) throws IOException {
		if (!vpkFile.exists(path)) {
			return null;
		}

		final byte[] data = vpkFile.readFile(path);
		if (data == null) {
			return null;
		}

		return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public boolean exists(final String path) {
		return vpkFile.exists(path);
	}

	@Override
	public void close() throws IOException {
		vpkFile.close();
	}
}