package pals.base.utils;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Date/time wrapper; builds on Java's calendar object, making it easier to
 * access date/time units and provides useful date/time operations.
 */
public class DateTime
{
    // Fields ******************************************************************
    private static Calendar cal = null;
    private final int year, month, day, hour, minute, second;
    // Methods - Constructors **************************************************
    public DateTime(int year, int month, int day, int hour, int minute, int second)
    {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minute = minute;
        this.second = second;
    }
    // Methods - Static ********************************************************
    
    // Methods *****************************************************************
    /**
     * Indicates if an object is the same as the current one; if the object is
     * not of this class-type, false is returned.
     * @param o The object being compared.
     * @return True = same, false = not the same.
     */
    @Override
    public boolean equals(Object o)
    {
        return o instanceof DateTime ? equals((DateTime)o) : false;
    }
    /**
     * Indicates if the two instances are the same time.
     * @param o The object being compared to this object.
     * @return True = same, false = not the same.
     */
    public boolean equals(DateTime o)
    {
        return this.year == o.year && this.month == o.month && this.day == o.day
                && this.minute == o.minute && this.second == o.second;
    }
    /**
     * Indicates if this datetime is the same day as the specified datetime.
     * @param o The datetime object to be compared.
     * @return True = same day, false = not the same day.
     */
    public boolean isSameDay(DateTime o)
    {
        return this.year == o.year && this.month == o.month && this.day == o.day;
    }
    // Methods - Static ********************************************************
    /**
     * @return Gets a new instance of the current date/time.
     */
    public static DateTime getInstance()
    {
        if(cal == null)
            cal = Calendar.getInstance();
        return new DateTime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
    }
    /**
     * @param timeZone The new time-zone to be used by all new DateTime
     * instances.
     */
    public static void setTimeZone(TimeZone timeZone)
    {
        cal = Calendar.getInstance(timeZone);
    }
    // Methods - Accessors *****************************************************
    /**
     * @return The current year.
     */
    public int getYear()
    {
        return year;
    }
    /**
     * @return The current month.
     */
    public int getMonth()
    {
        return month;
    }
    /**
     * @return The current day.
     */
    public int getDay()
    {
        return day;
    }
    /**
     * @return The current hour.
     */
    public int getHour()
    {
        return hour;
    }
    /**
     * @return The current minute.
     */
    public int getMinute()
    {
        return minute;
    }
    /**
     * @return The current second.
     */
    public int getSecond()
    {
        return second;
    }
}
