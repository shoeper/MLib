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

import java.util.LinkedList;
import msearch.daten.DatenFilm;
import msearch.daten.MSearchConfig;
import msearch.filmeSuchen.MSearchFilmeSuchen;
import msearch.io.MSearchGetUrl;
import msearch.tool.MSearchConst;
import msearch.tool.MSearchLog;
import msearch.tool.MSearchStringBuilder;
import org.apache.commons.lang3.StringEscapeUtils;

public class MediathekSwr extends MediathekReader implements Runnable {

    public static final String SENDER = "SWR";

    public MediathekSwr(MSearchFilmeSuchen ssearch, int startPrio) {
        super(ssearch, /* name */ SENDER, /* threads */ 2, /* urlWarten */ 2000, startPrio);
    }

    //===================================
    // public
    //===================================
    @Override
    public synchronized void addToList() {
        meldungStart();
        //Theman suchen
        listeThemen.clear();
        addToList__("http://swrmediathek.de/tvlist.htm");
        if (MSearchConfig.getStop()) {
            meldungThreadUndFertig();
        } else if (listeThemen.size() == 0) {
            meldungThreadUndFertig();
        } else {
            meldungAddMax(listeThemen.size());
            for (int t = 0; t < maxThreadLaufen; ++t) {
                //new Thread(new ThemaLaden()).start();
                Thread th = new Thread(new ThemaLaden());
                th.setName(nameSenderMReader + t);
                th.start();
            }
        }
    }

    //===================================
    // private
    //===================================
    private void addToList__(String ADRESSE) {
        //Theman suchen
        final String MUSTER_URL = "<a href=\"tvshow.htm?show=";
        final String MUSTER_THEMA = "title=\"";
        MSearchStringBuilder strSeite = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        strSeite = getUrlIo.getUri(nameSenderMReader, ADRESSE, MSearchConst.KODIERUNG_UTF, 2, strSeite, "");
        int pos = 0;
        int pos1;
        int pos2;
        String url;
        String thema = "";
        while (!MSearchConfig.getStop() && (pos = strSeite.indexOf(MUSTER_URL, pos)) != -1) {
            pos += MUSTER_URL.length();
            pos1 = pos;
            pos2 = strSeite.indexOf("\"", pos);
            if (pos1 != -1 && pos2 != -1 && pos1 != pos2) {
                url = strSeite.substring(pos1, pos2);
                pos = pos2;
                pos = strSeite.indexOf(MUSTER_THEMA, pos);
                pos += MUSTER_THEMA.length();
                pos1 = pos;
                pos2 = strSeite.indexOf("\"", pos);
                if (pos1 != -1 && pos2 != -1) {
                    thema = strSeite.substring(pos1, pos2);
                    thema = StringEscapeUtils.unescapeHtml4(thema.trim()); //wird gleich benutzt und muss dann schon stimmen
                }
                if (url.equals("")) {
                    MSearchLog.fehlerMeldung(-163255009, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.addToList__", "keine URL");
                } else {
                    //url = url.replace("&amp;", "&");
                    String[] add = new String[]{"http://swrmediathek.de/tvshow.htm?show=" + url, thema};
                    listeThemen.addUrl(add);
                }
            }
        }
    }

    private class ThemaLaden implements Runnable {

        MSearchGetUrl getUrl = new MSearchGetUrl(wartenSeiteLaden);
        private MSearchStringBuilder strSeite1 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);
        private MSearchStringBuilder strSeite2 = new MSearchStringBuilder(MSearchConst.STRING_BUFFER_START_BUFFER);

        public ThemaLaden() {
        }

        @Override
        public void run() {
            try {
                meldungAddThread();
                String[] link = null;
                while (!MSearchConfig.getStop() && (link = listeThemen.getListeThemen()) != null) {
                    themenSeitenSuchen(link[0] /* url */, link[1] /* Thema */);
                    meldungProgress(link[0]);
                }
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-739285690, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.SenderThemaLaden.run", ex);
            }
            meldungThreadUndFertig();
        }

        private void themenSeitenSuchen(String strUrlFeed, String thema) {
            final String MUSTER_URL = "<li><a class=\"plLink\" href=\"player.htm?show=";
            //strSeite1 = getUrl.getUri_Utf(nameSenderMReader, strUrlFeed, strSeite1, thema);
            strSeite1 = getUrl.getUri(nameSenderMReader, strUrlFeed, MSearchConst.KODIERUNG_UTF, 2 /* versuche */, strSeite1, thema);
            meldung(strUrlFeed);
            int pos1 = 0;
            int pos2;
            String url;
            int max = 0;
            while (!MSearchConfig.getStop() && (pos1 = strSeite1.indexOf(MUSTER_URL, pos1)) != -1) {
                if (!MSearchConfig.senderAllesLaden) {
                    ++max;
                    if (max > 2) {
                        break;
                    }
                } else {
                    ++max;
                    if (max > 20) {
                        break;
                    }
                }
                pos1 += MUSTER_URL.length();
                if ((pos2 = strSeite1.indexOf("\"", pos1)) != -1) {
                    url = strSeite1.substring(pos1, pos2);
                    if (url.equals("")) {
                        MSearchLog.fehlerMeldung(-875012369, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.addFilme2", "keine URL, Thema: " + thema);
                    } else {
                        url = "http://swrmediathek.de/AjaxEntry?callback=jsonp1347979401564&ekey=" + url;
                        json(strUrlFeed, thema, url);
                    }

                }

            }
        }

        private void json(String strUrlFeed, String thema, String urlJson) {
            //:"entry_media","attr":{"val0":"h264","val1":"3","val2":"rtmp://fc-ondemand.swr.de/a4332/e6/swr-fernsehen/landesschau-rp/aktuell/2012/11/582111.l.mp4",
            // oder
            // "entry_media":"http://mp4-download.swr.de/swr-fernsehen/zur-sache-baden-wuerttemberg/das-letzte-wort-podcast/20120913-2015.m.mp4"
            // oder
            // :"entry_media","attr":{"val0":"flashmedia","val1":"1","val2":"rtmp://fc-ondemand.swr.de/a4332/e6/swr-fernsehen/eisenbahn-romantik/381104.s.flv","val3":"rtmp://fc-ondemand.swr.de/a4332/e6/"},"sub":[]},{"name":"entry_media","attr":{"val0":"flashmedia","val1":"2","val2":"rtmp://fc-ondemand.swr.de/a4332/e6/swr-fernsehen/eisenbahn-romantik/381104.m.flv","val3":"rtmp://fc-ondemand.swr.de/a4332/e6/"},"sub":[]

            // "entry_title":"\"Troika-Tragödie - Verspielt die Regierung unser Steuergeld?\"

            final String MUSTER_TITEL_1 = "\"entry_title\":\"";
            final String MUSTER_TITEL_2 = "\"entry_title\":\"\\\"";
            final String MUSTER_DATUM = "\"entry_pdatehd\":\"";
            final String MUSTER_DAUER = "\"entry_durat\":\"";
            final String MUSTER_ZEIT = "\"entry_pdateht\":\"";
            final String MUSTER_URL_START = "{\"name\":\"entry_media\"";
            final String MUSTER_URL_1 = "\"entry_media\",\"attr\":{\"val0\":\"flashmedia\",\"val1\":\"\",\"val2\":\"http";
            final String MUSTER_PROT_1 = "http";
            final String MUSTER_URL_2 = "\"entry_media\",\"attr\":{\"val0\":\"h264\",\"val1\":\"3\",\"val2\":\"http";
            final String MUSTER_PROT_2 = "http";
            final String MUSTER_URL_3 = "\"entry_media\",\"attr\":{\"val0\":\"h264\",\"val1\":\"2\",\"val2\":\"rtmp";
            final String MUSTER_PROT_3 = "rtmp";
            final String MUSTER_URL_4 = "entry_media\",\"attr\":{\"val0\":\"flashmedia\",\"val1\":\"2\",\"val2\":\"rtmp";
            final String MUSTER_PROT_4 = "rtmp";

            final String MUSTER_DESCRIPTION = "\"entry_descl\":\"";
            final String MUSTER_THUMBNAIL_URL = "\"entry_image_16_9\":\"";
            int pos, pos1;
            int pos2;
            String url = "", urlKlein = "";
            String titel = "";
            String datum = "";
            String zeit = "";
            long dauer = 0;
            String description = "";
            String thumbnailUrl = "";
            String[] keywords = null;
            String tmp;
            try {
                strSeite2 = getUrl.getUri_Utf(nameSenderMReader, urlJson, strSeite2, "");
                if ((pos1 = strSeite2.indexOf(MUSTER_TITEL_1)) != -1) {
                    pos1 += MUSTER_TITEL_1.length();

                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        titel = strSeite2.substring(pos1, pos2);
                    }
                }
                if (titel.startsWith("\\") && (pos1 = strSeite2.indexOf(MUSTER_TITEL_2)) != -1) {
                    pos1 += MUSTER_TITEL_2.length();
                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        titel = strSeite2.substring(pos1, pos2);
                        if (titel.endsWith("\\")) {
                            titel = titel.substring(0, titel.length() - 2);
                        }
                    }
                }
                if ((pos1 = strSeite2.indexOf(MUSTER_DAUER)) != -1) {
                    pos1 += MUSTER_DAUER.length();
                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        String d = null;
                        try {
                            d = strSeite2.substring(pos1, pos2);
                            if (!d.equals("")) {
                                String[] parts = d.split(":");
                                long power = 1;
                                for (int i = parts.length - 1; i >= 0; i--) {
                                    dauer += Long.parseLong(parts[i]) * power;
                                    power *= 60;
                                }
                            }
                        } catch (Exception ex) {
                            MSearchLog.fehlerMeldung(-679012497, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json", "d: " + (d == null ? " " : d));
                        }
                    }
                }

                if ((pos1 = strSeite2.indexOf(MUSTER_DESCRIPTION)) != -1) {
                    pos1 += MUSTER_DESCRIPTION.length();
                    if ((pos2 = strSeite2.indexOf("\",", pos1)) != -1) {
                        description = strSeite2.substring(pos1, pos2);
                    }
                }

                if ((pos1 = strSeite2.indexOf(MUSTER_THUMBNAIL_URL)) != -1) {
                    pos1 += MUSTER_THUMBNAIL_URL.length();
                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        thumbnailUrl = strSeite2.substring(pos1, pos2);
                    }
                }

                keywords = extractKeywords(strSeite2);
                if ((pos1 = strSeite2.indexOf(MUSTER_DATUM)) != -1) {
                    pos1 += MUSTER_DATUM.length();
                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        datum = strSeite2.substring(pos1, pos2);
                        if (datum.length() < 10) {
                            if (datum.contains(".")) {
                                if ((tmp = datum.substring(0, datum.indexOf("."))).length() != 2) {
                                    datum = "0" + datum;
                                }
                            }
                            if (datum.indexOf(".") != datum.lastIndexOf(".")) {
                                if ((tmp = datum.substring(datum.indexOf(".") + 1, datum.lastIndexOf("."))).length() != 2) {
                                    datum = datum.substring(0, datum.indexOf(".") + 1) + "0" + datum.substring(datum.indexOf(".") + 1);
                                }
                            }
                        }
                    }
                }
                if ((pos1 = strSeite2.indexOf(MUSTER_ZEIT)) != -1) {
                    pos1 += MUSTER_ZEIT.length();
                    if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                        zeit = strSeite2.substring(pos1, pos2);
                        if (zeit.length() <= 5) {
                            zeit = zeit.trim() + ":00";
                        }
                        zeit = zeit.replace(".", ":");
                        if (zeit.length() < 8) {
                            if (zeit.contains(":")) {
                                if ((tmp = zeit.substring(0, zeit.indexOf(":"))).length() != 2) {
                                    zeit = "0" + zeit;
                                }
                            }
                            if (zeit.indexOf(":") != zeit.lastIndexOf(":")) {
                                if ((tmp = zeit.substring(zeit.indexOf(":") + 1, zeit.lastIndexOf(":"))).length() != 2) {
                                    zeit = zeit.substring(0, zeit.indexOf(":") + 1) + "0" + zeit + zeit.substring(zeit.lastIndexOf(":"));
                                }
                            }
                        }
                    }
                }
                // entweder
                pos = 0;
                while ((pos = strSeite2.indexOf(MUSTER_URL_START, pos)) != -1) {
                    if (!url.isEmpty() && !urlKlein.isEmpty()) {
                        break;
                    }
                    pos += "{\"name\"".length();
                    if ((pos1 = strSeite2.indexOf(MUSTER_URL_1, pos)) != -1) {
                        pos1 += MUSTER_URL_1.length();
                        if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                            tmp = strSeite2.substring(pos1, pos2);
                            if (!tmp.isEmpty()) {
                                tmp = MUSTER_PROT_1 + tmp;
                                if (tmp.endsWith("m.mp4") && urlKlein.isEmpty()) {
                                    urlKlein = tmp;
                                }
                                if (tmp.endsWith("l.mp4") && url.isEmpty()) {
                                    url = tmp;
                                }
                                continue;
                            } else {
                                MSearchLog.fehlerMeldung(-468200690, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json-1", thema + " " + urlJson);
                            }
                        }
                    }
                }
                // oder
                pos = 0;
                while ((pos = strSeite2.indexOf(MUSTER_URL_START, pos)) != -1) {
                    if (!url.isEmpty() && !urlKlein.isEmpty()) {
                        break;
                    }
                    pos += "{\"name\"".length();
                    if ((pos1 = strSeite2.indexOf(MUSTER_URL_2, pos)) != -1) {
                        pos1 += MUSTER_URL_2.length();
                        if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                            tmp = strSeite2.substring(pos1, pos2);
                            if (!tmp.isEmpty()) {
                                tmp = MUSTER_PROT_2 + tmp;
                                if (tmp.endsWith("m.mp4") && urlKlein.isEmpty()) {
                                    urlKlein = tmp;
                                }
                                if (tmp.endsWith("l.mp4") && url.isEmpty()) {
                                    url = tmp;
                                }
                                continue;
                            } else {
                                MSearchLog.fehlerMeldung(-468200690, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json-1", thema + " " + urlJson);
                            }
                        }
                    }
                }
                // oder
                pos = 0;
                while ((pos = strSeite2.indexOf(MUSTER_URL_START, pos)) != -1) {
                    if (!url.isEmpty() && !urlKlein.isEmpty()) {
                        break;
                    }
                    pos += "{\"name\"".length();
                    if ((pos1 = strSeite2.indexOf(MUSTER_URL_3, pos)) != -1) {
                        pos1 += MUSTER_URL_3.length();
                        if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                            tmp = strSeite2.substring(pos1, pos2);
                            if (!tmp.isEmpty()) {
                                tmp = MUSTER_PROT_3 + tmp;
                                if (tmp.endsWith("m.mp4") && urlKlein.isEmpty()) {
                                    urlKlein = tmp;
                                }
                                if (tmp.endsWith("l.mp4") && url.isEmpty()) {
                                    url = tmp;
                                }
                                continue;
                            } else {
                                MSearchLog.fehlerMeldung(-468200690, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json-1", thema + " " + urlJson);
                            }
                        }
                    }
                }
                // oder
                pos = 0;
                while ((pos = strSeite2.indexOf(MUSTER_URL_START, pos)) != -1) {
                    if (!url.isEmpty() && !urlKlein.isEmpty()) {
                        break;
                    }
                    pos += "{\"name\"".length();
                    if ((pos1 = strSeite2.indexOf(MUSTER_URL_4, pos)) != -1) {
                        pos1 += MUSTER_URL_4.length();
                        if ((pos2 = strSeite2.indexOf("\"", pos1)) != -1) {
                            tmp = strSeite2.substring(pos1, pos2);
                            if (!tmp.isEmpty()) {
                                tmp = MUSTER_PROT_4 + tmp;
                                if (tmp.endsWith("m.mp4") && urlKlein.isEmpty()) {
                                    urlKlein = tmp;
                                }
                                if (tmp.endsWith("l.mp4") && url.isEmpty()) {
                                    url = tmp;
                                }
                                if (tmp.endsWith("m.flv") && urlKlein.isEmpty()) {
                                    urlKlein = tmp;
                                }
                                if (tmp.endsWith("l.flv") && url.isEmpty()) {
                                    url = tmp;
                                }
                                continue;
                            } else {
                                MSearchLog.fehlerMeldung(-468200690, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json-1", thema + " " + urlJson);
                            }
                        }
                    }
                }
                if (url.isEmpty() && urlKlein.isEmpty()) {
                    MSearchLog.fehlerMeldung(-203690478, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.jason-2", thema + " " + urlJson);
                } else {
                    if (url.isEmpty()) {
                        url = urlKlein;
                        urlKlein = "";
                    }
                    DatenFilm film = new DatenFilm(nameSenderMReader, thema, strUrlFeed, titel, url, ""/*rtmpURL*/, datum, zeit, dauer, description,
                            thumbnailUrl, keywords);
                    if (!urlKlein.isEmpty()) {
                        film.addUrlKlein(urlKlein, "");
                    }
                    addFilm(film);
                }
            } catch (Exception ex) {
                MSearchLog.fehlerMeldung(-939584720, MSearchLog.FEHLER_ART_MREADER, "MediathekSwr.json-3", thema + " " + urlJson);
            }
        }

        private String[] extractKeywords(MSearchStringBuilder strSeite2) {
            // {"name":"entry_keywd","attr":{"val":"Fernsehserie"},"sub":[]}
            final String MUSTER_KEYWORD_START = "{\"name\":\"entry_keywd\",\"attr\":{\"val\":\"";
            final String MUSTER_KEYWORD_END = "\"},\"sub\":[]}";

            LinkedList<String> keywords = new LinkedList<>();
            int pos = 0;
            while ((pos = strSeite2.indexOf(MUSTER_KEYWORD_START, pos)) != -1) {
                pos += MUSTER_KEYWORD_START.length();
                int end = strSeite2.indexOf(MUSTER_KEYWORD_END, pos);
                if (end != -1) {
                    String keyword = strSeite2.substring(pos, end);
                    keywords.add(keyword);
                    pos = end;
                }
            }

            return keywords.toArray(new String[keywords.size()]);
        }
    }
}