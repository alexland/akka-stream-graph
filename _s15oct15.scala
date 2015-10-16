

object StreamGraph {

	def stepFn(q:Int):Int = {
		q match {
			case 1 => 5
			case 2 => 10
			case 3 => 15
			case _ => 0
		}
	}

	val dataIn = Seq.fill(5)(RND.nextInt(100))

	val f1:Flow[Int,Int,Unit] = {
		Flow[Int].filter(_ > 2)
	}

	val t1:Flow[Int,Int,Unit] = {
		Flow[Int].map(stepFn)
	}

	val F1 = Flow() { implicit b =>
		import FlowGraph.Implicits._
		val bc0 = b.add(Broadcast[Int](3))
		val mg0 = b.add(Merge[Int](3))
		bc0.out(0).map(_ => 1) ~> mg0
		bc0.out(1).map(_ => 2) ~> mg0
		bc0.out(2).map(_ => 3) ~> mg0
		(bc0.in, mg0.out)
	}

	def run(): Unit = {
		// implicit val system = ActorSystem("sg1")
		// implicit val mat = ActorMaterializer()
		Source(dataIn.toList)
			.via(F1)
			.via(f1)
			.via(t1)
			.runWith(Sink.foreach(println(_)))
			.onComplete {
				case _ => system.shutdown()
			}
	}
}


// --------------------------------------- //

/**
*	https://gist.github.com/dotta/78e8a4a72c5d2de07116
*	FlowGraph.partial() returns a Graph
*	FlowGraph.closed() returns a RunnableGraph
*	Graph does not require all of its ports to be connected
*/
val maxOf3 = FlowGraph.partial() { implicit b =>
	import FlowGraph.Implicits._

	val zip1 = b.add(ZipWith[Int,Int,Int](math.max _))
	val zip2 = b.add(ZipWith[Int,Int,Int](math.max _))

	zip1.out ~> zip2.in0
	// this partial graph will have 3 inputs & 1 output
	UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)

}

val res = Sink.head[Int]

/**
*	now build a closed graph which can be run
*	import the partial graph into this closed graph
* using b.add()
*/
val g = FlowGraph.closed(res) { implicit b =>
	sink =>
		import FlowGraph.Implicits._
		// partial graph imported into this closed (runnable) graph
		val sg = b.add(maxOf3)
		Source.single(1) ~> sg.in(0)
		Source.single(2) ~> sg.in(1)
		Source.single(3) ~> sg.in(2)
		sg.out ~> sink.inlet
}

val mx:Future[Int] = g.run()

Await.result(max, 300.millis) should equal(3)
