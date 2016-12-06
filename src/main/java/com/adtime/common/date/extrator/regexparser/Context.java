/*
 * default parsing context to use for default component parsers
 *
 */

package com.adtime.common.date.extrator.regexparser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class Context {
	Set<String> componentKeys = null;
	
	Logger log;

	/* per component-default values */
	Map<String, String> defaults;
	Map<String, String> parsedFields; 
	
	ContextKeyProvider keyProvider = null;

	public Context(Map<String, String> defaults, ContextKeyProvider keyProvider) throws Exception {
		if (keyProvider != null) {
			this.keyProvider = keyProvider;
			componentKeys = keyProvider.getValidKeys();
			verifyDefaults(defaults);
		} else {
			throw new IllegalArgumentException("keyProvider can not be null");
		}
		this.defaults = defaults;
		this.parsedFields = new HashMap<String, String>();
	}

	public Context(Map<String, String> defaults) throws Exception {
		this.defaults = defaults;
		this.parsedFields = new HashMap<String, String>();
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void reset() {
		parsedFields.clear();
	}

	public boolean isSet(String key) {
		if (parsedFields.get(key) != null) {
			return true;
		}
		return false;
	}

	public void setComponent(String key, String value) {
		this.parsedFields.put(key, value);
	}

	public String getComponent(String key) {
		return this.parsedFields.get(key);
	}

	public String getDefaultForComponent(String key) {
		return this.defaults.get(key);
	}

	/** all fields must have a default value */
	public void verifyDefaults(Map defaults) throws Exception {
		Iterator<String> it = componentKeys.iterator();
		while (it.hasNext()) {
			String code = it.next();
			if (defaults.get(code) == null)
				throw new Exception("no default setting for field: "
						+ code);
		}
	}

	public void fillInDefaults() {
		/*
		 * for those fields that are not set, use the default values that were
		 * configured
		 */
		Iterator<String> it = componentKeys.iterator();
		while (it.hasNext()) {
			String code = it.next();
			if (!isSet(code)) {
				String defaultValue = getDefaultForComponent(code);
				/* set it to the default value */
				setComponent(code, defaultValue);
				if (this.log != null)
					this.log.info("component "
							+ code
							+ " not set, using default " + defaultValue);
			}
		}
	}

	public String toString() {
		StringBuffer buff = new StringBuffer();
		Iterator<String> it = componentKeys.iterator();
		while (it.hasNext()) {
			String code = it.next();
			buff.append(code);
			buff.append(": ");
			if (!isSet(code)) {
				buff.append(getDefaultForComponent(code));
				buff.append(" (default)");
			} else
				buff.append(getComponent(code));

			buff.append("\n");
		}

		return buff.toString();
	}

	/* get the date as an epoch time */
	public abstract String getResult();
}
