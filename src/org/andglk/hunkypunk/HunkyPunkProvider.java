/*
	Copyright © 2009 Rafał Rzepecki <divided.mind@gmail.com>

	This file is part of Hunky Punk.

    Hunky Punk is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Hunky Punk is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Hunky Punk.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.andglk.hunkypunk;

import org.andglk.hunkypunk.HunkyPunk.Games;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class HunkyPunkProvider extends ContentProvider {
	private static final String DATABASE_NAME = "hunky_punk.db";
	private static final int DATABASE_VERSION = 2;
	private static final String GAMES_TABLE_NAME = "games";
	
	private static final int GAMES = 1;
	private static final int GAME_ID = 2;
	private static final int GAME_IFID = 3;
	
	private static final UriMatcher sUriMatcher;
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			final String query =
				"CREATE TABLE " + GAMES_TABLE_NAME + " ("
					+ Games._ID + " INTEGER PRIMARY KEY, "
					+ Games.IFID + " TEXT UNIQUE NOT NULL, "

					+ Games.TITLE + " TEXT, "
					+ Games.AUTHOR + " TEXT, "
					+ Games.LANGUAGE + " TEXT, "
					+ Games.HEADLINE + " TEXT, "
					+ Games.FIRSTPUBLISHED + " TEXT, "
					+ Games.GENRE + " TEXT, "
					+ Games.GROUP + " TEXT, "
					+ Games.DESCRIPTION + " TEXT, "
					+ Games.SERIES + " TEXT, "
					+ Games.SERIESNUMBER + " INTEGER, "
					+ Games.FORGIVENESS + " TEXT, "

					+ Games.PATH + " TEXT, "
					+ Games.LOOKED_UP
					+ ");";
			db.execSQL(query);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 1) {
				/* we don't need the old FILENAME column but there's nothing to be done */
				final String query = 
					"ALTER TABLE " + GAMES_TABLE_NAME + 
					" ADD COLUMN "
					+ Games.PATH + " TEXT;";
				db.execSQL(query);
				/* PATHs will get filled automatically on next scan */
			}
		}
	}
	
	private DatabaseHelper mOpenHelper;
	
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db;
		db = mOpenHelper.getWritableDatabase();
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		qb.setTables(GAMES_TABLE_NAME);
		switch (sUriMatcher.match(uri)) {
		case GAMES:
			break;
		case GAME_ID:
			qb.appendWhere(Games._ID + " = " + Long.toString(ContentUris.parseId(uri)));
			break;
		case GAME_IFID:
			qb.appendWhere(Games.IFID + " = ");
			qb.appendWhereEscapeString(uri.getPathSegments().get(1));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		
		String orderBy;
		if (TextUtils.isEmpty(sortOrder))
			orderBy = HunkyPunk.Games.DEFAULT_SORT_ORDER;
		else
			orderBy = sortOrder;
		
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case GAMES:
			return Games.CONTENT_TYPE;

		case GAME_ID:
		case GAME_IFID:
			return Games.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (sUriMatcher.match(uri) != GAMES)
			throw new IllegalArgumentException("Unknown URI " + uri);

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(GAMES_TABLE_NAME, Games.IFID, values);
		if (rowId > 0) {
			Uri gameUri = ContentUris.withAppendedId(Games.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(gameUri, null);
			return gameUri;
		}
		
		throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = 0;
		StringBuilder sb = null;
		switch (sUriMatcher.match(uri)) {
		case GAMES:
			count = db.delete(GAMES_TABLE_NAME, selection, selectionArgs);
			break;
		case GAME_ID:
			sb = new StringBuilder(Games._ID + " = " + Long.toString(ContentUris.parseId(uri)));
			break;
		case GAME_IFID:
			sb = new StringBuilder(Games.IFID + " = \'" + DatabaseUtils.sqlEscapeString(uri.getLastPathSegment()) + "\'");
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		if (sb != null) {
			if (selection != null)
				sb.append(" AND (" + selection + ")");
			count = db.delete(GAMES_TABLE_NAME, sb.toString(), selectionArgs);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = 0;
		StringBuilder sb = null;
		switch (sUriMatcher.match(uri)) {
		case GAMES:
			count = db.update(GAMES_TABLE_NAME, values, selection, selectionArgs);
			break;
		case GAME_ID:
			sb = new StringBuilder(Games._ID + " = " + Long.toString(ContentUris.parseId(uri)));
			break;
		case GAME_IFID:
			sb = new StringBuilder(Games.IFID + " = " + DatabaseUtils.sqlEscapeString(uri.getLastPathSegment()));
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		if (sb != null) {
			if (selection != null)
				sb.append(" AND (" + selection + ")");
			count = db.update(GAMES_TABLE_NAME, values, sb.toString(), selectionArgs);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(HunkyPunk.AUTHORITY, "games", GAMES);
		sUriMatcher.addURI(HunkyPunk.AUTHORITY, "games/#", GAME_ID);
		sUriMatcher.addURI(HunkyPunk.AUTHORITY, "games/*", GAME_IFID);
	}
}
