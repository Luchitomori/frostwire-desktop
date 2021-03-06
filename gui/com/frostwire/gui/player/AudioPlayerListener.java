package com.frostwire.gui.player;

import com.frostwire.mplayer.MediaPlaybackState;

/**
 * Listener for events generated by the AudioPlayer
 */
public interface AudioPlayerListener {

    /**
     * Open callback, stream is ready to play.
     */
    public void songOpened(AudioPlayer audioPlayer, AudioSource audioSource);

    public void progressChange(AudioPlayer audioPlayer, float currentTimeInSecs);
    
    public void volumeChange(AudioPlayer audioPlayer, double currentVolume);

    /**
     * Notification callback for basicplayer events such as opened, eom ...
     *
     * @param event
     */
    public void stateChange(AudioPlayer audioPlayer, MediaPlaybackState state);
    
    public void icyInfo(AudioPlayer audioPlayer, String data);
}
