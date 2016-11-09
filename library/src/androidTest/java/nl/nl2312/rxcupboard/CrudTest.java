package nl.nl2312.rxcupboard;

import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import nl.qbusict.cupboard.Cupboard;
import nl.qbusict.cupboard.CupboardBuilder;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class CrudTest {

	private static final String TEST_DATABASE = "RxCupboardTest.db";

	private SQLiteDatabase db;
	private RxDatabase rxDatabase;
	private long singleTime;
	private Predicate<TestEntity> isDefaultTestEntity;

	@Before
	public void setUp() throws Exception {
		Cupboard cupboard = new CupboardBuilder().build();
		cupboard.register(TestEntity.class);
		InstrumentationRegistry.getTargetContext().deleteDatabase(TEST_DATABASE);
		db = new TestDbHelper(InstrumentationRegistry.getTargetContext(), cupboard, TEST_DATABASE).getWritableDatabase();
		rxDatabase = RxCupboard.with(cupboard, db);

		singleTime = System.currentTimeMillis();
		isDefaultTestEntity = new Predicate<TestEntity>() {
			@Override
			public boolean test(TestEntity getEntity) throws Exception {
				return getEntity._id != null &&
						getEntity.string.equals("Test") &&
						getEntity.time == singleTime;
			}
		};
	}

	@Test
	public void testSimplePutCountGetDelete() {

		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = singleTime;

		// Simple put of new item
		assertNull(testEntity._id);
		rxDatabase.putDirect(testEntity);

		// Get returns correct item
		rxDatabase.get(TestEntity.class, testEntity._id)
				.test()
				.assertValue(isDefaultTestEntity);

		// Count
		rxDatabase.count(TestEntity.class)
				.test()
				.assertValue(1L);

		// Simple put to update an item
		long updatedTime = System.currentTimeMillis();
		testEntity.string = "Updated";
		testEntity.time = updatedTime;
		rxDatabase.putDirect(testEntity);
		assertNotNull(testEntity._id);
		assertEquals(testEntity.string, "Updated");
		assertNotSame(testEntity.time, updatedTime);

		// Simple delete
		boolean deleted = rxDatabase.deleteDirect(testEntity);
		assertEquals(true, deleted);

		rxDatabase.count(TestEntity.class)
				.test()
				.assertTerminated()
				.assertValue(0L);

		rxDatabase.get(TestEntity.class, testEntity._id)
				.test()
				.assertTerminated()
				.assertNoValues();

		// Non-existing delete
		boolean missingDeleted = rxDatabase.deleteDirect(testEntity);
		assertEquals(false, missingDeleted);

	}

	@Test
	public void testSimplePutUpdateIds() {

		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Test";
		testEntity.time = System.currentTimeMillis();

		// Empty id should create (and return) the db-assigned one
		assertNull(testEntity._id);
		long assigned = rxDatabase.putDirect(testEntity);
		assertNotNull(testEntity._id);
		assertEquals((long) testEntity._id, assigned);

		// Updates to the id should be returned
		testEntity._id = 123456L;
		long updated = rxDatabase.putDirect(testEntity);
		assertEquals((long) testEntity._id, 123456L);
		assertEquals(updated, 123456L);

	}

	@Test
	public void testPutDelete() {

		final long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Action";
		testEntity.time = time;

		// Observe put and use the object
		assertNull(testEntity._id);
		rxDatabase.put(testEntity)
				.test()
				.assertTerminated()
				.assertValue(testEntity)
				.assertValue(isDefaultTestEntity);

		rxDatabase.count(TestEntity.class)
				.test()
				.assertValue(1L);

		// Observe deleted object
		rxDatabase.delete(testEntity)
				.test()
				.assertTerminated()
				.assertValue(testEntity)
				.assertValue(isDefaultTestEntity);

		rxDatabase.count(TestEntity.class)
				.test()
				.assertValue(0L);

		rxDatabase.get(TestEntity.class, testEntity._id)
				.test()
				.assertTerminated()
				.assertNoValues();

	}

	@Test
	public void testActionPutDelete() {

		final long time = System.currentTimeMillis();
		final TestEntity testEntity = new TestEntity();
		testEntity.string = "Action";
		testEntity.time = time;

		// Put as side effect
		assertNull(testEntity._id);
		Single.just(testEntity).doOnSuccess(rxDatabase.<TestEntity>put())
				.test()
				.assertTerminated()
				.assertValue(testEntity)
				.assertValue(isDefaultTestEntity);

		rxDatabase.count(TestEntity.class)
				.test()
				.assertValue(1L);

		// Delete as side effect
		rxDatabase.get(TestEntity.class, testEntity._id)
				.doOnSuccess(rxDatabase.<TestEntity>delete())
				.test()
				.assertTerminated()
				.assertValue(testEntity)
				.assertValue(isDefaultTestEntity);

		rxDatabase.count(TestEntity.class)
				.test()
				.assertValue(0L);

		rxDatabase.get(TestEntity.class, testEntity._id)
				.test()
				.assertTerminated()
				.assertNoValues();

	}

	@Test
	public void testMultiPutGetDelete() {

		Flowable<Integer> range = Flowable.range(1, 10);
		Flowable<TestEntity> rangeAsEntities = range.map(new Function<Integer, TestEntity>() {
			@Override
			public TestEntity apply(Integer integer) throws Exception {
				TestEntity e = new TestEntity();
				e._id = integer.longValue();
				e.string = "Multi";
				e.time = integer;
				return e;
			}
		});

		// Generate 10 items, with explicit IDs set [1..10]
		rangeAsEntities
				.doOnNext(rxDatabase.<TestEntity>put())
				.test()
				.assertValueCount(10);

		// Get the items with assigned ids 1 to 10
		Flowable.zip(
				range,
				range.flatMapSingle(new Function<Integer, SingleSource<TestEntity>>() {
					@Override
					public SingleSource<TestEntity> apply(Integer id) throws Exception {
						return rxDatabase.get(TestEntity.class, id.longValue());
					}
				}),
				new BiFunction<Integer, TestEntity, Pair<Integer, TestEntity>>() {
					@Override
					public Pair<Integer, TestEntity> apply(Integer id, TestEntity testEntity) throws Exception {
						return Pair.create(id, testEntity);
					}
				}
		).test()
				.assertValue(new Predicate<Pair<Integer, TestEntity>>() {
					@Override
					public boolean test(Pair<Integer, TestEntity> pair) throws Exception {
						return pair.second._id.intValue() == pair.first &&
								pair.second.string.equals("Multi") &&
								pair.second.time == pair.first;
					}
				});

		// Delete the items with assigned ids 1 to 10
		range.flatMapSingle(new Function<Integer, Single<Boolean>>() {
			@Override
			public Single<Boolean> apply(Integer id) throws Exception {
				return rxDatabase.delete(TestEntity.class, id);
			}
		})
				.test()
				.assertValueCount(10);

		// Put 10 new items to delete
		rangeAsEntities
				.doOnNext(rxDatabase.<TestEntity>put())
				.test()
				.assertValueCount(10);

		// Now delete all 10 items by their object
		range.flatMapSingle(new Function<Integer, Single<TestEntity>>() {
			@Override
			public Single<TestEntity> apply(Integer id) throws Exception {
				return rxDatabase.get(TestEntity.class, id);
			}
		})
				.doOnNext(rxDatabase.<TestEntity>delete())
				.test()
				.assertValueCount(10);

		// Put 10 new items to delete
		rangeAsEntities
				.doOnNext(rxDatabase.<TestEntity>put())
				.test()
				.assertValueCount(10);

		// Now delete all 10 items at once
		rxDatabase.deleteAll(TestEntity.class)
				.test()
				.assertValue(10L);
	}

	@After
	public void tearDown() throws Exception {
		db.close();
	}

}
