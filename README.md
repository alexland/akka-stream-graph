

computation graph:


to run:

```Scala
	cd <top-level/project/path>
	java -jar target/scala-2.11/EtlGraph.jar
```


from the scala REPL:

```Scala
    sbt clean assembly
    sbt console
```

```Scala
    import org.dougybarbo
    import akka.stream.scaladsl._
```
