package com.rsmaxwell.diaries.response.utilities;

import java.io.File;

public class MyFileUtilities {

	public static String removeExtension(File f) {
		return removeExtension(f.getName());
	}

	public static String removeExtension(String fname) {
		int pos = fname.lastIndexOf('.');
		if (pos > -1)
			return fname.substring(0, pos);
		else
			return fname;
	}

}
