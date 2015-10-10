

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

		final case class RawDataLine(idx:Int, col1:Int, col2:Int, col3:Int)

		val fnx = () => for (i <- (1 to 4)) yield RND.nextInt(100)

		val fnx1 = (q:Vector[Int]) => q match {
			case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
		}

		val rawDataIn: Source[RawDataLine, Unit] = Source(
			(1 to 1000000)
				.map(_ => fnx().toVector)
				.map(c => fnx1(c))
		)

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

	}

}
