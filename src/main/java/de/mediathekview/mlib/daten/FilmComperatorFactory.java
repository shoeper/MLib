package de.mediathekview.mlib.daten;

import java.util.Comparator;

/**
 * A factory to build the film comperators.
 */
public class FilmComperatorFactory
{
    private static FilmComperatorFactory instance;

    public static FilmComperatorFactory getInstance()
    {
        if(instance == null)
        {
            instance = new FilmComperatorFactory();
        }
        return instance;
    }

    /**
     * The aviable comperator types for {@link Film}.
     */
    public enum FilmComperatorTypes {
        SENDER_COMPERAOR, TITEL_COMPERATOR, THEMA_COMPERATOR, DATE_COMPERATOR, DEFAULT_COMPERATOR
    }

    private FilmComperatorFactory()
    {
        super();
    }

    public Comparator<Film> getFilmComperator(FilmComperatorTypes aFilmComperatorType)
    {
        switch (aFilmComperatorType)
        {
            case DATE_COMPERATOR:
                return Comparator.comparing(Film::getTime);
            case SENDER_COMPERAOR:
                return Comparator.comparing(Film::getSender);
            case THEMA_COMPERATOR:
                return Comparator.comparing(Film::getThema);
            case TITEL_COMPERATOR:
                return Comparator.comparing(Film::getTitel);
            default:
                return createDefaultComperator();
        }
    }

    private Comparator<Film> createDefaultComperator()
    {
        return Comparator.comparing(Film::getSender).thenComparing(Film::getThema).thenComparing(Film::getTime);
    }


}