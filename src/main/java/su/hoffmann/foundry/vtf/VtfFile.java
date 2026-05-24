package su.hoffmann.foundry.vtf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VtfFile {

	private static final int VTF_SIGNATURE = 0x00465456;
	private static final int ENVMAP_FLAG = 0x00000080;

	private final VtfHeader header;
	private final ByteBuffer buffer;
	private final int highResolutionDataOffset;

	private VtfFile(final VtfHeader header, final ByteBuffer buffer, final int highResolutionDataOffset) {
		this.header = header;
		this.buffer = buffer;
		this.highResolutionDataOffset = highResolutionDataOffset;
	}

	public static VtfFile load(final Path path) throws IOException {
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			final ByteBuffer buf = ByteBuffer.allocateDirect((int) channel.size()).order(ByteOrder.LITTLE_ENDIAN);
			while (buf.hasRemaining()) {
				if (channel.read(buf) == -1) {
					break;
				}
			}
			buf.flip();
			return parse(buf);
		}
	}

	public static VtfFile parse(final ByteBuffer buffer) {
		if (buffer.getInt() != VTF_SIGNATURE) {
			throw new RuntimeException();
		}

		final int versionMajor = buffer.getInt();
		final int versionMinor = buffer.getInt();
		final int headerSize = buffer.getInt();
		final int width = buffer.getShort() & 0xFFFF;
		final int height = buffer.getShort() & 0xFFFF;
		final int flags = buffer.getInt();
		final int frames = buffer.getShort() & 0xFFFF;
		final int firstFrame = buffer.getShort() & 0xFFFF;

		buffer.position(buffer.position() + 4);

		final float[] reflectivity = { buffer.getFloat(), buffer.getFloat(), buffer.getFloat() };
		final float bumpmapScale = buffer.getFloat();
		final int highResFormatId = buffer.getInt();
		final int mipmapCount = buffer.get() & 0xFF;
		final int lowResFormatId = buffer.getInt();
		final int lowResWidth = buffer.get() & 0xFF;
		final int lowResHeight = buffer.get() & 0xFF;

		int depth = 1;
		if ((versionMajor > 7) || ((versionMajor == 7) && (versionMinor >= 2))) {
			depth = buffer.getShort() & 0xFFFF;
		}

		int highResOffset = headerSize;

		if ((lowResFormatId != -1) && (lowResWidth > 0) && (lowResHeight > 0)) {
			final VtfImageFormat lowFormat = VtfImageFormat.fromId(lowResFormatId);
			highResOffset += lowFormat.calculateSize(lowResWidth, lowResHeight);
		}

		if ((versionMajor > 7) || ((versionMajor == 7) && (versionMinor >= 3))) {
			buffer.position(headerSize - 8);
			buffer.position(buffer.position() + 3);
			final int numResources = buffer.getInt();

			for (int i = 0; i < numResources; i++) {
				final byte[] tag = new byte[4];
				buffer.get(tag);
				buffer.getInt();
				final int resOffset = buffer.getInt();

				if ((tag[0] == 0x30) && (tag[1] == 0x00) && (tag[2] == 0x00) && (tag[3] == 0x00)) {
					highResOffset = resOffset;
				}
			}
		}

		final VtfHeader header = new VtfHeader(versionMajor, versionMinor, width, height, flags, frames, firstFrame,
				reflectivity, bumpmapScale, VtfImageFormat.fromId(highResFormatId), mipmapCount, depth);

		return new VtfFile(header, buffer, highResOffset);
	}

	public int[] decode(final int mipLevel, final int frame, final int face, final int slice) {
		final int mipWidth = Math.max(1, header.width() >> mipLevel);
		final int mipHeight = Math.max(1, header.height() >> mipLevel);
		final int[] output = new int[mipWidth * mipHeight];

		final int dataOffset = calculateDataOffset(mipLevel, frame, face, slice);
		buffer.position(dataOffset);

		header.format().decode(buffer, output, mipWidth, mipHeight);
		return output;
	}

	private int calculateDataOffset(final int targetMip, final int frame, final int face, final int slice) {
		int currentOffset = highResolutionDataOffset;
		final int faces = (header.flags() & ENVMAP_FLAG) != 0 ? 6 : 1;

		for (int m = header.mipmapCount() - 1; m >= 0; m--) {
			final int mWidth = Math.max(1, header.width() >> m);
			final int mHeight = Math.max(1, header.height() >> m);
			final int mDepth = Math.max(1, header.depth() >> m);

			final int sizeOfOneImage = header.format().calculateSize(mWidth, mHeight);

			if (m == targetMip) {
				final int imageIndex = (frame * faces * mDepth) + (face * mDepth) + slice;
				return currentOffset + (imageIndex * sizeOfOneImage);
			}

			currentOffset += sizeOfOneImage * header.frames() * faces * mDepth;
		}

		throw new RuntimeException();
	}

	public VtfHeader getHeader() {
		return header;
	}
}