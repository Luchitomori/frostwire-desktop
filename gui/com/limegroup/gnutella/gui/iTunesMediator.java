package com.limegroup.gnutella.gui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gudy.azureus2.core3.util.FileUtil;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.settings.iTunesImportSettings;
import com.limegroup.gnutella.settings.iTunesSettings;

/**
 * Handles sending completed downloads into iTunes.
 */
public final class iTunesMediator {

    private static final Log LOG = LogFactory.getLog(iTunesMediator.class);
    
    private static final String JS_SCRIPT_NAME = "itunes_import.js";

    private static iTunesMediator INSTANCE;

    /**
     * The queue that will process the tunes to add.
     */
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("iTunesAdderThread");
    
    /**
     * Returns the sole instance of this class.
     */
    public static synchronized iTunesMediator instance() {
        if (INSTANCE == null) {
            INSTANCE = new iTunesMediator();
        }
        return INSTANCE;
    }

    /**
     * Initializes iTunesMediator with the script file.
     */
    private iTunesMediator() {
    }

    /**
     * If running on OSX, iTunes integration is enabled and the downloaded file
     * is a supported type, send it to iTunes.
     */
    private void addSongsITunes(File file) {
        
        // Make sure we convert any uppercase to lowercase or vice versa.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException ignored) {}
        
        // Verify that we're adding a real file.
        if (!file.exists()) {
            if (LOG.isDebugEnabled())
                LOG.debug("File: '" + file + "' does not exist");
            return;
        } 
        
        File [] files;
        if (file.isDirectory()) {
        	files = FileUtils.getFilesRecursive(file, 
        			iTunesSettings.ITUNES_SUPPORTED_FILE_TYPES.getValue());
        } else if (file.isFile() && isSupported(FileUtils.getFileExtension(file)))
        	files = new File[]{file};
        else
        	return;
        
        if (files.length == 0) {
        	return;
        }
        
        if (OSUtils.isMacOSX()) {
        	for (File toAdd : files) {
                if (LOG.isTraceEnabled())
                    LOG.trace("Will add '" + toAdd + "' to Playlist");
                
                QUEUE.execute(new ExecOSAScriptCommand(toAdd));
            }
        } else {
        	if (LOG.isTraceEnabled())
                LOG.trace("Will add '" + files.length + " files" + "' to Playlist");
        	QUEUE.execute(new ExecWSHScriptCommand(files));
        }
    }

    /**
     * Returns true if the extension of name is a supported file type.
     */
    private static boolean isSupported(String extension) {
    	if (extension == null)
    		return false;
        String[] types = iTunesSettings.ITUNES_SUPPORTED_FILE_TYPES.getValue();
        for (int i = 0; i < types.length; i++)
            if (extension.equalsIgnoreCase(types[i]))
                return true;
        return false;
    }
    
    /**
     * Constructs and returns a osascript command.
     */
    private static String[] createOSAScriptCommand(File file) {
        String path = file.getAbsolutePath();
        String playlist = iTunesSettings.ITUNES_PLAYLIST.getValue();

        String[] command = new String[] { 
            "osascript", 
            "-e", "tell application \"Finder\"", 
            "-e",   "set hfsFile to (POSIX file \"" + path + "\")", 
            "-e",   "set thePlaylist to \"" + playlist + "\"", 
            "-e",   "tell application \"iTunes\"",
            //"-e",       "activate", // launch and bring to front
            "-e",       "launch", // launch in background
            "-e",       "if not (exists playlist thePlaylist) then", 
            "-e",           "set thisPlaylist to make new playlist", 
            "-e",           "set name of thisPlaylist to thePlaylist", 
            "-e",       "end if",

            "-e",       "add hfsFile to playlist thePlaylist",
            "-e",   "end tell",
            "-e", "end tell" 
        };

        return command;
    }
    
    private static String[] createWSHScriptCommand(File[] files) {
    	
    	createJScriptIfNeeded();
    	
        String playlist = iTunesSettings.ITUNES_PLAYLIST.getValue();
        
        ArrayList<String> command = new ArrayList<String>();
        command.add("wscript");
        command.add("//B");
        command.add("//NoLogo");
        command.add(new File(CommonUtils.getUserSettingsDir(), JS_SCRIPT_NAME).getAbsolutePath());
        command.add(playlist);
        for (File file : files) {
        	command.add(file.getAbsolutePath());
        }
        
        return command.toArray(new String[0]);
    }
    
    /**
     * Executes the osascript CLI command
     */
    private class ExecOSAScriptCommand implements Runnable {
        /**
         * The file to add.
         */
        private final File file;

        /**
         * Constructs a new ExecOSAScriptCommand for the specified file.
         */
        public ExecOSAScriptCommand(File file) {
            this.file = file;
        }

        /**
         * Runs the osascript command
         */
        public void run() {
            try {
                Runtime.getRuntime().exec(createOSAScriptCommand(file));
            } catch (IOException err) {
                LOG.debug(err);
            }
        }
    }
    
    private class ExecWSHScriptCommand implements Runnable {
        /**
         * The file to add.
         */
        private final File[] files;

        /**
         * Constructs a new ExecOSAScriptCommand for the specified file.
         */
        public ExecWSHScriptCommand(File[] files) {
            this.files = files;
        }

        /**
         * Runs the osascript command
         */
        public void run() {
            try {
                Runtime.getRuntime().exec(createWSHScriptCommand(files));
            } catch (IOException err) {
                LOG.debug(err);
            }
        }
    }

	public void scanForSongs(File file) {
	    iTunesImportSettings.IMPORT_FILES.add(file);
	    if (OSUtils.isMacOSX() || OSUtils.isWindows()) {
			addSongsITunes(file);
		} else if (OSUtils.isUbuntu()) {
		    //System.out.println("Import in Banshee: " + file);
		}
	}

    public boolean isScanned(File file) {
        return iTunesImportSettings.IMPORT_FILES.contains(file);
    }
    
    public void removeFromScanned(File file) {
        iTunesImportSettings.IMPORT_FILES.remove(file);
    }
    
    private static void createJScriptIfNeeded() {
    	File fileJS = new File(CommonUtils.getUserSettingsDir(), JS_SCRIPT_NAME);
    	if (fileJS.exists()) {
    		return;
    	}
    	
        URL url = ResourceManager.getURLResource(JS_SCRIPT_NAME);
        InputStream is = null;
        try {
            if(url != null) {
                is = new BufferedInputStream(url.openStream());
                FileUtil.copyFile(is, fileJS, false);
            }
        } catch(IOException ignored) {
        } finally {
            IOUtils.close(is);
        }
    }
}
