package com.silvionetto.finance;

import java.util.Objects;

public record ChatReply(String response, ChatResponseFormat format) {

	public ChatReply {
		Objects.requireNonNull(response, "response must not be null");
		Objects.requireNonNull(format, "format must not be null");
	}
}
