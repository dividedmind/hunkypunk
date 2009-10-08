package org.andglk.hunkypunk;

import java.io.File;

import android.net.Uri;
import android.provider.BaseColumns;

public final class HunkyPunk {
	public static final String AUTHORITY = "org.andglk.hunkypunk.HunkyPunk";
	public static final File DIRECTORY = new File("/sdcard/Interactive Fiction");
	private HunkyPunk() {}
	
	public static final class Games implements BaseColumns {
		private Games() {}
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/games");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.andglk.game";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.andglk.game";
		public static final String DEFAULT_SORT_ORDER = "lower(title) ASC";
		
		public static final String TITLE = "title";
		public static final String IFID = "ifid";
		public static final String FILENAME = "filename";
	}
}