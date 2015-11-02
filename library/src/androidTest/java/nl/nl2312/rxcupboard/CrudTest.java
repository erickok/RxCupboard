package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;

import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class CrudTest extends InstrumentationTestCase {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private SQLiteDatabase db;
	private RxDatabase rxDatabase;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		Cupboard cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		getInstrumentation().getContext().deleteDatabase(TEST_DATABASE);
		db = new TestDbHelper(getInstrumentation().getContext(), cupboard, TEST_DATABASE).getWritableDatabase();
		rxDatabase = RxCupboard.with(cupboard, db);
	}

	public void testPutCountGetDelete() {

		long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = time;

		// Simple put of new item
		assertNull(testEntity._id);
		rxDatabase.put(testEntity);

		// Get returns one item
		Observable<TestEntity> getObservable = rxDatabase.get(TestEntity.class, testEntity._id);
		getObservable.count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(1, count.intValue());
			}
		});

		// Count
		Observable<Long> countObservable = rxDatabase.count(TestEntity.class);
		countObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(1, count.intValue());
			}
		});

		// Get returns correct item
		Observable<TestEntity> getEntityObservable = rxDatabase.get(TestEntity.class, testEntity._id);
		getEntityObservable.subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity getEntity) {
				assertEquals(testEntity._id, getEntity._id);
				assertEquals(testEntity.string, getEntity.string);
				assertEquals(testEntity.time, getEntity.time);
			}
		});

		// Simple put to update an item
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		rxDatabase.put(testEntity);
		assertNotNull(testEntity._id);
		assertEquals(testEntity.string, "Updated");
		assertNotSame(testEntity.time, updatedTime);

		// Simple delete
		boolean deleted = rxDatabase.delete(testEntity);
		assertEquals(true, deleted);

		Observable<Long> checkDeleteObservable = rxDatabase.count(TestEntity.class);
		checkDeleteObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(0, count.intValue());
			}
		});

		// Non-existing delete
		boolean missingDeleted = rxDatabase.delete(testEntity);
		assertEquals(false, missingDeleted);

	}

	public void testPutUpdateIds() {

		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = System.currentTimeMillis();

		// Empty id should create (and return) the db-assigned one
		assertNull(testEntity._id);
		long assigned = rxDatabase.put(testEntity);
		assertNotNull(testEntity._id);
		assertEquals((long)testEntity._id, assigned);

		// Updates to the id should be returned
		testEntity._id = 123456L;
		long updated = rxDatabase.put(testEntity);
		assertEquals((long)testEntity._id, 123456L);
		assertEquals(updated, 123456L);

	}

	public void testObservablePutDelete() {

		final long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Action";
		testEntity.time = time;

		// Observe put and use the object
		assertNull(testEntity._id);
		rxDatabase.putRx(testEntity).subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertNotNull(testEntity._id);
				assertEquals("Action", testEntity.string);
				assertEquals(time, testEntity.time);
			}
		});

		Observable<Long> checkPutObservable = rxDatabase.count(TestEntity.class);
		checkPutObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(1, count.intValue());
			}
		});

		// Observe deleted object
		rxDatabase.deleteRx(testEntity).doOnNext(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertNotNull(testEntity._id);
				assertEquals("Action", testEntity.string);
				assertEquals(time, testEntity.time);
			}
		}).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(1, count.intValue());
			}
		});

		Observable<Long> checkDeleteObservable = rxDatabase.count(TestEntity.class);
		checkDeleteObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(0, count.intValue());
			}
		});

	}

	public void testActionPutDelete() {

		final long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Action";
		testEntity.time = time;

		// Put as chain action
		assertNull(testEntity._id);
		Observable.just(testEntity).doOnNext(rxDatabase.put()).subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertNotNull(testEntity._id);
				assertEquals("Action", testEntity.string);
				assertEquals(time, testEntity.time);
			}
		});

		Observable<Long> checkPutObservable = rxDatabase.count(TestEntity.class);
		checkPutObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(1, count.intValue());
			}
		});

		// Delete as chain action
		rxDatabase.get(TestEntity.class, testEntity._id).doOnNext(rxDatabase.delete()).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(1, count.intValue());
			}
		});

		Observable<Long> checkDeleteObservable = rxDatabase.count(TestEntity.class);
		checkDeleteObservable.subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(0, count.intValue());
			}
		});

	}

	public void testMultiPutGetDelete() {

		// Generate 10 items, with explicit IDs set [1..10]
		Observable.range(1, 10).map(new Func1<Integer, Object>() {
			@Override
			public TestEntity call(Integer integer) {
				TestEntity e = new TestEntity();
				e._id = integer.longValue();
				e.string = "Multi";
				e.time = integer;
				return e;
			}
		}).doOnNext(rxDatabase.put()).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(10, count.intValue());
			}
		});

		rxDatabase.count(TestEntity.class).subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(10, count.intValue());
			}
		});

		// Get the items with assigned ids 1 to 10
		Observable.range(1, 10).flatMap(new Func1<Integer, Observable<TestEntity>>() {
			@Override
			public Observable<TestEntity> call(Integer id) {
				return rxDatabase.get(TestEntity.class, id.longValue());
			}
		}).subscribe(new Action1<TestEntity>() {
			@Override
			public void call(TestEntity testEntity) {
				assertEquals(testEntity.time, testEntity._id.intValue());
				assertEquals("Multi", testEntity.string);
			}
		});

		// Delete the items with assigned ids 1 to 10
		Observable.range(1, 10).map(new Func1<Integer, Long>() {
			@Override
			public Long call(Integer integer) {
				return integer.longValue();
			}
		}).doOnNext(rxDatabase.delete(TestEntity.class)).count().subscribe(new Action1<Integer>() {
			@Override
			public void call(Integer count) {
				assertEquals(10, count.intValue());
			}
		});

		rxDatabase.count(TestEntity.class).subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(0, count.intValue());
			}
		});

		// Put 10 new items to delete
		Observable.range(1, 10).map(new Func1<Integer, Object>() {
			@Override
			public TestEntity call(Integer integer) {
				TestEntity e = new TestEntity();
				e._id = integer.longValue();
				e.string = "Multi";
				e.time = integer;
				return e;
			}
		}).subscribe(rxDatabase.put());

		// Now delete all 10 items by their object
		Observable.range(1, 10).flatMap(new Func1<Integer, Observable<TestEntity>>() {
			@Override
			public Observable<TestEntity> call(Integer integer) {
				return rxDatabase.get(TestEntity.class, integer.longValue());
			}
		}).subscribe(rxDatabase.delete());

		rxDatabase.count(TestEntity.class).subscribe(new Action1<Long>() {
			@Override
			public void call(Long count) {
				assertEquals(0, count.intValue());
			}
		});

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		db.close();
	}

}
