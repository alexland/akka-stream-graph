

computation graph:

to build:

```bash
    %> cd <top-level/project/path>
    %> sbt clean assembly
```

```bash
	cd <top-level/project/path>
	java -jar target/scala-2.11/EtlGraph.jar
```


from the scala REPL:

```bash
    sbt clean assembly
    sbt console
```

```Scala
    import org.dougybarbo
    import akka.stream.scaladsl._
```
