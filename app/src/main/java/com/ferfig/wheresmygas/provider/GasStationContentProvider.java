package com.ferfig.wheresmygas.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ferfig.wheresmygas.provider.GasStationContract.GasStationEntry;

public class GasStationContentProvider extends ContentProvider {

    // Define final integer constants for the directory of gas stations and a single item.
    // It's convention to use 100, 200, 300, etc for directories,
    // and related ints (101, 102, ..) for items in that directory.
    public static final int GAS_STATION = 100;
    public static final int GAS_STATION_WITH_ID = 101;

    // Declare a static variable for the Uri matcher that must be constructed
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    public static final String UNKNOWN_URI = "Unknown uri: ";

    // Define a static buildUriMatcher method that associates URI's with their int match
    public static UriMatcher buildUriMatcher() {
        // Initialize a UriMatcher
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // Add URI matches
        uriMatcher.addURI(GasStationContract.AUTHORITY, GasStationContract.PATH_GAS_STATIONS, GAS_STATION);
        uriMatcher.addURI(GasStationContract.AUTHORITY, GasStationContract.PATH_GAS_STATIONS + "/#", GAS_STATION_WITH_ID);
        return uriMatcher;
    }

    // Member variable for a GasStationDbHelper that's initialized in the onCreate() method
    private GasStationDbHelper mGasStationDbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        mGasStationDbHelper = new GasStationDbHelper(context);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        // Get access to underlying database (read-only for query)
        final SQLiteDatabase db = mGasStationDbHelper.getReadableDatabase();

        // Write URI match code and set a variable to return a Cursor
        int match = sUriMatcher.match(uri);
        Cursor retCursor;

        // Query for the gas stations directory
        if (match == GAS_STATION) {
            retCursor = db.query(GasStationEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder);
            // Default exception
        } else {
            throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        // Set a notification URI on the Cursor and return that Cursor
        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                retCursor.setNotificationUri(contentResolver, uri);
            }
        }

        // Return the desired Cursor
        return retCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = mGasStationDbHelper.getWritableDatabase();

        // Write URI matching code to identify the match for the gas stations directory
        int match = sUriMatcher.match(uri);
        Uri returnUri; // URI to be returned
        if (match == GAS_STATION) {// Insert new values into the database
            long id = db.insert(GasStationEntry.TABLE_NAME, null, values);
            if (id > 0) {
                returnUri = ContentUris.withAppendedId(GasStationEntry.CONTENT_URI, id);
            } else {
                throw new SQLException("Failed to insert row into " + uri);
            }
            // Default case throws an UnsupportedOperationException
        } else {
            throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        Context context = getContext();
        if (context != null) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                contentResolver.notifyChange(uri, null);
            }
        }

        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get access to the database and write URI matching code to recognize a single item
        final SQLiteDatabase db = mGasStationDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        // Keep track of the number of deleted gas stations
        int gasStationsDeleted; // starts as 0
        switch (match) {
            // Handle the single item case, recognized by the ID included in the URI path
            case GAS_STATION:
                // Use selections/selectionArgs
                gasStationsDeleted = db.delete(GasStationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            // Handle the single item case, recognized by the ID included in the URI path
            case GAS_STATION_WITH_ID:
                // Get the gas station ID from the URI path
                String id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this ID
                gasStationsDeleted = db.delete(GasStationEntry.TABLE_NAME, "_id=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }
        // Notify the resolver of a change and return the number of items deleted
        if (gasStationsDeleted != 0) {
            // A gas station (or more) was deleted, set notification
            Context context = getContext();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                if (contentResolver!=null) {
                    contentResolver.notifyChange(uri, null);
                }
            }
        }
        // Return the number of gas stations deleted
        return gasStationsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get access to underlying database
        final SQLiteDatabase db = mGasStationDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        // Keep track of the number of updated gas stations
        int gasStationsUpdated;

        if (match == GAS_STATION_WITH_ID) {// Get the gas station ID from the URI path
            String id = uri.getPathSegments().get(1);
            // Use selections/selectionArgs to filter for this ID
            gasStationsUpdated = db.update(GasStationEntry.TABLE_NAME, values, "_id=?", new String[]{id});
            // Default exception
        } else {
            throw new UnsupportedOperationException(UNKNOWN_URI + uri);
        }

        // Notify the resolver of a change and return the number of items updated
        if (gasStationsUpdated != 0) {
            // A gas station (or more) was updated, set notification
            Context context = getContext();
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                if (contentResolver!=null) {
                    contentResolver.notifyChange(uri, null);
                }
            }
        }
        // Return the number of gas stations deleted
        return gasStationsUpdated;
    }
}
