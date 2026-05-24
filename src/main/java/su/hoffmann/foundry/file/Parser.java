package su.hoffmann.foundry.file;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Parser<T> {

	T parse(ByteBuffer data) throws IOException;

}