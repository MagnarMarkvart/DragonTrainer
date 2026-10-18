package ee.dragon.dragontrainer.gameclient;

import static org.assertj.core.api.Assertions.assertThat;

import ee.dragon.dragontrainer.gameclient.dto.MessageResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class Base64AdTest {

	@Test
	void open_decodesIdMessageAndProbabilityWhenTheGameMarksBase64() {
		MessageResponse message = new MessageResponse(
				encode("SQmuFkbf"),
				encode("Infiltrate The Jackals and recover their secrets."),
				"126",
				2,
				encode("Risky"),
				Base64Ad.FORM);

		assertThat(message.adId()).isEqualTo("SQmuFkbf");
		assertThat(message.message()).isEqualTo("Infiltrate The Jackals and recover their secrets.");
		assertThat(message.probability()).isEqualTo("Risky");
		assertThat(message.difficultyLabel()).isEqualTo("Risky");
		assertThat(message.encrypted()).isEqualTo(Base64Ad.FORM);
	}

	@Test
	void open_acceptsBase64ThatArrivedWithoutPadding() {
		assertThat(Base64Ad.open(Base64Ad.FORM, "Umlza3k")).isEqualTo("Risky");
	}

	@Test
	void open_leavesPlainTextAlone() {
		assertThat(Base64Ad.open(null, "Sure thing")).isEqualTo("Sure thing");
		assertThat(Base64Ad.open(Base64Ad.FORM, "Sure thing")).isEqualTo("Sure thing");
	}

	@Test
	void open_rot13TurnsLettersAndLeavesTheRest() {
		assertThat(Base64Ad.open(Base64Ad.ROT13, "Fher guvat")).isEqualTo("Sure thing");
		assertThat(Base64Ad.open(Base64Ad.ROT13, "Uryc n sevraq.")).isEqualTo("Help a friend.");
	}

	@Test
	void open_decodesALiveBase64AdIncludingHmmmAndAnAccent() {
		MessageResponse message = new MessageResponse(
				"NTZuVVNsdkI=",
				"SW52ZXN0aWdhdGUgT2xlZ8OhcmlvIEFpbWFyIGFuZCBmaW5kIG91dCB0aGVpciByZWxhdGlvbiB0byB0aGUgbWFnaWMgc3F1aXJyZWwu",
				"130",
				2,
				"SG1tbS4uLi4=",
				Base64Ad.FORM);

		assertThat(message.adId()).isEqualTo("56nUSlvB");
		assertThat(message.message())
				.isEqualTo("Investigate Olegário Aimar and find out their relation to the magic squirrel.");
		assertThat(message.probability()).isEqualTo("Hmmm....");
		assertThat(message.difficultyLabel()).isEqualTo("Hmmm....");
	}

	@Test
	void open_keepsTheOriginalWhenTheBytesAreNotText() {
		assertThat(Base64Ad.open(Base64Ad.FORM, encode(new byte[] {0x00, 0x01, 0x02}))).isEqualTo(encode(new byte[] {0x00, 0x01, 0x02}));
	}

	private static String encode(String value) {
		return encode(value.getBytes(StandardCharsets.UTF_8));
	}

	private static String encode(byte[] value) {
		return Base64.getEncoder().encodeToString(value);
	}
}
