package com.limegroup.gnutella.gui.search;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.util.TorrentUtils;
import org.limewire.util.StringUtils;

import com.frostwire.JsonEngine;
import com.frostwire.alexandria.LibraryUtils;
import com.frostwire.bittorrent.websearch.WebSearchResult;
import com.frostwire.gui.bittorrent.TorrentUtil;
import com.frostwire.gui.filters.SearchFilter;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.search.db.SmartSearchDB;
import com.limegroup.gnutella.gui.search.db.TorrentDBPojo;
import com.limegroup.gnutella.gui.search.db.TorrentFileDBPojo;
import com.limegroup.gnutella.settings.SearchSettings;

public class LocalSearchEngine {

    private static final Log LOG = LogFactory.getLog(LocalSearchEngine.class);

    private final int DEEP_SEARCH_DELAY;
    private final int MAXIMUM_TORRENTS_TO_SCAN;
    private final int DEEP_SEARCH_ROUNDS;
    private final int LOCAL_SEARCH_RESULTS_LIMIT;

    private static LocalSearchEngine INSTANCE;

    private static final Comparator<SearchResultDataLine> TORRENT_SEED_TABLELINE_COMPARATOR = new Comparator<SearchResultDataLine>() {

        @Override
        public int compare(SearchResultDataLine o1, SearchResultDataLine o2) {
            return o2.getSeeds() - o1.getSeeds();
        }
    };

    /**
     * We'll keep here every info hash we've already processed during the
     * session
     */
    private HashSet<String> KNOWN_INFO_HASHES = new HashSet<String>();
    private SmartSearchDB DB;
    private JsonEngine JSON_ENGINE;

    public LocalSearchEngine() {
        DEEP_SEARCH_DELAY = SearchSettings.SMART_SEARCH_START_DELAY.getValue();
        MAXIMUM_TORRENTS_TO_SCAN = SearchSettings.SMART_SEARCH_MAXIMUM_TORRENTS_TO_SCAN.getValue();
        DEEP_SEARCH_ROUNDS = SearchSettings.SMART_SEARCH_DEEP_SEARCH_ROUNDS.getValue();
        LOCAL_SEARCH_RESULTS_LIMIT = SearchSettings.SMART_SEARCH_FULLTEXT_SEARCH_RESULTS_LIMIT.getValue();

        DB = new SmartSearchDB(SearchSettings.SMART_SEARCH_DATABASE_FOLDER.getValue());
        JSON_ENGINE = new JsonEngine();
    }

    public static LocalSearchEngine instance() {
        if (INSTANCE == null) {
            INSTANCE = new LocalSearchEngine();
        }

        return INSTANCE;
    }

    public final static HashSet<String> IGNORABLE_KEYWORDS;

    static {
        IGNORABLE_KEYWORDS = new HashSet<String>();
        IGNORABLE_KEYWORDS.addAll(Arrays.asList("me", "you", "he", "she", "they", "them", "we", "us", "my", "your", "yours", "his", "hers", "theirs", "ours", "the", "of", "in", "on", "out", "to", "at", "as", "and", "by", "not", "is", "are", "am", "was", "were", "will", "be", "for", "el", "la",
                "es", "de", "los", "las", "en"));
    }

    /**
     * Avoid possible SQL errors due to escaping. Cleans all double spaces and
     * trims.
     * 
     * @param str
     * @return
     */
    private final static String stringSanitize(String str) {
        str = stripHtml(str);
        str = str.replaceAll("\\.torrent|www\\.|\\.com|[\\\\\\/%_;\\-\\.\\(\\)\\[\\]\\n\\r"+'\uu2013'+"]", " ");
        return StringUtils.removeDoubleSpaces(str);
    }
    
    /**
     * Very simple html strip routine. Not for a wide use.
     * 
     * @param str
     * @return
     */
    private static String stripHtml(String str) {
        str = str.replaceAll("\\<.*?>","");
        str = str.replaceAll("\\&.*?\\;", "");
        return str;
    }

    /**
     * Perform a simple Database Search, immediate results should be available
     * if there are matches.
     */
    public List<SmartSearchResult> search(String query) {
        query = LibraryUtils.prepareFuzzyLuceneQuery(query);

        //FULL TEXT SEARCH, Returns the File IDs we care about.
        String fullTextIndexSql = "SELECT * FROM FTL_SEARCH(?, ?, 0)";

        //System.out.println(fullTextIndexSql);
        List<List<Object>> matchedFileRows = DB.query(fullTextIndexSql, query, LOCAL_SEARCH_RESULTS_LIMIT);

        int fileIDStrOffset = " PUBLIC   FILES  WHERE  FILEID =".length();

        StringBuilder fileIDSet = new StringBuilder("(");

        int numFilesFound = matchedFileRows.size();
        int i = 0;

        for (List<Object> row : matchedFileRows) {
            String rowStr = (String) row.get(0);
            fileIDSet.append(rowStr.substring(fileIDStrOffset));

            if (i++ < (numFilesFound - 1)) {
                fileIDSet.append(",");
            }
        }
        fileIDSet.append(")");

        String sql = "SELECT Torrents.json, Files.json, torrentName, fileName FROM Torrents JOIN Files ON Torrents.torrentId = Files.torrentId WHERE Files.fileId IN " + fileIDSet.toString() + " ORDER BY seeds DESC LIMIT " + LOCAL_SEARCH_RESULTS_LIMIT;
        //System.out.println(sql);
        long start = System.currentTimeMillis();
        List<List<Object>> rows = DB.query(sql);
        long delta = System.currentTimeMillis() - start;
        System.out.print("Found " + rows.size() + " local results in " + delta + "ms. ");

        //no query should ever take this long.
        if (delta > 3000) {
            System.out.println("\nWarning: Results took too long, there's something wrong with the database, you might want to delete your 'search_db' folder inside the FrostWire preferences folder.");
        }

        List<SmartSearchResult> results = new ArrayList<SmartSearchResult>();
        Map<Integer, SearchEngine> searchEngines = SearchEngine.getSearchEngineMap();

        // GUBENE
        String torrentJSON = null;
        for (List<Object> row : rows) {
            try {
                torrentJSON = (String) row.get(0);
                String fileJSON = (String) row.get(1);

                TorrentDBPojo torrentPojo = JSON_ENGINE.toObject(torrentJSON, TorrentDBPojo.class);

                if (!searchEngines.get(torrentPojo.searchEngineID).isEnabled()) {
                    continue;
                }

                TorrentFileDBPojo torrentFilePojo = JSON_ENGINE.toObject(fileJSON, TorrentFileDBPojo.class);

                results.add(new SmartSearchResult(torrentPojo, torrentFilePojo));
                KNOWN_INFO_HASHES.add(torrentPojo.hash);
            } catch (Exception e) {
                // keep going dude
                System.out.println("Issues with POJO deserialization -> " + torrentJSON);
                e.printStackTrace();
                System.out.println("=====================");
            }
        }

        System.out.println("Ended up with " + results.size() + " results");

        return results;
    }

    public List<DeepSearchResult> deepSearch(byte[] guid, String query, SearchInformation info) {
        SearchResultMediator rp = null;

        // Let's wait for enough search results from different search engines.
        sleep();

        // Wait for enough results or die if the ResultPanel has been closed.
        int tries = DEEP_SEARCH_ROUNDS;
        Set<SearchEngine> engines = new HashSet<SearchEngine>(SearchEngine.getSearchEngines());

        for (int i = tries; i > 0; i--) {
            if ((rp = SearchMediator.getResultPanelForGUID(new GUID(guid))) == null) {
                return null;
            }

            if (rp.isStopped()) {
                return null;
            }

            scanAvailableResults(guid, query, info, rp, engines);

            sleep();
        }

        // did they close rp? nothing left to do.
        if (rp == null) {
            return null;
        }

        return null;
    }

    public void sleep() {
        try {
            Thread.sleep(DEEP_SEARCH_DELAY);
        } catch (InterruptedException e1) {
        }
    }

    public void scanAvailableResults(byte[] guid, String query, SearchInformation info, SearchResultMediator rp, Set<SearchEngine> searchEnginesThatGotTorrentViaHttp) {

        int foundTorrents = 0;

        List<SearchResultDataLine> allData = rp.getAllData();
        sortAndStripNonTorrents(allData);

        for (int i = 0; i < allData.size() && foundTorrents < MAXIMUM_TORRENTS_TO_SCAN; i++) {
            SearchResultDataLine line = allData.get(i);

            if (line.getInitializeObject() instanceof SearchEngineSearchResult) {
                foundTorrents++;

                boolean viaHttp = false;

                // download at least one (hopefully with a good seed number) via http
                SearchEngine engine = line.getSearchResult().getSearchEngine();
                if (searchEnginesThatGotTorrentViaHttp.contains(engine)) {
                    searchEnginesThatGotTorrentViaHttp.remove(engine);
                    viaHttp = true;
                }

                WebSearchResult webSearchResult = line.getSearchResult().getWebSearchResult();

                if (webSearchResult.getHash() == null && !viaHttp) {
                    // sorry, no possible to handle this case
                    continue;
                }

                if (!KNOWN_INFO_HASHES.contains(webSearchResult.getHash())) {
                    KNOWN_INFO_HASHES.add(webSearchResult.getHash());
                    SearchEngine searchEngine = line.getSearchEngine();
                    scanDotTorrent(webSearchResult, viaHttp, guid, query, searchEngine, info);
                }
            }
        }
    }

    /**
     * Remove all results that are not torrents and sort them by seed (desc. order)
     * @param allData
     */
    private void sortAndStripNonTorrents(List<SearchResultDataLine> allData) {
        Collections.sort(allData, TORRENT_SEED_TABLELINE_COMPARATOR);
        Iterator<SearchResultDataLine> iterator = allData.iterator();
        while (iterator.hasNext()) {
            SearchResultDataLine next = iterator.next();

            if (!next.getExtension().toLowerCase().contains("torrent")) {
                iterator.remove();
            }
        }
    }

    /**
     * Will decide wether or not to fetch the .torrent from the DHT.
     * 
     * If it has to download it, it will use a
     * LocalSearchTorrentDownloaderListener to start scanning, if the torrent
     * has already been fetched, it will perform an immediate search.
     * 
     * @param webSearchResult
     * @param searchEngine
     * @param info
     */
    private void scanDotTorrent(WebSearchResult webSearchResult, boolean viaHttp, byte[] guid, String query, SearchEngine searchEngine, SearchInformation info) {
        if (!torrentHasBeenIndexed(webSearchResult.getHash())) {
            // download the torrent
            String saveDir = SearchSettings.SMART_SEARCH_DATABASE_FOLDER.getValue().getAbsolutePath();

            SearchResultMediator rp = SearchMediator.getResultPanelForGUID(new GUID(guid));
            if (rp != null) {
                rp.incrementSearchCount();
            }

            String url = viaHttp ? webSearchResult.getTorrentURI() : TorrentUtil.getMagnet(webSearchResult.getHash());
            //System.out.println("Download - " + url);

            TorrentDownloaderFactory.create(new LocalSearchTorrentDownloaderListener(guid, query, webSearchResult, searchEngine, info), url, null, saveDir).start();
        }
    }

    private boolean torrentHasBeenIndexed(String infoHash) {
        List<List<Object>> rows = DB.query("SELECT * FROM Torrents WHERE infoHash LIKE ?", infoHash);
        return rows.size() > 0;
    }

    public void indexTorrent(WebSearchResult searchResult, TOTorrent theTorrent, SearchEngine searchEngine) {
        TorrentDBPojo torrentPojo = new TorrentDBPojo();
        torrentPojo.creationTime = searchResult.getCreationTime();
        torrentPojo.fileName = searchResult.getFileName();
        torrentPojo.hash = searchResult.getHash();
        torrentPojo.searchEngineID = searchEngine.getId();
        torrentPojo.seeds = searchResult.getSeeds();
        torrentPojo.size = searchResult.getSize();
        torrentPojo.torrentDetailsURL = searchResult.getTorrentDetailsURL();
        torrentPojo.torrentURI = searchResult.getTorrentURI();
        torrentPojo.vendor = searchResult.getVendor();

        String torrentJSON = JSON_ENGINE.toJson(torrentPojo);

        int torrentID = DB.insert("INSERT INTO Torrents (infoHash, timestamp, torrentName, seeds, json) VALUES (?, ?, LEFT(?, 10000), ?, ?)", torrentPojo.hash, System.currentTimeMillis(), torrentPojo.fileName.toLowerCase(), torrentPojo.seeds, torrentJSON);

        TOTorrentFile[] files = theTorrent.getFiles();

        for (TOTorrentFile f : files) {
            TorrentFileDBPojo tfPojo = new TorrentFileDBPojo();
            tfPojo.relativePath = f.getRelativePath();
            tfPojo.size = f.getLength();

            String fileJSON = JSON_ENGINE.toJson(tfPojo);
            String keywords = stringSanitize(torrentPojo.fileName + " " + tfPojo.relativePath).toLowerCase();

            DB.insert("INSERT INTO Files (torrentId, fileName, json, keywords) VALUES (?, LEFT(?, 10000), ?, ?)", torrentID, tfPojo.relativePath, fileJSON, keywords);
        }
    }

    private class LocalSearchTorrentDownloaderListener implements TorrentDownloaderCallBackInterface {

        private AtomicBoolean finished = new AtomicBoolean(false);
        private byte[] guid;
        private final Set<String> tokens;
        private SearchEngine searchEngine;
        private WebSearchResult webSearchResult;
        private SearchInformation info;
        public LocalSearchTorrentDownloaderListener(byte[] guid, String query, WebSearchResult webSearchResult, SearchEngine searchEngine, SearchInformation info) {
            this.guid = guid;
            this.tokens = new HashSet<String>(Arrays.asList(query.toLowerCase().split(" ")));
            this.searchEngine = searchEngine;
            this.webSearchResult = webSearchResult;
            this.info = info;
        }

        @Override
        public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {

            // index the torrent (insert it's structure in the local DB)
            if (state == TorrentDownloader.STATE_FINISHED && finished.compareAndSet(false, true)) {
                try {
                    File torrentFile = inf.getFile();
                    TOTorrent theTorrent = TorrentUtils.readFromFile(torrentFile, false);

                    // search right away on this torrent.
                    matchResults(theTorrent);

                    indexTorrent(webSearchResult, theTorrent, searchEngine);

                    torrentFile.delete();
                } catch (Throwable e) {
                    LOG.error("Error during torrent indexing", e);
                }
            }

            switch (state) {
            case TorrentDownloader.STATE_FINISHED:
            case TorrentDownloader.STATE_ERROR:
            case TorrentDownloader.STATE_DUPLICATE:
            case TorrentDownloader.STATE_CANCELLED:
                SearchResultMediator rp = SearchMediator.getResultPanelForGUID(new GUID(guid));
                if (rp != null) {
                    rp.decrementSearchCount();
                }
                break;
            }
        }

        private void matchResults(TOTorrent theTorrent) {

            if (!searchEngine.isEnabled()) {
                return;
            }

            final SearchResultMediator rp = SearchMediator.getResultPanelForGUID(new GUID(guid));

            // user closed the tab.
            if (rp == null || rp.isStopped()) {
                return;
            }

            SearchFilter filter = SearchMediator.getSearchFilterFactory().createFilter();

            TOTorrentFile[] fs = theTorrent.getFiles();
            for (int i = 0; i < fs.length; i++) {
                try {
                final DeepSearchResult result = new DeepSearchResult(fs[i], webSearchResult, searchEngine, info);

                if (!filter.allow(result))
                    continue;

                boolean foundMatch = true;
                
                String keywords = stringSanitize(result.getFileName() + " " + fs[i].getRelativePath()).toLowerCase();

                for (String token : tokens) {
                    if (!keywords.contains(token)) {
                        foundMatch = false;
                        break;
                    }
                }

                if (foundMatch) {
                    GUIMediator.safeInvokeAndWait(new Runnable() {
                        public void run() {
                            SearchMediator.getSearchResultDisplayer().addQueryResult(guid, result, rp);
                        }
                    });
                }
                } catch (Throwable e) {
                    LOG.error("Error analysing torrent file", e);
                }
            }
        }
    }

    public void shutdown() {
        DB.close();
    }

    public long getTotalTorrents() {
        List<List<Object>> query = DB.query("SELECT COUNT(*) FROM Torrents");
        return query.size() > 0 ? (Long) query.get(0).get(0) : 0;
    }

    public long getTotalFiles() {
        List<List<Object>> query = DB.query("SELECT COUNT(*) FROM Files");
        return query.size() > 0 ? (Long) query.get(0).get(0) : 0;
    }

    public void resetDB() {
        DB.reset();
    }
}
