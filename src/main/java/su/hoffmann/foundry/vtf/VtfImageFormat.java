package su.hoffmann.foundry.vtf;

import java.nio.ByteBuffer;

public enum VtfImageFormat {
	RGBA8888(0, 4, 1, 1, VtfImageFormat::decodeRGBA8888), ABGR8888(1, 4, 1, 1, VtfImageFormat::decodeABGR8888),
	RGB888(2, 3, 1, 1, VtfImageFormat::decodeRGB888), BGR888(3, 3, 1, 1, VtfImageFormat::decodeBGR888),
	RGB565(4, 2, 1, 1, VtfImageFormat::decodeRGB565), I8(5, 1, 1, 1, VtfImageFormat::decodeI8),
	IA88(6, 2, 1, 1, VtfImageFormat::decodeIA88), BGRA8888(12, 4, 1, 1, VtfImageFormat::decodeBGRA8888),
	DXT1(13, 8, 4, 4, VtfImageFormat::decodeDXT1), DXT3(14, 16, 4, 4, VtfImageFormat::decodeDXT3),
	DXT5(15, 16, 4, 4, VtfImageFormat::decodeDXT5), DXT1_ONEBITALPHA(20, 8, 4, 4, VtfImageFormat::decodeDXT1),
	RGBA16161616F(24, 8, 1, 1, VtfImageFormat::decodeRGBA16F),
	RGBA32323232F(29, 16, 1, 1, VtfImageFormat::decodeRGBA32F);

	private final int id;
	private final int bytesPerBlock;
	private final int blockWidth;
	private final int blockHeight;
	private final Decoder decoder;

	private VtfImageFormat(final int id, final int bytesPerBlock, final int blockWidth, final int blockHeight,
			final Decoder decoder) {
		this.id = id;
		this.bytesPerBlock = bytesPerBlock;
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.decoder = decoder;
	}

	public static VtfImageFormat fromId(final int id) {
		for (final VtfImageFormat format : values()) {
			if (format.id == id) {
				return format;
			}
		}

		throw new RuntimeException();
	}

	public int calculateSize(final int width, final int height) {
		final int blocksX = ((width + blockWidth) - 1) / blockWidth;
		final int blocksY = ((height + blockHeight) - 1) / blockHeight;
		return blocksX * blocksY * bytesPerBlock;
	}

	public void decode(final ByteBuffer buffer, final int[] output, final int width, final int height) {
		decoder.decode(buffer, output, width, height);
	}

	@FunctionalInterface
	private interface Decoder {
		void decode(ByteBuffer buffer, int[] out, int width, int height);
	}

	private static void decodeRGBA8888(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			out[i] = packARGB(buffer.get(), buffer.get(), buffer.get(), buffer.get());
		}
	}

	private static void decodeBGRA8888(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int b = buffer.get(), g = buffer.get(), r = buffer.get(), a = buffer.get();
			out[i] = packARGB(r, g, b, a);
		}
	}

	private static void decodeABGR8888(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int a = buffer.get(), b = buffer.get(), g = buffer.get(), r = buffer.get();
			out[i] = packARGB(r, g, b, a);
		}
	}

	private static void decodeRGB888(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			out[i] = packARGB(buffer.get(), buffer.get(), buffer.get(), 255);
		}
	}

	private static void decodeBGR888(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int b = buffer.get(), g = buffer.get(), r = buffer.get();
			out[i] = packARGB(r, g, b, 255);
		}
	}

	private static void decodeRGB565(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int value = buffer.getShort() & 0xFFFF;
			final int r = (((value >> 11) & 0x1F) * 255) / 31;
			final int g = (((value >> 5) & 0x3F) * 255) / 63;
			final int b = ((value & 0x1F) * 255) / 31;
			out[i] = packARGB(r, g, b, 255);
		}
	}

	private static void decodeI8(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int l = buffer.get() & 0xFF;
			out[i] = packARGB(l, l, l, 255);
		}
	}

	private static void decodeIA88(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int l = buffer.get() & 0xFF;
			final int a = buffer.get() & 0xFF;
			out[i] = packARGB(l, l, l, a);
		}
	}

	private static void decodeRGBA16F(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int r = clampFloatToByte(Float.float16ToFloat(buffer.getShort()));
			final int g = clampFloatToByte(Float.float16ToFloat(buffer.getShort()));
			final int b = clampFloatToByte(Float.float16ToFloat(buffer.getShort()));
			final int a = clampFloatToByte(Float.float16ToFloat(buffer.getShort()));
			out[i] = packARGB(r, g, b, a);
		}
	}

	private static void decodeRGBA32F(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		for (int i = 0; i < out.length; i++) {
			final int r = clampFloatToByte(buffer.getFloat());
			final int g = clampFloatToByte(buffer.getFloat());
			final int b = clampFloatToByte(buffer.getFloat());
			final int a = clampFloatToByte(buffer.getFloat());
			out[i] = packARGB(r, g, b, a);
		}
	}

	private static void decodeDXT1(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		final int blocksX = (w + 3) / 4;
		final int blocksY = (h + 3) / 4;
		for (int by = 0; by < blocksY; by++) {
			for (int bx = 0; bx < blocksX; bx++) {
				decodeDXT1Block(buffer, out, bx * 4, by * 4, w, h);
			}
		}
	}

	private static void decodeDXT3(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		final int blocksX = (w + 3) / 4;
		final int blocksY = (h + 3) / 4;
		for (int by = 0; by < blocksY; by++) {
			for (int bx = 0; bx < blocksX; bx++) {
				final long alphaData = buffer.getLong();
				decodeDXTBlockColor(buffer, out, bx * 4, by * 4, w, h, true, alphaData, 3);
			}
		}
	}

	private static void decodeDXT5(final ByteBuffer buffer, final int[] out, final int w, final int h) {
		final int blocksX = (w + 3) / 4;
		final int blocksY = (h + 3) / 4;
		for (int by = 0; by < blocksY; by++) {
			for (int bx = 0; bx < blocksX; bx++) {
				final int a0 = buffer.get() & 0xFF;
				final int a1 = buffer.get() & 0xFF;
				long alphaIndices = 0;
				for (int i = 0; i < 6; i++) {
					alphaIndices |= (buffer.get() & 0xFFL) << (8 * i);
				}

				final long packedAlphaHeader = ((long) a1 << 56) | ((long) a0 << 48) | alphaIndices;
				decodeDXTBlockColor(buffer, out, bx * 4, by * 4, w, h, true, packedAlphaHeader, 5);
			}
		}
	}

	private static void decodeDXT1Block(final ByteBuffer buffer, final int[] out, final int x, final int y, final int w,
			final int h) {
		decodeDXTBlockColor(buffer, out, x, y, w, h, false, 0, 1);
	}

	private static void decodeDXTBlockColor(final ByteBuffer buffer, final int[] out, final int x, final int y,
			final int w, final int h, final boolean hasAlpha, final long alphaData, final int dxtVersion) {
		final int color0 = buffer.getShort() & 0xFFFF;
		final int color1 = buffer.getShort() & 0xFFFF;
		final int code = buffer.getInt();

		final int r0 = (((color0 >>> 11) & 0x1F) * 255) / 31;
		final int g0 = (((color0 >>> 5) & 0x3F) * 255) / 63;
		final int b0 = ((color0 & 0x1F) * 255) / 31;

		final int r1 = (((color1 >>> 11) & 0x1F) * 255) / 31;
		final int g1 = (((color1 >>> 5) & 0x3F) * 255) / 63;
		final int b1 = ((color1 & 0x1F) * 255) / 31;

		for (int j = 0; j < 4; j++) {
			for (int i = 0; i < 4; i++) {
				if (((x + i) >= w) || ((y + j) >= h)) {
					continue;
				}

				final int colorCode = (code >>> (2 * ((4 * j) + i))) & 0x03;

				int a = 255;
				if (dxtVersion == 3) {
					a = (int) ((alphaData >>> (4 * ((4 * j) + i))) & 0x0F) * 17;
				} else if (dxtVersion == 5) {
					final int a0 = (int) ((alphaData >>> 48) & 0xFF);
					final int a1 = (int) ((alphaData >>> 56) & 0xFF);
					final int aCode = (int) ((alphaData >>> (3 * ((4 * j) + i))) & 0x07);
					if (aCode == 0) {
						a = a0;
					} else if (aCode == 1) {
						a = a1;
					} else if (a0 > a1) {
						a = (((8 - aCode) * a0) + ((aCode - 1) * a1)) / 7;
					} else if (aCode == 6) {
						a = 0;
					} else if (aCode == 7) {
						a = 255;
					} else {
						a = (((6 - aCode) * a0) + ((aCode - 1) * a1)) / 5;
					}
				}

				int finalR = 0, finalG = 0, finalB = 0;
				if (colorCode == 0) {
					finalR = r0;
					finalG = g0;
					finalB = b0;
				} else if (colorCode == 1) {
					finalR = r1;
					finalG = g1;
					finalB = b1;
				} else if ((color0 > color1) || hasAlpha) {
					if (colorCode == 2) {
						finalR = ((2 * r0) + r1) / 3;
						finalG = ((2 * g0) + g1) / 3;
						finalB = ((2 * b0) + b1) / 3;
					} else {
						finalR = (r0 + (2 * r1)) / 3;
						finalG = (g0 + (2 * g1)) / 3;
						finalB = (b0 + (2 * b1)) / 3;
					}
				} else if (colorCode == 2) {
					finalR = (r0 + r1) / 2;
					finalG = (g0 + g1) / 2;
					finalB = (b0 + b1) / 2;
				} else {
					finalR = 0;
					finalG = 0;
					finalB = 0;
					a = 255;
				}

				out[((y + j) * w) + (x + i)] = packARGB(finalR, finalG, finalB, a);
			}
		}
	}

	private static int packARGB(final int r, final int g, final int b, final int a) {
		return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
	}

	private static int clampFloatToByte(final float f) {
		return (int) (Math.max(0.0f, Math.min(1.0f, f)) * 255.0f);
	}
}