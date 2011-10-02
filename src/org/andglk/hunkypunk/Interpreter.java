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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.andglk.glk.FileRef;
import org.andglk.glk.FileStream;
import org.andglk.glk.Glk;
import org.andglk.glk.Window;
import org.andglk.hunkypunk.HunkyPunk;
import org.andglk.hunkypunk.R;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class Interpreter extends Activity {
    private static final String TAG = "hunkypunk.Interpreter";
	private Glk glk;
	private File mDataDir;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	System.loadLibrary("andglk-loader");

    	setTheme(R.style.theme);
        Intent i = getIntent();
        Uri uri = i.getData();
        String terp = i.getStringExtra("terp");
        String ifid = i.getStringExtra("ifid");
        mDataDir = HunkyPunk.getGameDataDir(uri, ifid);
        File saveDir = new File(mDataDir, "savegames");
        saveDir.mkdir();

		glk = new Glk(this);

        setContentView(glk.getView());
        glk.setTerpPath(getFilesDir()+"/../lib/lib"+terp+".so");		
		glk.setAutoSave(getBookmark(), 0);
        glk.setSaveDir(saveDir);
        glk.setTranscriptDir(HunkyPunk.getTranscriptDir()); // there goes separation, and so cheaply...
		glk.setGameFilePath(uri.getPath());
		glk.setArguments(
			new String[]{
				getFilesDir()+"/../lib/lib"+terp+".so", 
				uri.getPath()
			}
		);

        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
        	restore(savedInstanceState.getParcelableArrayList("windowStates"));
        else if (i.getBooleanExtra("loadBookmark", false))
        	loadBookmark();
    	glk.start();
    }

    private void loadBookmark() {
    	final File f = getBookmark(); 
    	if (!f.exists())
    		return;

    	final File ws = getWindowStates();
    	if (ws.exists()) try {
    		final FileInputStream fis = new FileInputStream(ws);
    		if (fis != null) {
    			final ObjectInputStream ois = new ObjectInputStream(fis);
		    	Glk.getInstance().onSelect(new Runnable() {
		    		@Override
		    		public void run() {
		    	    	Glk.getInstance().waitForUi(new Runnable() {
		    	    		public void run() {
				    	    	Window w = null;
				    	    	try {
				    	    		while ((w = Window.iterate(w)) != null)
				    	    			w.readState(ois);
									ois.close();
					    	    	fis.close();
								} catch (IOException e) {
									Log.w(TAG, "error while reading window states", e);
								}
		    	    		}
		    	    	});
		    		}
		    	});
			}
    	} catch (IOException e) {
			Log.e(TAG, "error while opening window state stream", e);
    	}
	}

	private void restore(final ArrayList<Parcelable> windowStates) {
    	final File f = getBookmark(); 
    	if (!f.exists())
    		return;

    	Glk.getInstance().onSelect(new Runnable() {
    		@Override
    		public void run() {
    	    	if (windowStates != null)
	    	    	Glk.getInstance().waitForUi(new Runnable() {
	    	    		public void run() {
			    	    	Window w = null;
			    	    	for (Parcelable p : windowStates)
			    	    		if ((w = Window.iterate(w)) != null) {
			    	    			w.restoreInstanceState(p);
									w.flush();
								}
			    	    		else
			    	    			break;
	    	    		}
	    	    	});
    		}
    	});
    }
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	glk.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
		if (glk.isAlive()) {
			glk.postAutoSaveEvent(getBookmark().getAbsolutePath());

			final File ws = getWindowStates();
			try {
				final FileOutputStream fos = new FileOutputStream(ws);
				final ObjectOutputStream oos = new ObjectOutputStream(fos);
				Window w = null;
				while ((w = Window.iterate(w)) != null)
					w.writeState(oos);
				oos.close();
				fos.close();
			} catch (IOException e) {
				Log.e(TAG, "error while writing windowstates", e);
			}
		}
    }
    
    private File getWindowStates() {
		return new File(mDataDir, "windowStates");
	}

	private File getBookmark() {
		return new File(mDataDir, "bookmark");
	}

	@Override
    protected void onDestroy() {
		glk.postExitEvent();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);

    	if (!glk.isAlive())
    		return;

    	ArrayList<Parcelable> states = new ArrayList<Parcelable>();
    	
    	Window w = null;
    	while ((w = Window.iterate(w)) != null)
    		states.add(w.saveInstanceState());
    	
    	outState.putParcelableArrayList("windowStates", states);
    }
}