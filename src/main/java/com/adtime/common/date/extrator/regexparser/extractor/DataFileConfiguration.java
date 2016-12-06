/*
 * encapsulates the configuration for a date parser stored inside a data file 
 *
 * @version $Id: DataFileConfiguration.java,v 1.1 2006/09/06 04:53:46 ahluwali Exp $
 * @author ahluwali
 */

package com.adtime.common.date.extrator.regexparser.extractor;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DataFileConfiguration {
	JSONObject origObj;

	JSONObject defaultObj = null; /* information about defaults */

	public DataFileConfiguration(JSONObject origObj,
			DataFileConfiguration defaultConfig) {
		this.origObj = origObj;

		if (defaultConfig != null) {
			this.defaultObj = defaultConfig.getOrigObj();
		}
	}

	private JSONObject getOrigObj() {
		return this.origObj;
	}

	/**
	 * get a string value for a key, defaulting to what the default
	 * configuration says
	 */
	public String getString(String key) {
		String override = this.origObj.getString(key);
		if (override != null)
			return override;
		else if (defaultObj != null)
			/* fallback to the default */
			return this.defaultObj.getString(key);
		else
			/* no default to fallback on, act as if not set */
			return null;
	}

	/**
	 * get an object value for a key, defaulting to what the default
	 * configuration says
	 */
	public JSONObject getObject(String key) {
		JSONObject override = this.origObj.getJSONObject(key);
		if (override != null)
			return override;
		else if (defaultObj != null)
			/* fallback to the default */
			return this.defaultObj.getJSONObject(key);
		else
			/* no default to fallback on, act as if not set */
			return null;
	}

	/**
	 * get an array value for a key, defaulting to what the default
	 * configuration says
	 */
	public JSONArray getArray(String key){
		JSONArray override = this.origObj.getJSONArray(key);
		if (override != null)
			return override;
		else if (defaultObj != null)
			/* fallback to the default */
			return this.defaultObj.getJSONArray(key);
		else
			/* no default to fallback on, act as if not set */
			return null;
	}

}
