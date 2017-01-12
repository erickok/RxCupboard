RxCupboard
============
RxCupboard brings the excellent Android [Cupboard](https://bitbucket.org/littlerobots/cupboard) library into the world of [RxJava](https://github.com/ReactiveX/RxJava). Using a small set of convenience `Flowable`s, you can fluently store and retrieve streams of POJOs from your database.

A sample project is included which shows how RxCupboard gracefully fits in an all-reactive Android app. The [.apk is available](https://github.com/erickok/RxCupboard/releases) as direct download. Import the library from Maven Central using Gradle:
```groovy
compile 'nl.2312:rxcupboard2:2.0'
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
rxDatabase.query(Cheese.class, "agedMonths >= ?", "12").subscribe(cheese -> {
	// Do something with cheese...
});
```

There is `query(Class<?>)` to load all objects form the table or use a simple WHERE selection with `query(Class<?>), selection, args`. RxCupboard support reactive pull. For example, using take(5) only 5 items are actually converted from the underlying Cursor to a POJO:

```java
rxDatabase.query(Cheese.class).take(5).subscribe(cheese -> {
	// Do something with cheese...
});
```

For more complex queries, use `buildQuery(Class<?>)` to use Cupboard's query builder and then call `query(DatabaseCompartment.QueryBuilder<T>)`:

```java
rxDatabase.query(rxDatabase.buildQuery(Cheese.class)
		.withSelection("agedMonths >= ?", "12")
		.orderBy("name")).toList()
		.subscribe(cheeses -> {
			// Do something with this ordered list of aged cheeses...
		});
```

Storing and removing items in the databse is as easy as usually with Cupboard. RxCupboard provides put or delete as stream functions or consumers. Deprecated direct functions are still available for convenience.

```java
Flowable.just(3, 6, 12, 24)
		.map(agedMonths -> new Cheese("Gouda", agedMonths, "Cow milk"))
		.flatMapSingle(cheese -> rxDatabase.put(cheese))
		.subscribe(rxDatabase.put());

Cheese saintMaure = new Cheese("Saint-Maure", 1, "Goat milk");
rxDatabase.putDirect(saintMaure); // Insert or update
rxDatabase.deleteDirect(saintMaure); // Delete
```

Finally, the connected database or a specific table can be monitored for changes using an Observable that reports inserts, updates and deletes.
```java
rxDatabase.changes(Cheese.class).subscribe(databaseChange -> {
	Cheese changedCheese = databaseChange.entity();
	// Do something with changedCheese...
});
Cheese pecorino = new Cheese("Pecorino", 3, "Sheep milk");
rxDatabase.put(pecorino); // Causes a DatabaseInsert change
pecorino.agedMonths = 6;
rxDatabase.putDirect(pecorino); // Causes a DatabaseUpdate change
rxDatabase.deleteDirect((pecorino); // Causes a DatabaseDelete change
```
There are also `inserts()`, `updates()` and `deletes()` flowables that receive only those database changes.

*Important:* To ensure that changes are properly monitored and published it is necessary to only perform operations through the same `RxCupboard` instance.

Usage with ContentProvider and Cursor
-------------------------------------
Cupboard also supports object persistance straight from a `Cursor` or `ContentProvider`. Cursors can only be queried.
```java
Cursor cursor = ...
RxCursor rxCursor = RxCupboard.with(cursor);
rxCursor.iterate(Cheese.class).subscribe(cheese -> {
	// Do something with cheese...
});
```

Content providers can be queried, inserted and deleted via the usual Cupboard way where the provider is assumed to act as REST.
```java
RxContentProvider rxContentProvider = RxCupboard.with(cupboard, getContext(),
        ContactsContract.Contacts.CONTENT_URI);
rxContentProvider.query(Contact.class).subscribe(contact -> {
	// Do something with contact...
});
```

RxJava 1
--------
If you are still using RxJava 1, you may use the last `1.x` branch release (unfortunadly named 0.7):

```groovy
compile 'nl.2312:rxcupboard:0.7'
```

Although the API has remained mostly unchanged, you may best take a look at the [1.x branch README](https://github.com/erickok/RxCupboard/blob/1.x/README.md) for usage instructions. Notably the direct (non-observable) put/delete methods have been deprecated in `2.x`.

Contributing
------------
Feel free to improve the code and send me pull requests! Feature request issues are also welcome.

Similar projects
----------------

Several other projects exist that also try to couple Rx and SQLite on Android. As both Cupboard and RxCupboard are (and always will be) quite simple, these projects may better suit your needs:

- [GreenDAO](https://github.com/greenrobot/greenDAO) has experimetal Rx support since 3.1.0
- [SquiDB](https://github.com/yahoo/squidb) is a full-featured Rx-compatible SQLite database layer with query builders, change notifications, transactions and more
- [StorIO](https://github.com/pushtorefresh/storio) has a fluent API and Observable support for SQLiteDatabase, with manual Object mapping and database changes
- [SQLBrite](https://github.com/square/sqlbrite) is a very light Observable wrapper around SQLiteOpenHelper and could perhaps grow into an ORM solution

License
-------
Designed and developed by [Eric Kok](mailto:eric@2312.nl) of [2312 development](http://2312.nl).

    Copyright 2014-2016 Eric Kok
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
