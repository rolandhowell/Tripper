package rh.tripper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public final class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "tripper.db";
    public static final int DATABASE_VERSION = 1;

    private DatabaseHelper (Context context){
        super(context, DATABASE_NAME, null, 1);
    }

    public static class TripEntry implements BaseColumns {
        public static final String TABLE_NAME = "trips";
        public static final String COLUMN_NAME_TRIPNAME = "tripName";
        public static final String COLUMN_NAME_STARTDATE = "startDate";
        public static final String COLUMN_NAME_EMAIL = "email";
    }

    public static class StopEntry implements BaseColumns {
        public static final String TABLE_NAME = "stops";
        public static final String COLUMN_NAME_EMAIL = "email";
        public static final String COLUMN_NAME_ARRIVAL = "arrivalDate";
        public static final String COLUMN_NAME_DEPT = "deptDate";
        public static final String COLUMN_NAME_STOPNAME = "stopName";
        public static final String COLUMN_NAME_STOPDESC = "stopDesc";
        public static final String COLUMN_NAME_LAT = "lat";
        public static final String COLUMN_NAME_LONG = "long";
    }

    private static final String SQL_CREATE_TRIP_ENTRIES =
            "CREATE TABLE " + TripEntry.TABLE_NAME + " (" +
                    TripEntry._ID + " INTEGER PRIMARY KEY," +
                    TripEntry.COLUMN_NAME_TRIPNAME + " TEXT," +
                    TripEntry.COLUMN_NAME_STARTDATE + " DATE," +
                    TripEntry.COLUMN_NAME_EMAIL + " TEXT)";

    private static final String SQL_DELETE_TRIP_ENTRIES =
            "DROP TABLE IF EXISTS " + TripEntry.TABLE_NAME;

    private static final String SQL_CREATE_STOP_ENTRIES =
            "CREATE TABLE " + StopEntry.TABLE_NAME + " (" +
                    StopEntry._ID + " INTEGER PRIMARY KEY," +
                    StopEntry.COLUMN_NAME_EMAIL + " TEXT," +
                    StopEntry.COLUMN_NAME_ARRIVAL + " DATE," +
                    StopEntry.COLUMN_NAME_DEPT + " DATE," +
                    StopEntry.COLUMN_NAME_STOPNAME + " TEXT," +
                    StopEntry.COLUMN_NAME_STOPDESC + " TEXT," +
                    StopEntry.COLUMN_NAME_LAT + " TEXT," +
                    StopEntry.COLUMN_NAME_LONG + " TEXT)";

    private static final String SQL_DELETE_STOP_ENTRIES =
            "DROP TABLE IF EXISTS " + StopEntry.TABLE_NAME;


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TRIP_ENTRIES);
        db.execSQL(SQL_CREATE_STOP_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TRIP_ENTRIES);
        db.execSQL(SQL_DELETE_STOP_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
