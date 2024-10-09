package fr.neamar.kiss.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DB extends SQLiteOpenHelper {

    private final static String DB_NAME = "kiss.s3db";
    private final static int DB_VERSION = 8;

    DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        // `query` is a keyword so we escape it. See: https://www.sqlite.org/lang_keywords.html
        database.execSQL("CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, \"query\" TEXT, record TEXT NOT NULL)");
        database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
        database.execSQL("CREATE TABLE notes ( _id INTEGER PRIMARY KEY AUTOINCREMENT, content TEXT NOT NULL, timestamp INTEGER NOT NULL, tags TEXT NOT NULL)");
        createTags(database);
        addTimeStamps(database);
        addAppsTable(database);
    }

    private void createTags(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE tags ( _id INTEGER PRIMARY KEY AUTOINCREMENT, tag TEXT NOT NULL, record TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_tags_record ON tags(record);");
    }

    private void addTimeStamps(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE history ADD COLUMN timeStamp INTEGER DEFAULT 0  NOT NULL");
    }

    private void addAppsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS custom_apps ( _id INTEGER PRIMARY KEY AUTOINCREMENT, custom_flags INTEGER DEFAULT 0, component_name TEXT NOT NULL UNIQUE, name TEXT NOT NULL DEFAULT '' )");
        db.execSQL("CREATE INDEX IF NOT EXISTS index_component ON custom_apps(component_name);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w("onUpgrade", "Updating database from version " + oldVersion + " to version " + newVersion);
        // See
        // http://www.drdobbs.com/database/using-sqlite-on-android/232900584
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                case 2:
                case 3:
                    database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                            + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
                    // fall through
                case 4:
                    createTags(database);
                    // fall through
                case 5:
                    addTimeStamps(database);
                    // fall through
                case 6:
                case 7:
                    addAppsTable(database);
                    // fall through
                default:
                    break;
            }
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w("onDowngrade", "Updating database from version " + oldVersion + " to version " + newVersion);

        if (newVersion < oldVersion) {
            switch (newVersion) {
                case 7:
                case 6:
                    database.execSQL("DROP INDEX index_component");
                    database.execSQL("DROP TABLE custom_apps");
                    break;
                case 5:
                    throw new UnsupportedOperationException("Can't downgrade app below DB level 5");
                default:
                    break;
            }
        }
    }
}
