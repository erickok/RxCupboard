package nl.nl2312.rxcupboard2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardFactory;

public class RxCupboard {

	private RxCupboard() {
	}

	public static RxDatabase with(Cupboard cupboard, SQLiteDatabase db) {
		return new RxDatabase(cupboard, cupboard.withDatabase(db), db);
	}

	public static RxDatabase withDefault(SQLiteDatabase db) {
		return new RxDatabase(CupboardFactory.cupboard(), CupboardFactory.cupboard().withDatabase(db), db);
	}

	public static RxContentProvider with(Cupboard cupboard, Context context, Uri uri) {
		return new RxContentProvider(cupboard, context, uri);
	}

	public static RxContentProvider withDefault(Context context, Uri uri) {
		return new RxContentProvider(CupboardFactory.cupboard(), context, uri);
	}

	public static RxCursor with(Cupboard cupboard, Cursor cursor) {
		return new RxCursor(cupboard, cursor);
	}

	public static RxCursor withDefault(Cursor cursor) {
		return new RxCursor(CupboardFactory.cupboard(), cursor);
	}

}
