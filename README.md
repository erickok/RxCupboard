RxCupboard
============
RxCupboard brings the excellent Android [Cupboard](https://bitbucket.org/qbusict/cupboard) library into the world of [RxJava](https://github.com/ReactiveX/RxJava). Using a small set of convenience `Observable`s, you can fluently store and retrieve streams of POJOs from your database.

Usage
------------
Start off by creating an `RxCupboard` instance using the default cupboard() or pass a Cupboard instance to:
```java
RxCupboard rxCupboard = RxCupboard.with(cupboard, database);
```

Querying your Cupboard-connected database results in a stream of the desired items.

```java
rxCupboard.query(Cheese.class, "agedMonths >= ", 12).subscribe(new Action1<Cheese>() {
    @Override public void call(Cheese cheese) {
        // Do something with item...
    }
});
```

There is `query(Class<?>)` to load all objects form the table or use a simple WHERE selection with `query(Class<?>), selection, args` or construct a query using Cupboard's query builder and `query(DatabaseCompartment.QueryBuilder<T>)`. RxCupboard support reactive pull. For example, using take(5) only 5 items are actually converted from the underlying Cursor to a POJO:

```java
rxCupboard.query(Cheese.class).take(5).subscribe(new Action1<Cheese>() {
    @Override public void call(Cheese item) {
        // Do something with item...
    }
});
```

Storing and removing items in the databse is as easy as usually with Cupboard. RxCupboard support the direct operations and has helper action implementations to use in streams.

```java
Cheese saintMaure = new Cheese("Saint-Maure", 1, "Goat milk");
rxCupboard.put(saintMaure); // Insert or update
rxCupboard.delete(saintMaure); // Delete

Observable.just(3, 6, 12, 24).map(new Func1<Integer, Cheese>() {
    @Override public Cheese call(Integer agedMonths) {
        return new Cheese("Gouda", agedMonths, "Cow milk");
    }
}).subscribe(rxCupboard.put());
```

Finally, the connected database or a specific table can be monitored for changes using an Observable that reports inserts, updates and deletes.
```java
rxCupboard.changes(Cheese.class).subscribe(new Action1<DatabaseChange<Cheese>>() {
    @Override
    public void call(DatabaseChange<Cheese> databaseChange) {
        Cheese changedCheese = databaseChange.entity();
        // Do something with the changed item...
    }
});
Cheese pecorino = new Cheese("Pecorino", 3, "Sheep milk");
rxCupboard.put(pecorino); // Causes a DatabaseInsert change
pecorino.agedMonths = 6;
rxCupboard.put(pecorino); // Causes a DatabaseUpdate change
rxCupboard.delete(pecorino); // Causes a DatabaseDelete change
```
To ensure that changes are properly monitored and published it is necessary to only perform operations through the same `RxCupboard` instance.

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
