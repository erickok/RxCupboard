package nl.nl2312.rxcupboard;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class CursorTest extends InstrumentationTestCase {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private Cupboard cupboard;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		getInstrumentation().getContext().deleteDatabase(TEST_DATABASE);

	}

	public void testSimpleCursor() {

		MatrixCursor cursor = new MatrixCursor(new String[] {"_id", "string", "time"}, 2);
		cursor.addRow(new Object[]{1L, "Cursor", 1L});
		cursor.addRow(new Object[]{2L, "Cursor", 2L});
		RxCursor rxCursor = RxCupboard.with(cupboard, cursor);

		// Should emit 2 items
		rxCursor.iterate(TestEntity.class).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(2, count.intValue());
			}
		});

		// Items are emitted correctly
		rxCursor.iterate(TestEntity.class).subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertEquals("Cursor", testEntity.string);
				assertEquals(testEntity.time, testEntity._id.longValue());
			}
		});

	}

	public void testDbCursor() {

		TestDbHelper helper = new TestDbHelper(getInstrumentation().getContext(), cupboard, TEST_DATABASE);
		SQLiteDatabase db = helper.getWritableDatabase();

		// Insert some rows in the database to query
		RxDatabase local = RxCupboard.with(cupboard, db);
		Observable.range(1, 10).map(new Func1<Integer, Object>() {
			@Override
			public Object call(Integer integer) {
				TestEntity testEntity = new TestEntity();
				testEntity.string = "Cursor";
				testEntity.time = integer.longValue();
				return testEntity;
			}
		}).subscribe(local.put());

		Cursor cursor = db.rawQuery("select * from TestEntity", null);
		RxCursor rxCursor = RxCupboard.with(cupboard, cursor);

		// Should emit 10 items
		rxCursor.iterate(TestEntity.class).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(10, count.intValue());
			}
		});

		cursor.moveToFirst(); // Cupboard leaves the at the end

		// Items are emitted correctly
		rxCursor.iterate(TestEntity.class).subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertEquals("Cursor", testEntity.string);
				assertEquals(testEntity.time, testEntity._id.longValue());
			}
		});

		cursor.moveToFirst(); // Cupboard leaves the at the end

		// Reactive pull, such that only asked items are converted
		final AtomicInteger emitCount = new AtomicInteger();
		rxCursor.iterate(TestEntity.class).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				emitCount.getAndIncrement();
			}
		}).take(5).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer integer) {
				assertEquals(5, integer.intValue());
			}
		});
		assertEquals(5, emitCount.intValue());

		db.close();

	}

}
