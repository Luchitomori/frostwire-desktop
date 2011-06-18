package com.frostwire.gui.bittorrent;

import java.io.File;
import java.util.Set;

import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.DisplayFormatters;

public class BTDownloadImpl implements BTDownload {

    private final DownloadManager _downloadManager;
    private final boolean _partialDownload;
    private final long _size;
    private final Set<DiskManagerFileInfo> _fileInfoSet;
    private String _hash;

    private boolean _deleteTorrentWhenRemove;

    private boolean _deleteDataWhenRemove;

    public BTDownloadImpl(DownloadManager downloadManager) {
        _downloadManager = downloadManager;
        _partialDownload = TorrentUtil.getSkippedFiles(downloadManager).size() > 0;

        if (_partialDownload) {
            _fileInfoSet = TorrentUtil.getNoSkippedFileInfoSet(downloadManager);
            long size = 0;
            for (DiskManagerFileInfo fileInfo : _fileInfoSet) {
                size += fileInfo.getLength();
            }
            _size = size;
        } else {
            _fileInfoSet = null;
            _size = downloadManager.getSize();
        }
        try {
            _hash = TorrentUtil.hashToString(downloadManager.getTorrent().getHash());
        } catch (TOTorrentException e) {
            e.printStackTrace();
            _hash = "";
        }

        _deleteTorrentWhenRemove = false;
        _deleteDataWhenRemove = false;
    }

    public long getSize() {
        return _size;
    }

    public String getDisplayName() {
        return _downloadManager.getDisplayName();
    }

    public boolean isResumable() {
        return TorrentUtil.isStartable(_downloadManager);
    }

    public boolean isPausable() {
        return TorrentUtil.isStopable(_downloadManager);
    }

    public boolean isCompleted() {
        return _downloadManager.getAssumedComplete();
    }

    public int getState() {
        return _downloadManager.getState();
    }

    public void remove() {
        TorrentUtil.removeDownload(_downloadManager, _deleteTorrentWhenRemove, _deleteDataWhenRemove);
    }

    public void pause() {
        if (isPausable()) {
            TorrentUtil.stop(_downloadManager);
        }
    }

    public void resume() {
        if (isResumable()) {
            TorrentUtil.start(_downloadManager);
        }
    }

    public File getSaveLocation() {
        return _downloadManager.getSaveLocation();
    }

    public int getProgress() {
        if (_partialDownload) {
            long downloaded = 0;
            for (DiskManagerFileInfo fileInfo : _fileInfoSet) {
                downloaded += fileInfo.getDownloaded();
            }
            return (int) ((downloaded * 100) / _size);
        } else {
            return _downloadManager.getStats().getDownloadCompleted(true) / 10;
        }
    }

    public String getStateString() {
        return DisplayFormatters.formatDownloadStatus(_downloadManager);
    }

    public long getBytesReceived() {
        return _downloadManager.getStats().getTotalGoodDataBytesReceived();
    }

    public long getBytesSent() {
        return _downloadManager.getStats().getTotalDataBytesSent();
    }

    public double getDownloadSpeed() {
        return _downloadManager.getStats().getDataReceiveRate() / 1000;
    }

    public double getUploadSpeed() {
        return _downloadManager.getStats().getDataSendRate() / 1000;
    }

    public long getETA() {
        return _downloadManager.getStats().getETA();
    }

    public DownloadManager getDownloadManager() {
        return _downloadManager;
    }

    public String getPeersString() {
        long lTotalPeers = -1;
        long lConnectedPeers = 0;
        if (_downloadManager != null) {
            lConnectedPeers = _downloadManager.getNbPeers();

            if (lTotalPeers == -1) {
                TRTrackerScraperResponse response = _downloadManager.getTrackerScrapeResponse();
                if (response != null && response.isValid()) {
                    lTotalPeers = response.getPeers();
                }
            }
        }

        long totalPeers = lTotalPeers;
        if (totalPeers <= 0) {
            DownloadManager dm = _downloadManager;
            if (dm != null) {
                totalPeers = dm.getActivationCount();
            }
        }

        //        long value = lConnectedPeers * 10000000;
        //        if (totalPeers > 0)
        //            value = value + totalPeers;

        int state = _downloadManager.getState();
        boolean started = state == DownloadManager.STATE_SEEDING || state == DownloadManager.STATE_DOWNLOADING;
        boolean hasScrape = lTotalPeers >= 0;

        String tmp;
        if (started) {
            tmp = hasScrape ? (lConnectedPeers > lTotalPeers ? "%1" : "%1 " + "/" + " %2") : "%1";
        } else {
            tmp = hasScrape ? "%2" : "";
        }

        tmp = tmp.replaceAll("%1", String.valueOf(lConnectedPeers));
        tmp = tmp.replaceAll("%2", String.valueOf(totalPeers));

        return tmp;
    }

    public String getSeedsString() {
        long lTotalSeeds = -1;
        long lTotalPeers = 0;
        long lConnectedSeeds = 0;
        DownloadManager dm = _downloadManager;
        if (dm != null) {
            lConnectedSeeds = dm.getNbSeeds();

            if (lTotalSeeds == -1) {
                TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
                if (response != null && response.isValid()) {
                    lTotalSeeds = response.getSeeds();
                    lTotalPeers = response.getPeers();
                }
            }
        }

        // Allows for 2097151 of each type (connected seeds, seeds, peers)
        long value = (lConnectedSeeds << 42);
        if (lTotalSeeds > 0)
            value += (lTotalSeeds << 21);
        if (lTotalPeers > 0)
            value += lTotalPeers;

        //boolean bCompleteTorrent = dm == null ? false : dm.getAssumedComplete();

        int state = dm.getState();
        boolean started = (state == DownloadManager.STATE_SEEDING || state == DownloadManager.STATE_DOWNLOADING);
        boolean hasScrape = lTotalSeeds >= 0;
        String tmp;

        if (started) {
            tmp = hasScrape ? (lConnectedSeeds > lTotalSeeds ? "%1" : "%1 " + "/" + " %2") : "%1";
        } else {
            tmp = hasScrape ? "%2" : "";
        }
        tmp = tmp.replaceAll("%1", String.valueOf(lConnectedSeeds));
        String param2 = "?";
        if (lTotalSeeds != -1) {
            param2 = String.valueOf(lTotalSeeds);
        }
        tmp = tmp.replaceAll("%2", param2);

        return tmp;
    }

    public boolean isDeleteTorrentWhenRemove() {
        return _deleteTorrentWhenRemove;
    }

    public void setDeleteTorrentWhenRemove(boolean deleteTorrentWhenRemove) {
        _deleteTorrentWhenRemove = deleteTorrentWhenRemove;
    }

    public boolean isDeleteDataWhenRemove() {
        return _deleteDataWhenRemove;
    }

    public void setDeleteDataWhenRemove(boolean deleteDataWhenRemove) {
        _deleteDataWhenRemove = deleteDataWhenRemove;
    }

    public String getHash() {
        return _hash;
    }

    public String getSeedToPeerRatio() {
        float ratio = -1;

        DownloadManager dm = _downloadManager;
        if (dm != null) {
            TRTrackerScraperResponse response = dm.getTrackerScrapeResponse();
            int seeds;
            int peers;

            if (response != null && response.isValid()) {
                seeds = Math.max(dm.getNbSeeds(), response.getSeeds());

                int trackerPeerCount = response.getPeers();
                peers = dm.getNbPeers();
                if (peers == 0 || trackerPeerCount > peers) {
                    if (trackerPeerCount <= 0) {
                        peers = dm.getActivationCount();
                    } else {
                        peers = trackerPeerCount;
                    }
                }
            } else {
                seeds = dm.getNbSeeds();
                peers = dm.getNbPeers();
            }

            if (peers < 0 || seeds < 0) {
                ratio = 0;
            } else {
                if (peers == 0) {
                    if (seeds == 0)
                        ratio = 0;
                    else
                        ratio = Float.POSITIVE_INFINITY;
                } else {
                    ratio = (float) seeds / peers;
                }
            }
        }

        if (ratio == -1) {
            return "";
        } else if (ratio == 0) {
            return "??";
        } else {
            return DisplayFormatters.formatDecimal(ratio, 3);
        }
    }

    public String getShareRatio() {
        DownloadManager dm = _downloadManager;

        int sr = (dm == null) ? 0 : dm.getStats().getShareRatio();

        if (sr == Integer.MAX_VALUE) {
            sr = Integer.MAX_VALUE - 1;
        }
        if (sr == -1) {
            sr = Integer.MAX_VALUE;
        }

        String shareRatio = "";

        if (sr == Integer.MAX_VALUE) {
            shareRatio = Constants.INFINITY_STRING;
        } else {
            shareRatio = DisplayFormatters.formatDecimal((double) sr / 1000, 3);
        }

        return shareRatio;
    }

    public boolean isPartialDownload() {
        return _partialDownload;
    }
}
