package nl.nl2312.rxcupboard;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nl.qbusict.cupboard.Cupboard;

class TestDbHelper extends SQLiteOpenHelper {

	private final Cupboard cupboard;

	TestDbHelper(Context context, Cupboard cupboard, String name) {
		super(context, name, null, 1);
		this.cupboard = cupboard;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		cupboard.withDatabase(db).createTables();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		cupboard.withDatabase(db).upgradeTables();
	}

}
