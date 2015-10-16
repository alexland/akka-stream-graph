


def stepFn(q:Int):Int = {
	q match {
		case 1 => 5
		case 2 => 10
		case 3 => 15
		case _ => 0
	}
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

val src = Source(Seq.fill(5)(RND.nextInt(100)).toList)
src
	.via(F1)
	.runWith(Sink.foreach(println(_))))

val res = src.via(F1).runWith(Sink.fold[Int,Int](0)(_ + _))
res.value

val f1:Flow[Int,Int,Unit] = {
Flow[Int].filter(_ > 2)
}

val t1:Flow[Int,Int,Unit] = {
Flow[Int].map(stepFn)
}

val res = src
.via(F1)
.via(f1)
.via(t1)
.runWith(Sink.fold[Int,Int](0)(_ + _))
