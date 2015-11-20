RxCupboard
============
RxCupboard brings the excellent Android [Cupboard](https://bitbucket.org/littlerobots/cupboard) library into the world of [RxJava](https://github.com/ReactiveX/RxJava). Using a small set of convenience `Observable`s, you can fluently store and retrieve streams of POJOs from your database.

A sample project is included which shows how RxCupboard gracefully fits in an all-reactive Android app. The [.apk is available]() as direct download. Import the library from Maven Central using Gradle:
```groovy
compile 'nl.2312:rxcupboard:0.5'
```

Usage with database
-------------------
Start off by creating an `RxDatabase` instance using `withDefault(SQLiteDatabase)` or pass a Cupboard instance to:
```java
SQLiteDatabase db = ...
RxDatabase rxDatabase = RxCupboard.with(cupboard, db);
```

Querying your Cupboard-connected database results in a stream of the desired items.

```java
rxDatabase.query(Cheese.class, "agedMonths >= ", 12).subscribe(new Action1<Cheese>() {
    @Override public void call(Cheese cheese) {
        // Do something with cheese...
    }
});
```

There is `query(Class<?>)` to load all objects form the table or use a simple WHERE selection with `query(Class<?>), selection, args`. RxCupboard support reactive pull. For example, using take(5) only 5 items are actually converted from the underlying Cursor to a POJO:

```java
rxDatabase.query(Cheese.class).take(5).subscribe(new Action1<Cheese>() {
    @Override public void call(Cheese cheese) {
        // Do something with cheese...
    }
});

For more complex queries, use `buildQuery(Class<?>)` to use Cupboard's query builder and then call `query(DatabaseCompartment.QueryBuilder<T>)`:

rxDatabase.query(rxDatabase.buildQuery(Cheese.class).withSelection("agedMonths >= 12").orderBy("name")).toList().subscribe(new Action1<Cheese>() {
    @Override public void call(List<Cheese> cheeses) {
        // Do something with this ordered list of aged cheeses...
    }
});
```

Storing and removing items in the databse is as easy as usually with Cupboard. RxCupboard support the direct operations and has helper action implementations to use in streams.

```java
Cheese saintMaure = new Cheese("Saint-Maure", 1, "Goat milk");
rxDatabase.put(saintMaure); // Insert or update
rxDatabase.delete(saintMaure); // Delete

Observable.just(3, 6, 12, 24).map(new Func1<Integer, Cheese>() {
    @Override public Cheese call(Integer agedMonths) {
        return new Cheese("Gouda", agedMonths, "Cow milk");
    }
}).subscribe(rxDatabase.put());
```

Finally, the connected database or a specific table can be monitored for changes using an Observable that reports inserts, updates and deletes.
```java
rxDatabase.changes(Cheese.class).subscribe(new Action1<DatabaseChange<Cheese>>() {
    @Override
    public void call(DatabaseChange<Cheese> databaseChange) {
        Cheese changedCheese = databaseChange.entity();
        // Do something with changedCheese...
    }
});
Cheese pecorino = new Cheese("Pecorino", 3, "Sheep milk");
rxDatabase.put(pecorino); // Causes a DatabaseInsert change
pecorino.agedMonths = 6;
rxDatabase.put(pecorino); // Causes a DatabaseUpdate change
rxDatabase.delete(pecorino); // Causes a DatabaseDelete change
```
To ensure that changes are properly monitored and published it is necessary to only perform operations through the same `RxCupboard` instance.


Usage with ContentProvider and Cursor
-------------------------------------
Cupboard also supports object persistance straight from a `Cursor` or `ContentProvider`. Cursors can only be queried.
```java
Cursor cursor = ...
RxCursor rxCursor = RxCupboard.with(cursor);
rxCursor.iterate(Cheese.class).subscribe(new Action1<TestEntity>() {
	@Override public void call(Cheese cheese) {
        // Do something with cheese...
    }
});
```

Content providers can be queried, inserted and deleted via the usual Cupboard way where the provider is assumed to act as REST.
```java
RxContentProvider rxContentProvider = RxCupboard.with(cupboard, getContext(),
        ContactsContract.Contacts.CONTENT_URI);
rxContentProvider.query(Contact.class).subscribe(new Action1<Integer>() {
	@Override public void call(Contact contact) {
		// Do something with contact...
	}
});
```

Contributing
------------
Feel free to improve the code and send me pull requests! Feature request issues are also welcome.

Similar projects
----------------

Several other projects exist that also try to couple Rx and SQLite on Android. As both Cupboard and RxCupboard are (and always will be) quite simple, these projects may better suit your needs:

- [StorIO](https://github.com/pushtorefresh/storio) has a fluent API and Observable support for SQLiteDatabase, with manual Object mapping and database changes
- [SQLBrite](https://github.com/square/sqlbrite) is a very light Observable wrapper around SQLiteOpenHelper and could perhaps grow into an ORM solution

License
-------
Designed and developed by [Eric Kok](mailto:eric@2312.nl) of [2312 development](http://2312.nl).

    Copyright 2014 Eric Kok
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
