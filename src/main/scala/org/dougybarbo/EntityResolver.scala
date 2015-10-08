

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

		final case class RawDataLine(idx:Int, col1:Int, col2:Int, col3:Int) {
			def sumLine()
		}

		val fnx = () => for (i <- (1 to 4)) yield RND.nextInt(100)

		val fnx1 = (q:Vector[Int]) => q match {
			case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
		}

		val rawDataIn: Source[RawDataLine, Unit] = Source(
			(1 to 100)
				.map(_ => fnx().toVector)
				.map(c => fnx1(c))
		)

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

		val fnx1 = (v:Vector[Int]) => v.map(_ / 2)

		val fnx2 = (v:Vector[Int]) => v.filter(_ >= 25)

		val lineSum1: Flow[RawDataLine, Int, Unit] = Flow[RawDataLine]
			.map(c => toVec(c).sum)

		val lineSum2: Flow[Vector[Int], Int, Unit] = Flow[Vector[Int]]
   			.map(c => c.sum)
   		}

		val lineDiv: Flow[RawDataLine, Vector[Int], Unit] = Flow[RawDataLine]
			.map(c => toVec(c))
			.map(fnx1(_))

		val lineFilter1: Flow[Vector[Int], Vector[Int], Unit] = Flow[Vector[Int]]
			.map(fnx2(_))

		val streamSum: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

		/**
		*	RunnableGraph instance is immutable, thread-safe, and freely shareable;
		*	'Broadcast' will propagate back-pressure to its upstream element
		*/
		val countGraph: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(count)
				.toMat(counter)(Keep.right)

		val res: Future[Int] = g1.run()
		res.foreach(c => println(s"total lines processed: $c"))


		val g1: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineSum1)
				.toMat(streamSum)(Keep.right)

		val res: Future[Int] = g1.run()
		res.foreach(c => println(s"stream sum is: $c"))			// 1842


		val g2: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineDiv)
				.via(lineSum2)
				.toMat(streamSum)(Keep.right)

		val res: Future[Int] = g2.run()
		res.foreach(c => println(s"stream sum is: $c"))			// 911

		val g3: RunnableGraph[Future[Int]] =
			rawDataIn
				.via(lineDiv)
				.via(lineFilter1)
				.via(lineSum2)
				.toMat(streamSum)(Keep.right)

		val res: Future[Int] = g3.run()
		res.foreach(c => println(s"stream sum is: $c"))				// 655

	}


}











