package su.hoffmann.foundry.kv;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class KvParser {

	private KvParser() {
	}

	public static KvNode parse(final ByteBuffer data) {
		// TODO: Implement Lexer/Parser directly based on ByteBuffers
		return new ParserImplementation(new KvLexer(StandardCharsets.US_ASCII.decode(data).toString())).parseFile();
	}

	private static class ParserImplementation {
		private final KvLexer lexer;
		private KvLexer.Token current;

		private ParserImplementation(final KvLexer lexer) {
			this.lexer = lexer;
			current = lexer.next();
		}

		private void eat(final KvLexer.TokenType type) {
			if (current.type() != type) {
				throw new RuntimeException();
			}
			current = lexer.next();
		}

		private void skipConditionals() {
			if (current.type() == KvLexer.TokenType.LBRACKET) {
				eat(KvLexer.TokenType.LBRACKET);
				while ((current.type() != KvLexer.TokenType.RBRACKET) && (current.type() != KvLexer.TokenType.EOF)) {
					current = lexer.next();
				}
				eat(KvLexer.TokenType.RBRACKET);
			}
		}

		KvNode parseFile() {
			final List<KvNode> rootChildren = new ArrayList<>();
			while (current.type() != KvLexer.TokenType.EOF) {
				rootChildren.add(parseNode());
			}
			return new KvNode("", null, rootChildren);
		}

		private KvNode parseNode() {
			if (current.type() != KvLexer.TokenType.STRING) {
				throw new RuntimeException();
			}

			final String key = current.value();
			eat(KvLexer.TokenType.STRING);
			skipConditionals();

			if (current.type() == KvLexer.TokenType.LBRACE) {
				eat(KvLexer.TokenType.LBRACE);
				final List<KvNode> children = new ArrayList<>();
				while ((current.type() != KvLexer.TokenType.RBRACE) && (current.type() != KvLexer.TokenType.EOF)) {
					children.add(parseNode());
				}
				eat(KvLexer.TokenType.RBRACE);
				return new KvNode(key, null, children);
			}
			if (current.type() == KvLexer.TokenType.STRING) {
				final String value = current.value();
				eat(KvLexer.TokenType.STRING);
				skipConditionals();
				return new KvNode(key, value, null);
			}
			throw new RuntimeException();
		}
	}
}