package su.hoffmann.foundry.vtf;

public record VtfHeader(int versionMajor, int versionMinor, int width, int height, int flags, int frames,
		int firstFrame, float[] reflectivity, float bumpmapScale, VtfImageFormat format, int mipmapCount, int depth) {
}