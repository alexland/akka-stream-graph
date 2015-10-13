

package org.dougybarbo

import scala.util.{
	Random => RND
}
import scala.concurrent.{
	ExecutionContext,
	Future
}
import scala.collection.immutable
import scala.io.{
	Source => ioSource
}
import scala.util.{
	Failure,
	Success,
	Try
}

import akka.actor.ActorSystem
import akka.stream.{
	ActorMaterializer
}
import akka.stream.io.{
	Framing,
	InputStreamSource
}
import akka.stream.scaladsl._

import akka.util.ByteString
import org.json4s.JsonAST.JString
import org.json4s.jackson.JsonMethods._


object Main {

	def main(args: Array[String]): Unit = {

		trait Record {
			def c1: Int
			def c2: Int
		}

		final case class RawDataLine(
			idx:Int,
			c1:Int,
			c2:Int,
			c3:Int
		) extends Record

		final case class RawDataLineSm(
			idx: Int,
			c1: Int
		) extends Record

		val fnx = () => for (i <- (1 to 4)) yield RND.nextInt(100)
		val fnx0 = () => for (i <- (1 to 2)) yield RND.nextInt(100)

		val fnx1 = (q:Vector[Int]) => q match {
			case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
			case Vector(a, b) => RawDataLineSm(a, b)
		}

		val fnx2 = (q:Vector[Int]) => q match {
			case Vector(a, b) => RawDataLineSm(a, b)
		}

		val rawDataIn: Source[RawDataLine, Unit] = Source(
			(1 to 1000000)
				.map(_ => fnx().toVector)
				.map(c => fnx1(c))
		)

		val rawDataIn0: Source[RawDataLineSm, Unit] = Source(
			(1 to 5)
				.map(_ => fnx0().toVector)
				.map(c => fnx2(c))
		)

		def twoX(r:Record):Record = {
			r match {
				case RawDataLineSm(a, b) => RawDataLineSm(a*2, b*2)
				case RawDataLine(a, b, c, d) => RawDataLine(a*2, b*2, c*2, d*2)
			}
		}

		def twoX(r:RawDataLineSm):RawDataLineSm = {
			r match {
				case RawDataLineSm(a, b) => RawDataLineSm(a*2, b*2)
			}
		}

		def rowSum(r:RawDataLineSm):Int = {
			r match {
				case RawDataLineSm(a, b) => a + b
			}
		}


		// commify printed integer values
		val formatter = java.text.NumberFormat.getIntegerInstance
		val fmt = (v:Int) => formatter.format(v)

		implicit val actorSystem = ActorSystem("entity-resolver")
		import actorSystem.dispatcher
		implicit val flowMaterializer = ActorMaterializer()

		/**
		*	'count': a reusable Flow that transforms each item in the stream into a
		*	a '1'
		*	'counter', the Sink, can fold over it to get a count of items in the stream
		*/
		val count: Flow[RawDataLine, Int, Unit] = Flow[RawDataLine].map(_ => 1)
		val counter: Sink[ Int, Future[Int] ] = Sink.fold[Int, Int](0)(_ + _)

		val toVec = (q:RawDataLine) => q match {
			case RawDataLine(a, b, c, d) => Vector(a, b, c, d)
		}

		/**
		*	transformation functions passed to higher-order
		*	functions in the graph
		*	variables named t followed by integer are anonymous
		*	functions that transform individual data lines
		*/
		val t1 = (v:Vector[Int]) => v.map(_ / 2)
		val t2 = (v:Vector[Int]) => v.filter(_ <= 25)

		val lineSum1: Flow[RawDataLine, Int, Unit] = Flow[RawDataLine]
			.map(c => toVec(c).sum)

		val lineSum2: Flow[Vector[Int], Int, Unit] = Flow[Vector[Int]]
   			.map(c => c.sum)

		val lineDiv: Flow[RawDataLine, Vector[Int], Unit] = Flow[RawDataLine]
			.map(c => toVec(c))
			.map(t1(_))

		val lineFilter1: Flow[Vector[Int], Vector[Int], Unit] = Flow[Vector[Int]]
			.map(t2(_))

		val streamSum: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

		val res = (g:RunnableGraph[Future[Int]]) => g.run()

		/**
		*	4 RunnableGraph instances, all are
		*	immutable, thread-safe, and freely shareable;
		*
		*/
		val countGraph: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(count)
				.toMat(counter)(Keep.right)

		res(countGraph)
			.foreach( c => println( s"total lines processed per graph: " + fmt(c) ) )

		val g1: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineSum1)
				.toMat(streamSum)(Keep.right)
		res(g1)
			.foreach( c => println( "sum from g1: " + fmt(c) ) )


		val g2: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineDiv)
				.via(lineSum2)
				.toMat(streamSum)(Keep.right)
		res(g2)
			.foreach( c => println( "sum from g2: " + fmt(c) ) )


		val g3: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineDiv)
				.via(lineFilter1)
				.via(lineSum2)
				.toMat(streamSum)(Keep.right)
		res(g3)
			.foreach( c => println(s"sum from g3: "+ fmt(c) ) )

	// DAG examples

		val c1:Flow[RawDataLine, Int, Unit] = Flow[RawDataLine].map(_ => 1)
		val c2:Flow[Int, Int, Unit] = Flow[Int].map(_ * 5)

		val s1: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)
		val s2: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

		val g4 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			val dataOut:Sink[Any, Future[Unit]] = Sink.foreach(println(_))
			rawDataIn ~> c1 ~> c2 ~> dataOut
		}

		g4.run()

		val g5 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			val bcast = builder.add(Broadcast[Int](2))
			val c1:Flow[RawDataLine, Int, Unit] = Flow[RawDataLine].map(_ => 1)
			val c2:Flow[Int, Int, Unit] = Flow[Int].map(_ * 2)
			val c3:Flow[Int, Int, Unit] = Flow[Int].map(_ * 5)
			val dataOut1:Sink[Any, Future[Unit]] = Sink.foreach(println(_))
			val dataOut2:Sink[Any, Future[Unit]] = Sink.foreach(println(_))
			rawDataIn ~> c1 ~> bcast ~> c2 ~> dataOut1
			bcast ~> c3 ~> dataOut2
		}

		g5.run()

		val g6 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			// val bcast = builder.add(Broadcast[Int](2))
			// val merge = builder.add(Merge[Int](2))
			val c0:Flow[RawDataLineSm, RawDataLineSm, Unit] = {
				Flow[RawDataLineSm].map(twoX(_))
			}
			val c1:Flow[RawDataLineSm, Int, Unit] = Flow[RawDataLineSm].map(rowSum(_))
			val dataOut:Sink[Any, Future[Unit]] = Sink.foreach(println(_))
			rawDataIn0 ~> c0 ~> c1 ~> dataOut
		}

		g6.run()


	}
}
