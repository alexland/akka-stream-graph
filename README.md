

to build:
```bash
    cd <top-level/project/path>
    sbt clean assembly
```

```bash
	cd <top-level/project/path>
	java -jar target/scala-2.11/StreamGraphs.jar
```

should give you this output in your shell:

```bash
total lines processed per graph: 1,000,000
sum from g1: 198,005,741
sum from g2: 98,002,857
sum from g3: 25,990,327
```


or you can run these graphs from the scala REPL:
```bash
    sbt clean assembly
    sbt console
```
```scala
    import org.dougybarbo
    import akka.stream.scaladsl._
```
