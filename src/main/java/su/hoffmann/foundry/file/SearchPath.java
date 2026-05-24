package su.hoffmann.foundry.file;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface SearchPath extends Closeable {

	ByteBuffer read(String path) throws IOException;

	boolean exists(String path);

}
