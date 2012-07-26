for i in {1..100}
do
java -jar target/scala-2.9.1/word-path-solution_2.9.1-0.1-one-jar.jar log/large/${i}.pairs /usr/share/dict/words true > log/large/${i}.pairs.all.log &
done