

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

import org.dougybarbo.ETL._


object Main {

	def main(args: Array[String]): Unit = {
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

		val g7 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			val bcast = builder.add(Broadcast[RawDataLineSm](2))
			// val merge = builder.add(Merge[Int](2))
			val c0:Flow[RawDataLineSm, RawDataLineSm, Unit] = {
				Flow[RawDataLineSm].map(twoX(_))
			}
			val c1:Flow[RawDataLineSm,Int,Unit] = Flow[RawDataLineSm]
				.map(rowSum(_))
			val c2:Flow[RawDataLineSm,Int,Unit] = Flow[RawDataLineSm]
				.map(rowMul(_))
			val dataOut1:Sink[Any,Future[Unit]] = Sink.foreach(println(_))
			val dataOut2:Sink[Any,Future[Unit]] = Sink.foreach(println(_))

			rawDataIn0 ~> c0 ~> bcast ~> c1 ~> dataOut1
													bcast ~> c2 ~> dataOut2
		}

		g7.run()

		// val g8 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
		// 	import FlowGraph.Implicits._
		// 	val bcast = builder.add(Broadcast[RawDataLineSm](2))
		// 	val merge = builder.add(Zip[RawDataLineSm,RawDataLineSm])
		//
		// 	val c0:Flow[RawDataLineSm, RawDataLineSm, Unit] = {
		// 		Flow[RawDataLineSm].map(twoX(_))
		// 	}
		// 	val c1:Flow[RawDataLineSm,RawDataLineSm,Unit] = Flow[RawDataLineSm]
		// 		.map(oneFifthX(_))
		// 	val c2:Flow[RawDataLineSm,RawDataLineSm,Unit] = Flow[RawDataLineSm]
		// 		.map(twoX(_))
		// 	// c3 concatanates 2 small records into a single big record
		// 	val c3:Flow[RawDataLineSm,RawDataLine,Unit] = Flow[RawDataLineSm]
		// 		.map(f1(_))
		// 	// c4 row-wise sum of a big record
		// 	val c4:Flow[RawDataLine,Int,Unit] = Flow[RawDataLine]
		// 		.map(rowSum1(_))
		// 	val dataOut:Sink[Any,Future[Unit]] = Sink.foreach(println(_))
		// 	rawDataIn0 ~> c0 ~> bcast ~> c1 ~> merge ~> c3 ~> c4 ~> dataOut
		// 											bcast ~> c2 ~> merge
		// }
		//
		// g8.run()









	}
}
