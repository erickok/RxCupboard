package nl.nl2312.rxcupboard;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.Flowable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CursorTest {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private Cupboard cupboard;

	@Before
	public void setUp() throws Exception {
		cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE);
	}

	@Test
	public void matrixCursor_iterate() throws InterruptedException {

		MatrixCursor cursor = new MatrixCursor(new String[]{"_id", "string", "time"}, 2);
		cursor.addRow(new Object[]{1L, "Cursor", 1L});
		cursor.addRow(new Object[]{2L, "Cursor", 2L});
		RxCursor rxCursor = RxCupboard.with(cupboard, cursor);

		// Should emit 2 items
		rxCursor.iterate(TestEntity.class)
				.test()
				.assertTerminated()
				.assertValueCount(2)
				.assertValueAt(0, new Predicate<TestEntity>() {
					@Override
					public boolean test(TestEntity testEntity1) throws Exception {
						return testEntity1._id == 1L &&
								testEntity1.string.equals("Cursor") &&
								testEntity1.time == 1L;
					}
				})
				.assertValueAt(1, new Predicate<TestEntity>() {
					@Override
					public boolean test(TestEntity testEntity2) throws Exception {
						return testEntity2._id == 2L &&
								testEntity2.string.equals("Cursor") &&
								testEntity2.time == 2L;
					}
				});

	}

	@Test
	public void dbCursor_iterate() {

		TestDbHelper helper = new TestDbHelper(InstrumentationRegistry.getTargetContext(), cupboard, TEST_DATABASE);
		SQLiteDatabase db = helper.getWritableDatabase();

		// Insert some rows in the database to query
		RxDatabase local = RxCupboard.with(cupboard, db);
		Flowable.range(1, 10)
				.map(new Function<Integer, TestEntity>() {
					@Override
					public TestEntity apply(Integer integer) throws Exception {
						TestEntity testEntity = new TestEntity();
						testEntity.string = "Cursor";
						testEntity.time = integer.longValue();
						return testEntity;
					}
				})
				.doOnNext(local.<TestEntity>put())
				.test();

		Cursor cursor = db.rawQuery("select * from TestEntity", null);
		RxCursor rxCursor = RxCupboard.with(cupboard, cursor);

		// Should emit 10 items
		rxCursor.iterate(TestEntity.class)
				.test()
				.assertTerminated()
				.assertValueCount(10);

		cursor.moveToFirst(); // Cupboard leaves the cursor at the end

		// Items are emitted correctly
		rxCursor.iterate(TestEntity.class)
				.doOnNext(new Consumer<TestEntity>() {
					@Override
					public void accept(TestEntity testEntity) throws Exception {
						assertTrue(testEntity._id >= 0);
						assertEquals(testEntity.string, "Cursor");
						assertEquals(testEntity.time, testEntity._id.longValue());
					}
				})
				.test()
				.assertTerminated();

		cursor.moveToFirst(); // Cupboard leaves the cursor at the end

		// Reactive pull, such that only asked items are converted
		rxCursor.iterate(TestEntity.class)
				.scan(0, new BiFunction<Integer, TestEntity, Integer>() {
					@Override
					public Integer apply(Integer count, TestEntity testEntity) throws Exception {
						return count + 1;
					}
				})
				.take(5)
				.test()
				.assertTerminated()
				.assertValues(0, 1, 2, 3, 4);

		db.close();

	}

}
