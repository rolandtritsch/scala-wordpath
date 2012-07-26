Find a solution to go from one 4-letter word to another, changing one letter at a time.

Build it
--------

Use macports to install sbt (port install sbt (at least 0.11.2)).
Run >sbt compile one-jar<

Run it
------

You might need to increase the max stack size with >ulimit -s 32000<

Run >java -Xmx4096m -Xss1024m -jar target/scala-2.9.1/wordpath_2.9.1-0.1-one-jar.jar log/large.pairs /usr/share/dict/words 7<	