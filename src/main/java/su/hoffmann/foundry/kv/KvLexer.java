package su.hoffmann.foundry.kv;

public final class KvLexer {

	public enum TokenType {
		STRING, LBRACE, RBRACE, LBRACKET, RBRACKET, EOF
	}

	public record Token(TokenType type, String value) {
	}

	private final String input;
	private int position = 0;

	public KvLexer(final String input) {
		this.input = input;
	}

	public Token next() {
		skipWhitespaceAndComments();

		if (position >= input.length()) {
			return new Token(TokenType.EOF, "");
		}

		final char character = input.charAt(position);
		switch (character) {
		case '{':
			position++;
			return new Token(TokenType.LBRACE, "{");
		case '}':
			position++;
			return new Token(TokenType.RBRACE, "}");
		case '[':
			position++;
			return new Token(TokenType.LBRACKET, "[");
		case ']':
			position++;
			return new Token(TokenType.RBRACKET, "]");
		default:
			break;
		}

		return character == '"' ? readQuoted() : readUnquoted();
	}

	private void skipWhitespaceAndComments() {
		while (position < input.length()) {
			final char character = input.charAt(position);

			if (Character.isWhitespace(character)) {
				position++;
			} else if ((character == '/') && ((position + 1) < input.length()) && (input.charAt(position + 1) == '/')) {
				while ((position < input.length()) && (input.charAt(position) != '\n')) {
					position++;
				}
			} else {
				break;
			}
		}
	}

	private Token readQuoted() {
		position++;
		final StringBuilder stringBuilder = new StringBuilder();

		while (position < input.length()) {
			char character = input.charAt(position);
			if (character == '"') {
				position++;
				return new Token(TokenType.STRING, stringBuilder.toString());
			}
			if ((character == '\\') && ((position + 1) < input.length())) {
				position++;
				character = input.charAt(position);
				switch (character) {
				case 'n':
					stringBuilder.append('\n');
					break;
				case 't':
					stringBuilder.append('\t');
					break;
				case '"':
					stringBuilder.append('"');
					break;
				case '\\':
					stringBuilder.append('\\');
					break;
				default:
					stringBuilder.append(character);
					break;
				}
			} else {
				stringBuilder.append(character);
			}
			position++;
		}

		throw new RuntimeException();
	}

	private Token readUnquoted() {
		final int startPos = position;

		while (position < input.length()) {
			final char character = input.charAt(position);
			if (Character.isWhitespace(character) || (character == '{') || (character == '}') || (character == '[')
					|| (character == ']') || (character == '"')) {
				break;
			}
			position++;
		}
		return new Token(TokenType.STRING, input.substring(startPos, position));
	}
}