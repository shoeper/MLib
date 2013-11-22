/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package msearch.tool;

import java.util.EventListener;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

public class MSearchListenerMediathekView implements EventListener {

    public static final int EREIGNIS_BLACKLIST_GEAENDERT = 1;
    public static final int EREIGNIS_LISTE_HISTORY_GEAENDERT = 2;
    public static final int EREIGNIS_LISTE_PSET = 3;
    public static final int EREIGNIS_FILMLISTE_GEAENDERT = 5;
    public static final int EREIGNIS_ANZAHL_DOWNLOADS = 6;
    public static final int EREIGNIS_LISTE_URL_FILMLISTEN = 7;
    public static final int EREIGNIS_LISTE_FILMLISTEN_SERVER = 8;
    public static final int EREIGNIS_LISTE_DOWNLOADS = 9;
    public static final int EREIGNIS_LISTE_ABOS = 10;
    public static final int EREIGNIS_LISTE_ERLEDIGTE_ABOS = 11;
    public static final int EREIGNIS_ART_IMPORT_FILMLISTE = 12;
    public static final int EREIGNIS_ART_DOWNLOAD_PROZENT = 13;
    public static final int EREIGNIS_START_EVENT = 14;
    public static final int EREIGNIS_LOG_FEHLER = 15;
    public static final int EREIGNIS_LOG_SYSTEM = 16;
    public static final int EREIGNIS_LOG_PLAYER = 17;
    public static final int EREIGNIS_PROGRAMM_OEFFNEN = 18;
    public static final int EREIGNIS_MEDIATHEKGUI_ORG_TITEL = 19;
    public static final int EREIGNIS_MEDIATHEKGUI_PROGRAMM_AKTUELL = 20;
    public static final int EREIGNIS_MEDIATHEKGUI_UPDATE_VERFUEGBAR = 21;
    public static final int EREIGNIS_PANEL_FILTER_ANZEIGEN = 22;
    public static final int EREIGNIS_PANEL_BESCHREIBUNG_ANZEIGEN = 23;
    public static final int EREIGNIS_SUCHFELD_FOCUS_SETZEN = 24;
    public int ereignis = -1;
    public String klasse = "";
    private static EventListenerList listeners = new EventListenerList();

    public MSearchListenerMediathekView(int eereignis, String kklasse) {
        ereignis = eereignis;
        klasse = kklasse;
    }

    public void ping() {
    }

    public static synchronized void addListener(MSearchListenerMediathekView listener) {
        listeners.add(MSearchListenerMediathekView.class, listener);
    }

    public static synchronized void notify(int ereignis, String klasse) {
        for (MSearchListenerMediathekView l : listeners.getListeners(MSearchListenerMediathekView.class)) {
            if (l.ereignis == ereignis) {
                if (!l.klasse.equals(klasse)) {
                    // um einen Kreislauf zu verhindern
                    try {
                        l.pingen();
                    } catch (Exception ex) {
                        MSearchLog.fehlerMeldung(562314008, MSearchLog.FEHLER_ART_PROG, "ListenerMediathekView.notifyMediathekListener", ex);
                    }
                }
            }
        }
    }

    private void pingen() {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                ping();
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ping();
                    }
                });
            }
        } catch (Exception ex) {
            MSearchLog.fehlerMeldung(698989743, MSearchLog.FEHLER_ART_PROG, "ListenerMediathekView.pingen", ex);
        }
    }
}