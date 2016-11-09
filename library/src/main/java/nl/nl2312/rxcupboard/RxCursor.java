package nl.nl2312.rxcupboard;

import android.database.Cursor;

import io.reactivex.Flowable;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CursorCompartment;

public class RxCursor {

	private final CursorCompartment cursor;

	RxCursor(Cupboard cupboard, Cursor cursor) {
		this.cursor = cupboard.withCursor(cursor);
	}

	public <T> Flowable<T> iterate(Class<T> entityClass) {
		return Flowable.fromIterable(cursor.iterate(entityClass));
	}

}
