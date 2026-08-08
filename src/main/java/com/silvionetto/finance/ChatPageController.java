package com.silvionetto.finance;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

	@GetMapping("/")
	public String chatPage() {
		return "chat";
	}
}
