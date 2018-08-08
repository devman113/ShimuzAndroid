package com.theshmuz.app.loaders;

import com.theshmuz.app.ShmuzHelper;
import com.theshmuz.app.UpdatorStatus;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

public class SavePositionTask extends AsyncTask<String, Void, Void> {

	private ShmuzHelper dbh;
	private UpdatorStatus updator;

	public SavePositionTask(ShmuzHelper dbh, UpdatorStatus updator) {
		this.dbh = dbh;
		this.updator = updator;
	}

	@Override
	protected Void doInBackground(String... params) {
		//need to get type, id, and position (as string for now...)
		String type = params[0];
		String id = params[1];
		int position = Integer.parseInt(params[2]);
		int duration = Integer.parseInt(params[3]); //only use if >=0...

		SQLiteDatabase db = dbh.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(ShmuzHelper.POSITION, position);
		if(duration >= 0) cv.put(ShmuzHelper.DURATION, duration);

		boolean isSeries = ShmuzHelper.isSeries(type);
		String tableName;
		String whereClause;
		String[] whereArgs;
		if(isSeries) {
			tableName = ShmuzHelper.TABLE_SERIES_CONTENT;
			whereClause = ShmuzHelper.SERIES_REF + "=? AND " + ShmuzHelper.SID + "=?";
			whereArgs = new String[]{type, id};
		}
		else {
			tableName = type;
			whereClause = ShmuzHelper.SID + "=?";
			whereArgs = new String[]{id};
		}

		db.update(tableName, cv, whereClause, whereArgs);

		updator.setStatus(UpdatorStatus.UPDATE_STATUS_SUCCESS);

		return null;
	}

}
