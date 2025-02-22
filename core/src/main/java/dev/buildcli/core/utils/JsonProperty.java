package dev.buildcli.core.utils;

public enum JsonProperty {

	MODEL("model"),
	ROLE("role"),
	CONTENT("content"),
	MESSAGES("messages"),
	MESSAGE("message"),
	CHOICES("choices");
	
	private String prop;
	
	private JsonProperty(String prop) {
		this.prop = prop;
	}
	
	public String val() {
		return this.prop;
	}
}
