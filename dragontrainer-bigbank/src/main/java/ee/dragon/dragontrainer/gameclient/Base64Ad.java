package ee.dragon.dragontrainer.gameclient;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Base64Ad {

	public static final int FORM = 1;
	public static final int ROT13 = 2;

	private Base64Ad() {
	}

	public static String open(Integer encrypted, String value) {
		if (encrypted == null || value == null || value.isBlank()) {
			return value;
		}
		if (encrypted == FORM) {
			return decodeBase64(value);
		}
		if (encrypted == ROT13) {
			return rot13(value);
		}
		return value;
	}

	private static String decodeBase64(String value) {
		String compact = value.replaceAll("\\s", "");
		try {
			byte[] decoded = decodeBytes(compact);
			String text = StandardCharsets.UTF_8
					.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(decoded))
					.toString();
			if (text.isBlank() || text.chars().anyMatch(Character::isISOControl)) {
				return value;
			}
			return text;
		} catch (IllegalArgumentException | CharacterCodingException ex) {
			return value;
		}
	}

	private static byte[] decodeBytes(String compact) {
		try {
			return Base64.getDecoder().decode(pad(compact));
		} catch (IllegalArgumentException standard) {
			return Base64.getUrlDecoder().decode(pad(compact));
		}
	}

	private static String pad(String value) {
		int remainder = value.length() % 4;
		if (remainder == 0) {
			return value;
		}
		return value + "=".repeat(4 - remainder);
	}

	private static String rot13(String value) {
		StringBuilder out = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch >= 'a' && ch <= 'z') {
				out.append((char) ('a' + (ch - 'a' + 13) % 26));
			} else if (ch >= 'A' && ch <= 'Z') {
				out.append((char) ('A' + (ch - 'A' + 13) % 26));
			} else {
				out.append(ch);
			}
		}
		return out.toString();
	}
}
