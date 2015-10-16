

object StreamGraph {


	/**
	*	https://gist.github.com/dotta/78e8a4a72c5d2de07116
	*	FlowGraph.partial() returns a Graph
	*	FlowGraph.closed() returns a RunnableGraph
	*	Graph does not require all of its ports to be connected
	*/

	val maxOf3 = FlowGraph.partial() { implicit b =>
		import FlowGraph.Implicits._
		val zip1 = b.add(ZipWith[Int,Int,Int]((u, v) => if (u > v) u else v))
		val zip2 = b.add(ZipWith[Int,Int,Int]((u, v) => if (u > v) u else v))
		zip1.out ~> zip2.in0
		// this partial graph will have 3 inputs & 1 output
		UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
	}



	/**
	*	to execute, now build a closed graph (RunnableGraph) &
	*	import the partial graph into this closed graph
	* using b.add()
	*/
	val resSink = Sink.head[Int]
	val g = FlowGraph.closed(resSink) { implicit b =>
		sink =>
		import FlowGraph.Implicits._
		val sg = b.add(maxOf3)
		Source.single(1) ~> sg.in(0)
		Source.single(2) ~> sg.in(1)
		Source.single(3) ~> sg.in(2)
		sg.out ~> sink.inlet
	}

	// materialize the graph
	val mx:Future[Int] = g.run()
	val res = Await.result(mx, 300.millis)
	println(res)

	// Await.result(max, 300.millis) should equal(3)

}
