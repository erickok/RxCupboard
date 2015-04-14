package nl.nl2312.rxcupboard;

import android.database.Cursor;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CursorCompartment;
import rx.Observable;

public class RxCursor {

	private final CursorCompartment cursor;

	RxCursor(Cupboard cupboard, Cursor cursor) {
		this.cursor = cupboard.withCursor(cursor);
	}

	public <T> Observable<T> iterate(Class<T> entityClass) {
		return Observable.from(cursor.iterate(entityClass));
	}

}
