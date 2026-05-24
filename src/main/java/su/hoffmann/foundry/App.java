package su.hoffmann.foundry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.zip.ZipFile;

import info.ata4.bspsrc.lib.BspFile;
import info.ata4.bspsrc.lib.exceptions.BspException;
import info.ata4.bspsrc.lib.lump.Lump;
import info.ata4.bspsrc.lib.lump.LumpType;
import su.hoffmann.foundry.file.FileCache;
import su.hoffmann.foundry.file.PakSearchPath;
import su.hoffmann.foundry.file.SourceFileSystem;
import su.hoffmann.foundry.file.VpkSearchPath;
import su.hoffmann.foundry.kv.KvNode;
import su.hoffmann.foundry.kv.KvParser;

public final class App {
	private App() {
		try {
			final SourceFileSystem sourceFileSystem = new SourceFileSystem();
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_misc_dir.vpk")));
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_pak_dir.vpk")));
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_sound_misc_dir.vpk")));
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_textures_dir.vpk")));
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_sound_vo_english_dir.vpk")));
			sourceFileSystem.addSearchPath(new VpkSearchPath(Path.of("hl2/hl2_sound_vo_german_dir.vpk")));

			final FileCache<KvNode> materialCache = new FileCache<>(sourceFileSystem, KvParser::parse);

			Files.walk(Path.of("./hl2/maps/")).forEach(mapPath -> {
				if (Files.isDirectory(mapPath) || !mapPath.getFileName().toString().endsWith(".bsp")) {
					return;
				}
				System.out.println("Lade Map: " + mapPath.getFileName());
				try {
					loadBspFile(sourceFileSystem, materialCache, mapPath);
				} catch (final Exception e) {
					System.err.println("Fehler beim Laden der Map " + mapPath.getFileName() + ": " + e.getMessage());
				}
			});

			materialCache.statistics();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private void loadBspFile(final SourceFileSystem sourceFileSystem, final FileCache<KvNode> materialCache,
			final Path path) throws IOException, BspException {
		final BspFile bspFile = new BspFile(path);
		final ZipFile pakZipFile = bspFile.getPakFile().getZipFile();
		sourceFileSystem.mountTemporary(new PakSearchPath(pakZipFile));
		final Lump stringDataLump = bspFile.getLump(LumpType.LUMP_TEXDATA_STRING_DATA);
		final ByteBuffer stringDataBuffer = stringDataLump.getBuffer();
		final byte[] rawBytes = new byte[stringDataBuffer.capacity()];
		stringDataBuffer.get(rawBytes);
		final Lump stringTableLump = bspFile.getLump(LumpType.LUMP_TEXDATA_STRING_TABLE);
		final ByteBuffer stringTableBuffer = stringTableLump.getBuffer();
		stringTableBuffer.order(bspFile.getByteOrder());
		final int stringCount = stringTableBuffer.capacity() / 4;

		for (int i = 0; i < stringCount; i++) {
			final int offset = stringTableBuffer.getInt();
			int length = 0;
			while (((offset + length) < rawBytes.length) && (rawBytes[offset + length] != 0)) {
				length++;
			}

			final String identifier = new String(rawBytes, offset, length, StandardCharsets.US_ASCII);
			final String vmtPath = "materials/" + identifier + ".vmt";

			final KvNode materialNode = materialCache.get(vmtPath);
			if (materialNode == null) {
				System.err.println("  -> Material fehlt: " + vmtPath);
			} else {
//				 System.out.println("Erfolgreich geladen: " + vmtPath);
			}
		}

		sourceFileSystem.unmountTemporary();
	}

	public static void main(final String[] args) {
		new App();
	}
}