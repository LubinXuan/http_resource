/*
 * utility that reads configuration files from disk
 */

package com.adtime.common.date.extrator.regexparser.extractor;

import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class DataFileReader {
	/* name of the defaults file for data file configurations */
	public static final String DEFAULT = "default";

	/*
	 * hash map from data file codes to DataFileConfiguration objects (i.e.
	 * files that have been read
	 */
	public static HashMap<String, DataFileConfiguration> dataFiles = new HashMap<String, DataFileConfiguration>();

	public static DataFileConfiguration defaultDataConfiguration = null;

	public static String filesDir = null;

	public static void setConfigPath(String dir) {
		if (filesDir != null && !filesDir.equals(dir))
			/*
			 * changing directories programmatically, clear the in-memory cache
			 */
			dataFiles.clear();

		filesDir = dir;
	}

	public static DataFileConfiguration getConfiguration(String dataFileCode)
			throws Exception {
		/* init the default configuration if not done so already */
		if (defaultDataConfiguration == null)
			defaultDataConfiguration = getConfiguration(DEFAULT, false);
		
		/* use default data file when no preference specified */
		return getConfiguration(dataFileCode, true);
	}

	public static DataFileConfiguration getConfiguration(String dataFileCode,
			boolean useDefault) throws Exception {
		DataFileConfiguration obj = (DataFileConfiguration) dataFiles
				.get(dataFileCode);
		if (obj == null) {
			/* not in the in-memory-cache, have to load it from disk */
			String data = getContents(filesDir + "/" + dataFileCode + ".json");
			JSONObject jsonObj = JSONObject.parseObject(data);
			obj = new DataFileConfiguration(jsonObj,
					(useDefault) ? defaultDataConfiguration : null);
			dataFiles.put(dataFileCode, obj);
		}

		/* return the object found/loaded */
		return obj;
	}

	/* load the contents of the given file into a string */
	public static String getContents(String fileName) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(fileName), "UTF-8"));
		String thisLine = reader.readLine();
		StringBuffer ret = new StringBuffer(thisLine);
		while ((thisLine = reader.readLine()) != null)
			ret.append("\n" + thisLine);

		return ret.toString();
	}

}
