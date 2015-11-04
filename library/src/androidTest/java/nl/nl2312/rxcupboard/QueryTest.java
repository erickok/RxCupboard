package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class QueryTest extends InstrumentationTestCase {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private Cupboard cupboard;
	private SQLiteDatabase db;
	private RxDatabase rxDatabase;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		getInstrumentation().getContext().deleteDatabase(TEST_DATABASE);
		db = new TestDbHelper(getInstrumentation().getContext(), cupboard, TEST_DATABASE).getWritableDatabase();
		rxDatabase = RxCupboard.with(cupboard, db);

		// Insert 10 rows in the TestEntity table
		Observable.range(1, 10).map(new Func1<Integer, TestEntity>() {
			@Override
			public TestEntity call(Integer integer) {
				TestEntity testEntity = new TestEntity();
				testEntity.string = "Test";
				testEntity.time = integer.longValue();
				return testEntity;
			}
		}).subscribe(rxDatabase.put());

	}

	public void testQueryAll() {

		// Emit all 10 items from the TestEntity table
		final AtomicInteger id = new AtomicInteger();
		rxDatabase.query(TestEntity.class).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertEquals(id.incrementAndGet(), testEntity._id.intValue());
				assertEquals(id.get(), testEntity.time);
				assertEquals("Test", testEntity.string);
			}
		}).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(10, count.intValue());
			}
		});

	}

	public void testQuerySelection() {

		// Emit the 5 items from the TestEntity table with id <= 5
		final AtomicInteger id = new AtomicInteger();
		rxDatabase.query(TestEntity.class, "_id <= ?", Integer.toString(5)).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertEquals(id.incrementAndGet(), testEntity._id.intValue());
				assertEquals(id.get(), testEntity.time);
				assertEquals("Test", testEntity.string);
			}
		}).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(5, count.intValue());
			}
		});

	}

	public void testQueryBuilder() {

		// Emit the 5 items from the TestEntity table with id <= 5 using the Cupboard QueryBuilder
		final AtomicInteger id = new AtomicInteger();
		rxDatabase.query(rxDatabase.buildQuery(TestEntity.class).withSelection("_id <= ?", Integer.toString(5)))
				.doOnNext(new Action1<TestEntity>() {
					@Override
					public void call(TestEntity testEntity) {
						assertEquals(id.incrementAndGet(), testEntity._id.intValue());
						assertEquals(id.get(), testEntity.time);
						assertEquals("Test", testEntity.string);
					}
				}).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(5, count.intValue());
			}
		});

	}

	public void testReactivePull() {

		// Normally there are 10 items
		final AtomicInteger allCount = new AtomicInteger();
		rxDatabase.query(TestEntity.class).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				allCount.incrementAndGet();
			}
		}).subscribe();
		assertEquals(10, allCount.intValue());

		// Ask for only 5 elements of the 10 in the table
		final AtomicInteger pullCount = new AtomicInteger();
		rxDatabase.query(TestEntity.class).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				pullCount.incrementAndGet();
			}
		}).take(5).subscribe();
		assertEquals(5, pullCount.intValue());

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		db.close();
	}

}
