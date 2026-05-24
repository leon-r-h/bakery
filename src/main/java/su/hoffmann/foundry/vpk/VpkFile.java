package su.hoffmann.foundry.vpk;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Locale;

public final class VpkFile implements Closeable {

	private static final int VPK_SIGNATURE = 0x55aa1234;
	private static final short VPK_TERMINATOR = (short) 0xFFFF;
	private static final short VPK_EMBEDDED_ARCHIVE_INDEX = (short) 0x7FFF;

	private final Path dirFilePath;
	private final String baseName;
	private final int headerAndTreeSize;

	private final HashMap<String, VpkEntry> entries;
	private final HashMap<Short, FileChannel> archiveChannels;

	public VpkFile(final Path dirFilePath) throws IOException {
		this.dirFilePath = dirFilePath;
		entries = HashMap.newHashMap(8192 * 2);
		archiveChannels = HashMap.newHashMap(16);

		final String fileName = dirFilePath.getFileName().toString();
		baseName = fileName.endsWith("_dir.vpk") ? fileName.substring(0, fileName.length() - 8)
				: fileName.substring(0, fileName.length() - 4);

		try (final FileChannel dirChannel = FileChannel.open(dirFilePath, StandardOpenOption.READ)) {
			final ByteBuffer headerBuffer = ByteBuffer.allocateDirect(12).order(ByteOrder.LITTLE_ENDIAN);
			readFully(dirChannel, headerBuffer, 0);
			headerBuffer.flip();

			if (headerBuffer.getInt() != VPK_SIGNATURE) {
				throw new IOException("Ungültige VPK-Signatur gefunden.");
			}

			final int version = headerBuffer.getInt();
			final int treeSize = headerBuffer.getInt();
			final int headerSize;

			if (version == 1) {
				headerSize = 12;
			} else if (version == 2) {
				headerSize = 28;
			} else {
				throw new IOException("VPK-Version " + version + " wird nicht unterstützt.");
			}

			headerAndTreeSize = headerSize + treeSize;

			final ByteBuffer treeBuffer = ByteBuffer.allocateDirect(treeSize).order(ByteOrder.LITTLE_ENDIAN);
			readFully(dirChannel, treeBuffer, headerSize);
			treeBuffer.flip();

			parseTree(treeBuffer);

			archiveChannels.put(VPK_EMBEDDED_ARCHIVE_INDEX, FileChannel.open(dirFilePath, StandardOpenOption.READ));
		}
	}

	private void parseTree(final ByteBuffer buffer) throws IOException {
		while (buffer.hasRemaining()) {
			final String extension = readNullTerminatedString(buffer);
			if (extension.isEmpty()) {
				break;
			}

			while (buffer.hasRemaining()) {
				final String directory = readNullTerminatedString(buffer);
				if (directory.isEmpty()) {
					break;
				}

				while (buffer.hasRemaining()) {
					final String fileName = readNullTerminatedString(buffer);
					if (fileName.isEmpty()) {
						break;
					}

					final int crc = buffer.getInt();
					final short preloadBytes = buffer.getShort();
					final short archiveIndex = buffer.getShort();
					final int entryOffset = buffer.getInt();
					final int entryLength = buffer.getInt();

					if (buffer.getShort() != VPK_TERMINATOR) {
						throw new IOException("Ungültiger VPK-Terminator im Dateibaum gefunden.");
					}

					byte[] preloadData = null;
					if (preloadBytes > 0) {
						preloadData = new byte[preloadBytes];
						buffer.get(preloadData);
					}

					final String fullPath = constructFullPath(directory, fileName, extension);
					entries.put(fullPath, new VpkEntry(crc, preloadData, archiveIndex, entryOffset, entryLength));
				}
			}
		}
	}

	private String constructFullPath(final String directory, final String fileName, final String extension) {
		final boolean hasDir = !directory.equals(" ");
		final boolean hasExt = !extension.equals(" ");

		final int capacity = (hasDir ? directory.length() + 1 : 0) + fileName.length()
				+ (hasExt ? extension.length() + 1 : 0);
		final StringBuilder stringBuilder = new StringBuilder(capacity);

		if (hasDir) {
			stringBuilder.append(directory).append('/');
		}

		stringBuilder.append(fileName);

		if (hasExt) {
			stringBuilder.append('.').append(extension);
		}

		return stringBuilder.toString().toLowerCase(Locale.ROOT);
	}

	private String readNullTerminatedString(final ByteBuffer buffer) {
		final int startPos = buffer.position();
		while (buffer.get() != 0) {
		}

		final int length = buffer.position() - startPos - 1;
		if (length == 0) {
			return "";
		}

		final byte[] bytes = new byte[length];
		buffer.position(startPos);
		buffer.get(bytes);
		buffer.get();

		return new String(bytes, StandardCharsets.US_ASCII);
	}

	public boolean exists(final String path) {
		return entries.containsKey(path.toLowerCase(Locale.ROOT));
	}

	public byte[] readFile(final String path) throws IOException {
		final VpkEntry entry = entries.get(path.toLowerCase(Locale.ROOT));
		if (entry == null) {
			throw new FileNotFoundException("Datei in VPK nicht gefunden: " + path);
		}

		final int totalSize = (entry.preloadData != null ? entry.preloadData.length : 0) + entry.entryLength;
		final ByteBuffer resultBuffer = ByteBuffer.allocate(totalSize);

		if (entry.preloadData != null) {
			resultBuffer.put(entry.preloadData);
		}

		if (entry.entryLength > 0) {
			final FileChannel archiveChannel = getArchiveChannel(entry.archiveIndex);

			final long absoluteOffset = (entry.archiveIndex == VPK_EMBEDDED_ARCHIVE_INDEX)
					? headerAndTreeSize + entry.entryOffset
					: entry.entryOffset;

			resultBuffer.limit(resultBuffer.position() + entry.entryLength);
			readFully(archiveChannel, resultBuffer, absoluteOffset);
		}

		return resultBuffer.array();
	}

	private FileChannel getArchiveChannel(final short archiveIndex) throws IOException {
		FileChannel channel = archiveChannels.get(archiveIndex);
		if (channel != null) {
			return channel;
		}

		final String chunkNumber = archiveIndex < 10 ? "00" + archiveIndex
				: archiveIndex < 100 ? "0" + archiveIndex : Short.toString(archiveIndex);
		final String chunkName = baseName + "_" + chunkNumber + ".vpk";
		final Path chunkPath = dirFilePath.resolveSibling(chunkName);

		if (!Files.exists(chunkPath)) {
			throw new FileNotFoundException("VPK Archiv-Block nicht gefunden: " + chunkPath);
		}

		channel = FileChannel.open(chunkPath, StandardOpenOption.READ);
		archiveChannels.put(archiveIndex, channel);

		return channel;
	}

	private void readFully(final FileChannel channel, final ByteBuffer buffer, long position) throws IOException {
		while (buffer.hasRemaining()) {
			final int read = channel.read(buffer, position);
			if (read == -1) {
				throw new IOException("Unerwartetes Dateiende beim Lesen des VPK-Archivs.");
			}
			position += read;
		}
	}

	@Override
	public void close() throws IOException {
		for (final FileChannel channel : archiveChannels.values()) {
			if ((channel != null) && channel.isOpen()) {
				channel.close();
			}
		}

		archiveChannels.clear();
		entries.clear();
	}

	private record VpkEntry(int crc, byte[] preloadData, short archiveIndex, int entryOffset, int entryLength) {
	}
}