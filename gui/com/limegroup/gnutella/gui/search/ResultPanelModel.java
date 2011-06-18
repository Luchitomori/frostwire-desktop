package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.gui.tables.BasicDataLineModel;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.settings.SearchSettings;

/** 
 * Model for search results.
 *
 * Ensures that if new lines are added and they are similiar to old lines,
 * that the new lines are added as extra information to the existing lines,
 * instead of as brand new lines.
 */
class ResultPanelModel extends BasicDataLineModel<TableLine, SearchResult> {
    
    /**
     * 
     */
    private static final long serialVersionUID = -2382156313320196261L;

    /**
     * The model storing metadata information.
     */
    protected final MetadataModel METADATA;
    
    /**
     * The columns.
     */
    protected final SearchTableColumns COLUMNS = new SearchTableColumns();
    
    /**
     * Whether or not metadata is being tallied.
     */
    protected boolean _useMetadata = true;
    
    /**
     * HashMap for quick access to indexes based on SHA1 info.
     */
    private final Map<String, Integer> _indexes = new HashMap<String, Integer>();
    
    /**
     * The TableLineGrouper to use for slow matching.
     *
     * Allocated when needed.
     */
    private TableLineGrouper _grouper;

    /**
     * The number of sources for this search.
     */
    private int _numSources;
    
    private int _numResults;
    
    /**
     * Constructs a new ResultPanelModel with a new MetadataModel.
     */
    ResultPanelModel() {
        this(new MetadataModel());
    }
    
    /**
     * Constructs a new ResultPanelModel with the given MetadataModel.
     */
    ResultPanelModel(MetadataModel mm) {
        super(TableLine.class);
        METADATA = mm;
    }
    
    /**
     * Whether or not the line should add to the metadata.
     */
    void setUseMetadata(boolean use) {
        _useMetadata = use;
    }
    
    /**
     * Gets the columns used by this model.
     */
    SearchTableColumns getColumns() {
        return COLUMNS;
    }
    
    /**
     * Creates a new TableLine.
     */
    public TableLine createDataLine() {
        return new TableLine(COLUMNS);
    }
    
    /**
     * Gets the column at the specified index.
     */
    public LimeTableColumn getTableColumn(int idx) {
        return COLUMNS.getColumn(idx);
    }
    
    /**
     * Overrides default compare to move spam results to the bottom,
     * or to change the 'count' compare to use different values for
     * multicast or secure results.
     */
    public int compare(TableLine ta, TableLine tb) {
        int spamRet = compareSpam(ta, tb);
        if (spamRet != 0)
            return spamRet;
        
        if (!isSorted() || _activeColumn != SearchTableColumns.COUNT_IDX)
            return super.compare(ta, tb);
        else
            return compareCount(ta, tb, false);
    }
    
    /**
     * Returns the metadata model storing information about each result
     * for easy filtering.
     */
    MetadataModel getMetadataModel() {
        return METADATA;
    }
    
    /** 
     * Overrides the default remove to remove the index from the hashmap.
     *
     * @param row  the index of the row to remove.
     */
    public void remove(int row) {
        TableLine tl = get(row);
        String sha1 = getHash(row);
        if(sha1 != null)
            _indexes.remove(sha1);
        super.remove(row);
        _numSources -= tl.getSeeds();
        _numResults -= 1;
        remapIndexes(row);
    }
    
    /**
     * Override default so new ones get added to the end
     */
    public int add(SearchResult o) {
        return add(o, getRowCount());
    }
    
    /**
     * Override to fix compile error on OSX.
     */
    public int add(TableLine dl) {
        return super.add(dl);
	}
	
	/**
	 * Override to not iterate through each result.
	 */
	public Object refresh() {
        fireTableRowsUpdated(0, getRowCount());
        return null;
    }
    
    /**
     * Does a slow refresh, forcing the underlying results to have
     * 'update' called on them.
     */
    public void slowRefresh() {
        super.refresh();
    }
    
    /**
     * Adds sr to line as a new source.
     */
    protected int addNewResult(TableLine line, SearchResult sr) {
        int oldCount = line.getSeeds();
        line.addNewResult(sr, METADATA);
        int newCount = line.getSeeds();
        int added = newCount - oldCount;
        _numSources += added;
        _numResults += 1;
        return added;
    }

    /**
     * Maintains the indexes HashMap & MetadataModel.
     */    
    public int add(TableLine tl, int row) {
        _numSources += tl.getSeeds();
        _numResults += 1;
        String sha1 = tl.getHash();
        if(sha1 != null)
            _indexes.put(sha1, new Integer(row));
        int addedAt = super.add(tl, row);
        remapIndexes(addedAt + 1);
        if(_useMetadata)
            METADATA.addNew(tl); // MUST be after add, else callbacks whack out
        return addedAt;
    }
    
    /**
     * Gets the row this DataLine is at.
     */
    public int getRow(TableLine tl) {
        String sha1 = tl.getHash();
        if(sha1 != null)
            return fastMatch(sha1);
        else
            return super.getRow(tl);
    }
    
    /**
     * Returns the number of sources found for this search.
     */
    int getTotalSources() {
        return _numSources;
    }
    
    /** 
     * Overrides the default sort to maintain the indexes HashMap,
     * according to the current sort column and order.
     */
    protected void doResort() {
        super.doResort();
        _indexes.clear(); // it's easier & quicker to just clear & re-input
        remapIndexes(0);
    }
    
    /**
     * Overrides the default clear to erase the indexes HashMap,
     * Metadata and Grouper.
     */
    public void clear() {
        if(METADATA != null)
            METADATA.clear();
        if(_grouper != null)
            _grouper.clear();
        simpleClear();
    }
    
    /**
     * Does nothing -- lines need no cleanup.
     */
    protected void cleanup() {}
    
    /**
     * Simple clear -- clears the number of sources & cached SHA1 indexes.
     * Calls super.clear to erase the stored lines.
     */
    protected void simpleClear() {
        _numSources = 0;
        _numResults = 0;
        _indexes.clear();
        super.clear();
    }
    
    /**
     * Remaps the indexes, starting at 'start' and going to the end of
     * the list.  This is needed for when rows are added to the middle of
     * the list to maintain the correct rows per objects.
     */
    private void remapIndexes(int start) {
        remapIndexes(start, getRowCount());
    }        
    
    /**
     * Remaps the indexes, starting at 'start' and going to 'end'.
     * This is useful for when we move a row from end to start or vice versa.
     */
    private void remapIndexes(int start, int end) {
        for (int i = start; i < end; i++) {
            String sha1 = getHash(i);
            if(sha1 != null)
                _indexes.put(sha1, new Integer(i));
        }
    }
    
    /**
     * Gets the SHA1 URN for a row.
     */
    private String getHash(int idx) {
        if(idx >= getRowCount())
            return null;
        return get(idx).getHash();
    }
    
    /** Compares the spam difference between the two rows. */
    private int compareSpam(TableLine a, TableLine b) {
        if (SearchSettings.moveJunkToBottom()) {
            if (SpamFilter.isAboveSpamThreshold(a)) {
                if (!SpamFilter.isAboveSpamThreshold(b)) {
                    return 1;
                }
            } else if (SpamFilter.isAboveSpamThreshold(b)) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * Compares the count between two rows.
     */
    private int compareCount(TableLine a, TableLine b, boolean spamCompare) {
        if(spamCompare) {
            int spamRet = compareSpam(a, b);
            if(spamRet != 0)
                return spamRet;
        }
        
        int c1 = normalizeLocationCount(a.getSeeds(), a.getQuality());
        int c2 = normalizeLocationCount(b.getSeeds(), b.getQuality());
        return (c1 - c2) * _ascending;
    }
    
    /** Normalizes the location count, depending on the quality. */
    private int normalizeLocationCount(int count, int quality) {
        switch(quality) {
        case QualityRenderer.SECURE_QUALITY:
            return Integer.MAX_VALUE-1;
        case QualityRenderer.MULTICAST_QUALITY:
            return Integer.MAX_VALUE-2;
        default:
            return count;
        }
    }
    
    /**
     * Fast match -- lookup in the table.
     */
    private int fastMatch(String sha1) {
        Integer idx = _indexes.get(sha1);
        if(idx == null)
            return -1;
        else
            return idx.intValue();
    }

    public int getTotalResults() {
        return _numResults;
    }        
}

