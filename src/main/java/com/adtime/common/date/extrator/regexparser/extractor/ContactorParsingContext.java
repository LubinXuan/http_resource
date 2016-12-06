package com.adtime.common.date.extrator.regexparser.extractor;

import com.adtime.common.date.extrator.regexparser.Context;
import com.adtime.common.date.extrator.regexparser.ContextKeyProvider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ContactorParsingContext extends Context {
	public static class ContactorKeyProvider implements ContextKeyProvider {
		private static Set<String> keys = new HashSet<String>();
		static {
			keys.add("emailuser");
			keys.add("domain");
		}
		public Set<String> getValidKeys() {
			return keys;
		}
		
	}

	public void setComponent(String key, String value) {
		if (key != null) {
			if (key.equals("phone")) {
				super.setComponent("id", "phone");
			} else if (key.equals("emailuser")) {
				super.setComponent("id", "email");
			} else if (key.equals("mobile")){
				super.setComponent("id", "mobile");
			} else if (key.equals("contactor")){
				super.setComponent("id", "contactor");
			}
		}
		super.setComponent(key, value);
	}
	
	public ContactorParsingContext(Map<String, String> defaults) throws Exception {
		super(defaults, new ContactorKeyProvider());
	}

	/* get the date as an epoch time */
	public String getResult() {
		String user = this.getComponent("emailuser");
		String domain = this.getComponent("domain");
		String phone = this.getComponent("phone");
		String mobile = this.getComponent("mobile");
		String contactor = this.getComponent("contactor");
		if (phone != null && !phone.equals("")) {
			return phone;
		} else if (mobile != null && !mobile.equals("")) {
			return mobile;
		} else if (contactor != null && !contactor.equals("")) {
			return contactor;
		}
		return user + "@" + domain;
	}
}
