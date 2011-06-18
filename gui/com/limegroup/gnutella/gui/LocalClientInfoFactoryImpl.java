package com.limegroup.gnutella.gui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.gui.bugs.LocalClientInfo;

/** A factory for creating LocalClientInfo objects. */
@Singleton
public class LocalClientInfoFactoryImpl implements LocalClientInfoFactory {
    
    
    @Inject
    public LocalClientInfoFactoryImpl() {
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.gui.LocalClientInfoFactory#createLocalClientInfo(java.lang.Throwable, java.lang.String, java.lang.String, boolean)
     */
    public LocalClientInfo createLocalClientInfo(Throwable bug, String threadName, String detail, boolean fatal) {
        return new LocalClientInfo(bug, threadName, detail, fatal, null);
    }

}
