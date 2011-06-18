package com.frostwire.gui;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdesktop.jdic.desktop.Desktop;
import org.limewire.collection.SortedList;
import org.limewire.util.OSUtils;

import com.frostwire.CoreFrostWireUtils;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.settings.BittorrentSettings;

public final class GuiFrostWireUtils extends CoreFrostWireUtils {

	/**
	 * The idea is to find the first of the given options for Font Families.
	 * Inspired on CSS's font-family: Font1, Font2, Font3.
	 * 
	 * If Font1 is found, it'll be returned. If not it'll go to Font2 and so on.
	 * Only that this method has a fallback default font family and you can
	 * specify more than just 3 fonts.
	 * 
	 * Take that CSS.
	 * 
	 * @param defaultFamily
	 * @param prioritizedFamilies (case independent font family names)
	 * @return The name of the highest priority family name found in the system, if nothing is found it'll return the default family value.
	 */
	public final static String getFontFamily(String defaultFamily,
			String... prioritizedFamilies) {
		String[] availableFontFamilyNames = GraphicsEnvironment
				.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

		// reverse priority index.
		final Map<String, Integer> reversePriorityIndex = new HashMap<String, Integer>();
		for (int i = 0; i < prioritizedFamilies.length; i++) {
			reversePriorityIndex.put(prioritizedFamilies[i].toLowerCase(), i);
		}

		// If we find a family we'll insert it in order by priority on this
		// list.
		List<String> foundFamilies = new SortedList<String>(
				new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						/**
						 * We compare these guys by their priority. Lower has
						 * higher priority
						 */
						if (o1.equals(o2))
							return 0;

						// compare backwards since lower means higher priority.
						return new Integer(reversePriorityIndex.get(o1
								.toLowerCase())).compareTo(reversePriorityIndex
								.get(o2.toLowerCase()));
					}
				});

		// scan available family names and see if we got any of our candidates.
		for (String fontName : availableFontFamilyNames) {
			if (reversePriorityIndex.containsKey(fontName.toLowerCase())) {
				foundFamilies.add(fontName);
			}
		}

		if (foundFamilies.size() > 0) {
			return foundFamilies.get(0);
		}

		return defaultFamily;
	}
	
	/**
	 * Tries to open a file using java.awt.Desktop, jdic, or GUIMediator.launchFile.
	 * 
	 * @param file
	 */
	public static void launchFile(File file) {
		try {
			boolean isJava16orGreater = CoreFrostWireUtils
					.isJavaMinorVersionEqualOrGreaterThan("1.6");

			if (isJava16orGreater) {
				java.awt.Desktop.getDesktop().open(file);
			} else {
				//try jdic if you're not a mac and you're on below 1.6
				if (!OSUtils.isMacOSX()) {
					Desktop.open(file);
				} else {
					//if you're an old mac, try the old way
					GUIMediator.launchFile(file);
				}
			}
		} catch (Exception e) {
			GUIMediator.launchFile(file);
		}
	}
	
	/*
	 * @param delay - How long to wait before opening the torrent detail page.
	 * NOTE: Pass delay == -1 if you want to ignore the BittorrentSettings.TORRENT_DETAIL_PAGE_SHOWN_AFTER_DOWNLOAD
	 * and force the show of the torrent detail page.
	 */
	public static void showTorrentDetails(long delay, 
			String redirectUrl, 
			String query, 
			String torrentDetailsURL, 
			String torrentFileName) {
		
		if (delay!=-1 && !BittorrentSettings.TORRENT_DETAIL_PAGE_SHOWN_AFTER_DOWNLOAD.getValue())
			return;
		
		if (delay == -1) {
			delay = 0;
		}
		
		try {
			if (redirectUrl != null) {
				String queryParam="q="+ HTTPUtils.encode(query, "utf-8");
				String torrentDetailsURLparam = "u="+ HTTPUtils.encode(torrentDetailsURL,"utf-8");
				String torrentFileNameparam = "t=" + HTTPUtils.encode(torrentDetailsURL,"utf-8");
				GUIMediator.waitAndOpenURL(redirectUrl + "?"+queryParam+"&"+torrentDetailsURLparam+"&"+torrentFileNameparam,delay);
			} else
				GUIMediator.waitAndOpenURL(torrentDetailsURL, delay);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
