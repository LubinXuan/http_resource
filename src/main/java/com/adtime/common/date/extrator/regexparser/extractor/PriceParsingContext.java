package com.adtime.common.date.extrator.regexparser.extractor;

import com.adtime.common.date.extrator.regexparser.Context;
import com.adtime.common.date.extrator.regexparser.ContextKeyProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class PriceParsingContext extends Context {
	public static class PriceKeyProvider implements ContextKeyProvider {
		private static Set<String> keys = new HashSet<String>();
		static {
			keys.add("price");
		}
		public Set<String> getValidKeys() {
			return keys;
		}
		
	}
	
	public PriceParsingContext(Map<String, String> defaults) throws Exception {
		super(defaults, new PriceKeyProvider());
	}

	/* get the date as an epoch time */
	public String getResult() {
		String price = this.getComponent("price");
		return price;
	}
}
