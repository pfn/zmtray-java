package com.zimbra.app.systray;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.zimbra.app.systray.options.OptionsDialog;

public class OSXSupport {

    public static void addApplicationAdapter(ZimbraTray zt, Application app) {
        app.addApplicationListener(getApplicationAdapter(zt));
    }
    
    private static ApplicationAdapter getApplicationAdapter(
            final ZimbraTray zt) {
        return new ApplicationAdapter() {
            @Override
            public void handlePreferences(ApplicationEvent e) {
                OptionsDialog.showForm(zt);
            }

            @Override
            public void handleQuit(ApplicationEvent e) {
                System.exit(0);
            }

            @Override
            public void handleReOpenApplication(ApplicationEvent e) {
                zt.showNewMessages(true);
                zt.pollNow();
            }

        };
    }
}
