/*
 * File    : ViewUtils.java
 * Created : 24-Oct-2003
 * By      : parg
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package com.frostwire.gui.bittorrent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.themes.SkinMenu;
import com.limegroup.gnutella.gui.themes.SkinMenuItem;

public class MenuUtil {
    public static void addSpeedMenu(SkinMenu menuAdvanced, boolean isTorrentContext, boolean hasSelection, boolean downSpeedDisabled,
            boolean downSpeedUnlimited, long totalDownSpeed, long downSpeedSetMax, long maxDownload, boolean upSpeedDisabled, boolean upSpeedUnlimited,
            long totalUpSpeed, long upSpeedSetMax, long maxUpload, final int num_entries, final SpeedAdapter adapter) {
        // advanced > Download Speed Menu //

        final SkinMenu menuDownSpeed = new SkinMenu(I18n.tr("Set Down Speed"));
        menuAdvanced.add(menuDownSpeed);

        final SkinMenuItem itemCurrentDownSpeed = new SkinMenuItem();
        itemCurrentDownSpeed.setEnabled(false);
        StringBuffer speedText = new StringBuffer();
        String separator = "";
        //itemDownSpeed.                   
        if (downSpeedDisabled) {
            speedText.append(I18n.tr("Disabled"));
            separator = " / ";
        }
        if (downSpeedUnlimited) {
            speedText.append(separator);
            speedText.append(I18n.tr("Unlimited"));
            separator = " / ";
        }
        if (totalDownSpeed > 0) {
            speedText.append(separator);
            speedText.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(totalDownSpeed));
        }
        itemCurrentDownSpeed.setText(speedText.toString());
        menuDownSpeed.add(itemCurrentDownSpeed);

        menuDownSpeed.addSeparator();

        final SkinMenuItem itemsDownSpeed[] = new SkinMenuItem[12];
        ActionListener itemsDownSpeedListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() != null && e.getSource() instanceof SkinMenuItem) {
                    SkinMenuItem item = (SkinMenuItem) e.getSource();
                    int speed = item.getClientProperty("maxdl") == null ? 0 : ((Integer) item.getClientProperty("maxdl")).intValue();
                    adapter.setDownSpeed(speed);
                }
            }
        };

        itemsDownSpeed[1] = new SkinMenuItem();
        itemsDownSpeed[1].setText(I18n.tr("No limit"));
        itemsDownSpeed[1].putClientProperty("maxdl", new Integer(0));
        itemsDownSpeed[1].addActionListener(itemsDownSpeedListener);
        menuDownSpeed.add(itemsDownSpeed[1]);

        if (hasSelection) {

            //using 200KiB/s as the default limit when no limit set.
            if (maxDownload == 0) {
                if (downSpeedSetMax == 0) {
                    maxDownload = 200 * 1024;
                } else {
                    maxDownload = 4 * (downSpeedSetMax / 1024) * 1024;
                }
            }

            for (int i = 2; i < 12; i++) {
                itemsDownSpeed[i] = new SkinMenuItem();
                itemsDownSpeed[i].addActionListener(itemsDownSpeedListener);

                // dms.length has to be > 0 when hasSelection
                int limit = (int) (maxDownload / (10 * num_entries) * (12 - i));
                StringBuffer speed = new StringBuffer();
                speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit * num_entries));
                //                if (num_entries > 1) {
                //                    speed.append(" ");
                //                    speed.append(MessageText
                //                            .getString("MyTorrentsView.menu.setSpeed.in"));
                //                    speed.append(" ");
                //                    speed.append(num_entries);
                //                    speed.append(" ");
                //                    speed.append(MessageText
                //                            .getString("MyTorrentsView.menu.setSpeed.slots"));
                //                    speed.append(" ");
                //                    speed
                //                            .append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
                //                }
                itemsDownSpeed[i].setText(speed.toString());
                itemsDownSpeed[i].putClientProperty("maxdl", new Integer(limit));
                menuDownSpeed.add(itemsDownSpeed[i]);
            }
        }

        // ---
//        menuDownSpeed.addSeparator();
//
//        final SkinMenuItem itemDownSpeedManualSingle = new SkinMenuItem();
//        itemDownSpeedManualSingle.setText(I18n.tr("Manual..."));
//        itemDownSpeedManualSingle.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                //int speed_value = getManualSpeedValue(shell, true);
//                //if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
//            }
//        });
//        menuDownSpeed.add(itemDownSpeedManualSingle);

        //        if (num_entries > 1) {
        //            final MenuItem itemDownSpeedManualShared = new MenuItem(menuDownSpeed, SWT.PUSH);
        //            Messages.setLanguageText(itemDownSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers");
        //            itemDownSpeedManualShared.addSelectionListener(new SelectionAdapter() {
        //                public void widgetSelected(SelectionEvent e) {
        //                    int speed_value = getManualSharedSpeedValue(shell, true, num_entries);
        //                    if (speed_value > 0) {adapter.setDownSpeed(speed_value);}
        //                }
        //            });
        //        }

        // advanced >Upload Speed Menu //

        final SkinMenu menuUpSpeed = new SkinMenu(I18n.tr("Set Up Speed"));
        menuAdvanced.add(menuUpSpeed);

        final SkinMenuItem itemCurrentUpSpeed = new SkinMenuItem();
        itemCurrentUpSpeed.setEnabled(false);
        separator = "";
        speedText = new StringBuffer();
        //itemUpSpeed.                   
        if (upSpeedDisabled) {
            speedText.append(I18n.tr("Disabled"));
            separator = " / ";
        }
        if (upSpeedUnlimited) {
            speedText.append(separator);
            speedText.append(I18n.tr("Unlimited"));
            separator = " / ";
        }
        if (totalUpSpeed > 0) {
            speedText.append(separator);
            speedText.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(totalUpSpeed));
        }
        itemCurrentUpSpeed.setText(speedText.toString());
        menuUpSpeed.add(itemCurrentUpSpeed);

        // ---
        menuUpSpeed.addSeparator();

        final SkinMenuItem itemsUpSpeed[] = new SkinMenuItem[12];
        ActionListener itemsUpSpeedListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() != null && e.getSource() instanceof SkinMenuItem) {
                    SkinMenuItem item = (SkinMenuItem) e.getSource();
                    int speed = item.getClientProperty("maxul") == null ? 0 : ((Integer) item.getClientProperty("maxul")).intValue();
                    adapter.setUpSpeed(speed);
                }
            }
        };

        itemsUpSpeed[1] = new SkinMenuItem();
        itemsUpSpeed[1].setText(I18n.tr("No limit"));
        itemsUpSpeed[1].putClientProperty("maxul", new Integer(0));
        itemsUpSpeed[1].addActionListener(itemsUpSpeedListener);
        menuUpSpeed.add(itemsUpSpeed[1]);

        if (hasSelection) {
            //using 75KiB/s as the default limit when no limit set.
            if (maxUpload == 0) {
                maxUpload = 75 * 1024;
            } else {
                if (upSpeedSetMax == 0) {
                    maxUpload = 200 * 1024;
                } else {
                    maxUpload = 4 * (upSpeedSetMax / 1024) * 1024;
                }
            }
            for (int i = 2; i < 12; i++) {
                itemsUpSpeed[i] = new SkinMenuItem();
                itemsUpSpeed[i].addActionListener(itemsUpSpeedListener);

                int limit = (int) (maxUpload / (10 * num_entries) * (12 - i));
                StringBuffer speed = new StringBuffer();
                speed.append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit * num_entries));
                //                if (num_entries > 1) {
                //                    speed.append(" ");
                //                    speed.append(MessageText
                //                            .getString("MyTorrentsView.menu.setSpeed.in"));
                //                    speed.append(" ");
                //                    speed.append(num_entries);
                //                    speed.append(" ");
                //                    speed.append(MessageText
                //                            .getString("MyTorrentsView.menu.setSpeed.slots"));
                //                    speed.append(" ");
                //                    speed
                //                            .append(DisplayFormatters.formatByteCountToKiBEtcPerSec(limit));
                //                }

                itemsUpSpeed[i].setText(speed.toString());
                itemsUpSpeed[i].putClientProperty("maxul", new Integer(limit));
                menuUpSpeed.add(itemsUpSpeed[i]);
            }
        }

//        menuUpSpeed.addSeparator();
//
//        final SkinMenuItem itemUpSpeedManualSingle = new SkinMenuItem();
//        itemUpSpeedManualSingle.setText(I18n.tr("Manual..."));
//        itemUpSpeedManualSingle.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                //int speed_value = getManualSpeedValue(shell, false);
//                //if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
//            }
//        });
//        menuUpSpeed.add(itemUpSpeedManualSingle);

        //        if (num_entries > 1) {
        //            final MenuItem itemUpSpeedManualShared = new MenuItem(menuUpSpeed, SWT.PUSH);
        //            Messages.setLanguageText(itemUpSpeedManualShared, isTorrentContext?"MyTorrentsView.menu.manual.shared_torrents":"MyTorrentsView.menu.manual.shared_peers" );
        //            itemUpSpeedManualShared.addSelectionListener(new SelectionAdapter() {
        //                public void widgetSelected(SelectionEvent e) {
        //                    int speed_value = getManualSharedSpeedValue(shell, false, num_entries);
        //                    if (speed_value > 0) {adapter.setUpSpeed(speed_value);}
        //                }
        //            });
        //        }

    }

    public static SkinMenu createAdvancedSubMenu() {

        BTDownload[] downloaders = BTDownloadMediator.instance().getSelectedDownloaders();

        if (downloaders.length != 1) {
            return null;
        }

        ArrayList<DownloadManager> list = new ArrayList<DownloadManager>(downloaders.length);
        for (BTDownload downloader : downloaders) {
            DownloadManager dm = downloader.getDownloadManager();
            if (dm != null) {
                list.add(dm);
            }
        }
        
        if (list.size() == 0) {
            return null;
        }

        final DownloadManager[] dms = list.toArray(new DownloadManager[0]);

        boolean upSpeedDisabled = false;
        long totalUpSpeed = 0;
        boolean upSpeedUnlimited = false;
        long upSpeedSetMax = 0;

        boolean downSpeedDisabled = false;
        long totalDownSpeed = 0;
        boolean downSpeedUnlimited = false;
        long downSpeedSetMax = 0;

        for (int i = 0; i < dms.length; i++) {
            DownloadManager dm = (DownloadManager) dms[i];

            try {
                int maxul = dm.getStats().getUploadRateLimitBytesPerSecond();
                if (maxul == 0) {
                    upSpeedUnlimited = true;
                } else {
                    if (maxul > upSpeedSetMax) {
                        upSpeedSetMax = maxul;
                    }
                }
                if (maxul == -1) {
                    maxul = 0;
                    upSpeedDisabled = true;
                }
                totalUpSpeed += maxul;

                int maxdl = dm.getStats().getDownloadRateLimitBytesPerSecond();
                if (maxdl == 0) {
                    downSpeedUnlimited = true;
                } else {
                    if (maxdl > downSpeedSetMax) {
                        downSpeedSetMax = maxdl;
                    }
                }
                if (maxdl == -1) {
                    maxdl = 0;
                    downSpeedDisabled = true;
                }
                totalDownSpeed += maxdl;

            } catch (Exception ex) {
                Debug.printStackTrace(ex);
            }
        }

        final SkinMenu menuAdvanced = new SkinMenu(I18n.tr("Advanced"));

        // advanced > Download Speed Menu //

        long maxDownload = COConfigurationManager.getIntParameter("Max Download Speed KBs", 0) * 1024;
        long maxUpload = COConfigurationManager.getIntParameter("Max Upload Speed KBs", 0) * 1024;

        addSpeedMenu(menuAdvanced, true, true, downSpeedDisabled, downSpeedUnlimited, totalDownSpeed, downSpeedSetMax, maxDownload, upSpeedDisabled,
                upSpeedUnlimited, totalUpSpeed, upSpeedSetMax, maxUpload, dms.length, new SpeedAdapter() {
                    public void setDownSpeed(final int speed) {
                        for (int i = 0; i < dms.length; i++) {
                            dms[i].getStats().setDownloadRateLimitBytesPerSecond(speed);
                        }
                        //                        DMTask task = new DMTask(dms) {
                        //                            public void run(DownloadManager dm) {
                        //                                dm.getStats().setDownloadRateLimitBytesPerSecond(speed);
                        //                            }
                        //                        };
                        //                        task.go();
                    }

                    public void setUpSpeed(final int speed) {
                        for (int i = 0; i < dms.length; i++) {
                            dms[i].getStats().setUploadRateLimitBytesPerSecond(speed);
                        }
                        //                        DMTask task = new DMTask(dms) {
                        //                            public void run(DownloadManager dm) {
                        //                                dm.getStats().setUploadRateLimitBytesPerSecond(speed);
                        //                            }
                        //                        };
                        //                        task.go();
                    }
                });

        return menuAdvanced;
    }

    public interface SpeedAdapter {
        public void setUpSpeed(int val);

        public void setDownSpeed(int val);
    }
}
