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
package msearch.filmeSuchen.sender;

import msearch.filmeSuchen.MSearchFilmeSuchen;
import msearch.io.MSearchGetUrl;
import msearch.daten.MSearchConfig;
import msearch.daten.DatenFilm;
import msearch.tool.DatumZeit;
import msearch.tool.MSearchConst;
import msearch.tool.MSearchLog;
import msearch.tool.MSearchStringBuilder;

public class Mediathek3Sat extends MediathekReader implements Runnable {

    public static final String SENDER = "3Sat";
    private final String MUSTER_ALLE = "http://www.3sat.de/mediathek/rss/mediathek.xml";

    public Mediathek3Sat(MSearchFilmeSuchen ssearch, int startPrio) {
        super(ssearch, /* name */ SENDER, /* threads */ 2, /* urlWarten */ 500, startPrio);
    }

    @Override
    void addToList() {
        final String ADRESSE = "http://www.3sat.de/page/?source=/specials/133576/index.html";
        final String MUSTER_URL = "<a href=\"/mediaplayer/rss/mediathek";
        listeThemen.clear();
        MSearchStringBuilder seite = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        meldungStart();
        //seite = new GetUrl(daten).getUriArd(ADRESSE, seite, "");
        seite = getUrlIo.getUri_Iso(nameSenderMReader, ADRESSE, seite, "");
        int pos1 = 0;
        int pos2;
        String url = "";
        while ((pos1 = seite.indexOf(MUSTER_URL, pos1)) != -1) {
            try {
                pos1 += MUSTER_URL.length();
                if ((pos2 = seite.indexOf("\"", pos1)) != -1) {
                    url = seite.substring(pos1, pos2);
                }
                if (url.equals("")) {
                    continue;
                }
                // in die Liste eintragen
                String[] add = new String[]{"http://www.3sat.de/mediaplayer/rss/mediathek" + url, ""};
                listeThemen.addUrl(add);
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-498653287, MSearchLog.FEHLER_ART_MREADER, "Mediathek3sat.addToList", ex);
            }
        }
        if (MSearchConfig.getStop()) {
            meldungThreadUndFertig();
        } else if (listeThemen.size() == 0) {
            meldungThreadUndFertig();
        } else {
            listeSort(listeThemen, 1);
            // noch den RSS für alles anfügen
            // Liste von http://www.3sat.de/mediathek/rss/mediathek.xml holen
            String[] add = new String[]{MUSTER_ALLE, ""};
            listeThemen.add(0, add); // alle nachfolgenden Filme ersetzen Filme die bereits in der Liste sind
            meldungAddMax(listeThemen.size());
            for (int t = 0; t < maxThreadLaufen; ++t) {
                //new Thread(new ThemaLaden()).start();
                Thread th = new Thread(new ThemaLaden());
                th.setName(nameSenderMReader + t);
                th.start();
            }

        }
    }

    private class ThemaLaden implements Runnable {

        MSearchGetUrl getUrl = new MSearchGetUrl(wartenSeiteLaden);
        private MSearchStringBuilder seite1 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        private MSearchStringBuilder seite2 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);

        @Override
        public synchronized void run() {
            try {
                meldungAddThread();
                String[] link;
                while (!MSearchConfig.getStop() && (link = getListeThemen()) != null) {
                    meldungProgress(link[0]);
                    laden(link[0] /* url */, link[1] /* Thema */);
                }
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-987452384, MSearchLog.FEHLER_ART_MREADER, "Mediathek3Sat.ThemaLaden.run", ex);
            }
            meldungThreadUndFertig();
        }

        void laden(String url_rss, String thema_rss) {
            // <title>3sat.schweizweit: Mediathek-Beiträge</title>
            final String MUSTER_URL = "type=\"video/x-ms-asf\" url=\"";
            final String MUSTER_URL_QUICKTIME_1 = "type=\"video/quicktime\" url=\"http://hstreaming.zdf.de/3sat/veryhigh";
            final String MUSTER_URL_QUICKTIME_2 = "type=\"video/quicktime\" url=\"http://hstreaming.zdf.de/3sat/300";
            final String MUSTER_TITEL = "<title>";
            final String MUSTER_DESCRIPTION = "<media:description>";
            final String MUSTER_IMAGE = "<media:thumbnail url=\"";
            final String MUSTER_DURATION = "media:content duration=\"";
            final String MUSTER_DATUM = "<pubDate>";
            final String MUSTER_LINK = "<link>";
            boolean urlAlle = url_rss.equals(MUSTER_ALLE);
            seite1 = getUrlIo.getUri_Utf(nameSenderMReader, url_rss, seite1, "");
            int pos = 0;
            int pos1;
            int pos2;
            int ende = 0;
            String url;
            String thema = "3sat";
            String link;
            String datum;
            String zeit;
            long duration;
            String description;
            String imageUrl;
            String titel;
            String tmp;
            if ((pos = seite1.indexOf(MUSTER_TITEL, pos)) == -1) {
                return;
            } else {
                //für den HTML-Titel
                pos += MUSTER_TITEL.length();
                pos1 = pos;
                if (!urlAlle) {
                    if ((pos2 = seite1.indexOf("<", pos1)) != -1) {
                        thema = seite1.substring(pos1, pos2);
                        thema = thema.replace("3sat.", "");
                        if (thema.contains(":")) {
                            thema = thema.substring(0, thema.indexOf(":"));
                        }
                    }
                }

            }
            // erst mal auf den Start setzen
            if ((pos = seite1.indexOf("<item>")) == -1) {
                return;
            }
            while (!MSearchConfig.getStop() && (pos = seite1.indexOf(MUSTER_TITEL, pos)) != -1) {
                pos += MUSTER_TITEL.length();
                ende = seite1.indexOf(MUSTER_TITEL, pos); // beginnt der nächste Film
                url = "";
                link = "";
                datum = "";
                zeit = "";
                titel = "";
                duration = 0;
                try {
                    pos1 = pos;
                    if ((pos2 = seite1.indexOf("<", pos1)) != -1) {
                        titel = seite1.substring(pos1, pos2);
                        if (titel.contains(":") && (titel.indexOf(":") + 1) < titel.length()) {
                            //enthält : und ist nicht das letztes zeichen
                            if (urlAlle) {
                                thema = titel.substring(0, titel.indexOf(":"));
                            }
                            titel = titel.substring(titel.indexOf(":") + 1);
                        }
                        titel = titel.trim();
                        if (!titel.equals("")) {
                            while (titel.charAt(0) == '\u00A0') {
                                titel = titel.substring(1);
                                if (titel.equals("")) {
                                    break;
                                }
                            }
                        }
                    }
                    link = seite1.extract(MUSTER_LINK, "<", pos);
                    // Film über die ID suchen
                    boolean ok = false;
                    if (link.contains("?obj=")) {
                        String id = link.substring(link.indexOf("?obj=") + "?obj=".length());
                        if (id.isEmpty()) {
                            MSearchLog.fehlerMeldung(-912690789, MSearchLog.FEHLER_ART_MREADER, "Mediathek3sat.laden", "keine id: " + url_rss);
                        } else {
                            id = "http://www.3sat.de/mediathek/xmlservice/web/beitragsDetails?ak=web&id=" + id;
                            meldung(id);
                            DatenFilm film = MediathekZdf.filmHolenId(getUrl, seite2, nameSenderMReader, thema, titel, link, id);
                            if (film == null) {
                                // dann mit der herkömmlichen Methode versuchen
                                MSearchLog.fehlerMeldung(-925464987, MSearchLog.FEHLER_ART_MREADER, "Mediathek3sat.laden", "auf die alte Art: " + url_rss);
                            } else {
                                // dann wars gut
                                addFilm(film);
                                ok = true;
                            }
                        }
                    }
                    // =============================================================================
                    // URL dann auf die herkömmliche Art
                    if (!ok) {
                        tmp = seite1.extract(MUSTER_DURATION, "\"", pos);
                        try {
                            duration = Long.parseLong(tmp);
                        } catch (Exception ex) {
                            MSearchLog.fehlerMeldung(-363524108, MSearchLog.FEHLER_ART_MREADER, "Mediathek3Sat.addToList", "duration");
                        }
                        tmp = seite1.extract(MUSTER_DATUM, "<", pos);
                        if (tmp.equals("")) {
                            MSearchLog.fehlerMeldung(-987453983, MSearchLog.FEHLER_ART_MREADER, "Mediathek3Sat.addToList", "keine Datum");
                        } else {
                            datum = DatumZeit.convertDatum(tmp);
                            zeit = DatumZeit.convertTime(tmp);
                        }
                        description = seite1.extract(MUSTER_DESCRIPTION, "</media:description>", pos);
                        imageUrl = seite1.extract(MUSTER_IMAGE, "\"", pos);
                        pos1 = seite1.indexOf(MUSTER_URL, pos);
                        if (pos1 != -1 && (ende == -1 || pos1 < ende)) {
                            // asx
                            pos1 += MUSTER_URL.length();
                            if ((pos2 = seite1.indexOf("\"", pos1)) != -1) {
                                url = seite1.substring(pos1, pos2);
                            }
                            if (!url.equals("") && url.endsWith("asx")) {
                                url = url.replace("/300/", "/veryhigh/");
                                flashHolen(thema, titel, link, url, datum, zeit, duration, description, imageUrl);
                            }
                        } else {
                            // dann mit Quicktime-Link versuchen
                            pos1 = seite1.indexOf(MUSTER_URL_QUICKTIME_1, pos);
                            if (pos1 != -1 && (ende == -1 || pos1 < ende)) {
                                pos1 += MUSTER_URL_QUICKTIME_1.length();
                                if ((pos2 = seite1.indexOf("\"", pos1)) != -1) {
                                    url = seite1.substring(pos1, pos2);
                                }
                                if (!url.equals("") && url.endsWith("mov")) {
                                    url = "http://hstreaming.zdf.de/3sat/veryhigh" + url;
                                    quicktimeHolen(thema, titel, link, url, datum, zeit, duration, description, imageUrl);
                                }
                            }
                        }
                        if (url.equals("")) {
                            MSearchLog.fehlerMeldung(-976432589, MSearchLog.FEHLER_ART_MREADER, "Mediathek3Sat.addToList", new String[]{"keine URL:", titel, url_rss});
                        }
                    }
                } catch (Exception ex) {
                    MSearchLog.fehlerMeldung(-823694892, MSearchLog.FEHLER_ART_MREADER, "Mediathek3Sat.laden", ex);
                }
            } //while, die ganz große Schleife
        }

        private void flashHolen(String thema, String titel, String urlThema, String urlFilm, String datum, String zeit, long durationInSeconds, String description, String imageUrl) {
            meldung(urlFilm);
            //DatenFilm f = MediathekZdf.flash(getUrl, seite2, nameSenderMReader, thema, titel, urlThema, urlFilm, datum, zeit);
            DatenFilm f = MediathekZdf.flash(getUrl, seite2, nameSenderMReader, thema, titel, urlThema, urlFilm, datum, zeit, durationInSeconds, description, imageUrl, new String[]{});
            if (f != null) {
                addFilm(f);
            }
        }

        private void quicktimeHolen(String thema, String titel, String urlThema, String urlFilm, String datum, String zeit, long durationInSeconds, String description, String imageUrl) {
            meldung(urlFilm);
            //DatenFilm f = MediathekZdf.flash(getUrl, seite2, nameSenderMReader, thema, titel, urlThema, urlFilm, datum, zeit);
            DatenFilm f = MediathekZdf.quicktime(getUrl, seite2, nameSenderMReader, thema, titel, urlThema, urlFilm, datum, zeit, durationInSeconds, description, "", imageUrl, new String[]{});
            if (f != null) {
                addFilm(f);
            }
        }

        private synchronized String[] getListeThemen() {
            return listeThemen.pollFirst();
        }
    }
}