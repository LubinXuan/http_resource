package com.adtime.common.date.extrator.regexparser.extractor;

import java.util.Map;

public class IdentityComponentParser extends ComponentParser {

	public IdentityComponentParser(Map map) {
		super(map);
	}
	
	@Override
	public String parse(String match) {
		return match;
	}

}
