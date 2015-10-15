

package org.dougybarbo

import java.text.NumberFormat.{
	getIntegerInstance => gII
}

import scala.util.{
	Random => RND
}

import scala.concurrent.duration._

import scala.concurrent.{
	ExecutionContext,
	Future
}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
		*	RunnableGraph instances are all:
		*		immutable,
		*		thread-safe
		*		freely shareable
		*
		*/
		implicit val actorSystem = ActorSystem("streamGraphs")
		import actorSystem.dispatcher
		implicit val flowMaterializer = ActorMaterializer()

		val res = (g:RunnableGraph[Future[Int]]) => g.run()

		val rawDataIn:Source[RawDataLine, Unit] = Source(rawData)
		val rawDataIn0:Source[RawDataLineSm, Unit] = Source(rawData0)

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

		val g4 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			val c1:Flow[RawDataLine, Int, Unit] = Flow[RawDataLine].map(_ => 1)
			val c2:Flow[Int, Int, Unit] = Flow[Int].map(_ * 5)
			val dataOut:Sink[Any, Future[Unit]] = Sink.foreach(println(_))
			rawDataIn ~> c1 ~> c2 ~> dataOut
		}

		println("from stream graph 4: ")
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

		println("from from stream graph 5: ")
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

		println("from stream graph 6: ")
		g6.run()

		val g7 = FlowGraph.closed() { implicit builder: FlowGraph.Builder[Unit] =>
			import FlowGraph.Implicits._
			val bcast = builder.add(Broadcast[RawDataLineSm](2))

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

		println("from stream graph 7: ")
		g7.run()

		/**
		*	constructing sources, sinks & flows from partial graphs;
		*	g8 is a Source created from a partial flow graph;
		*	Source exposes a special apply method that takes a fn
		*	which returns an Outlet[T]; this unconnected sink
		*	will become the sink that must be connected before
		*	this Source can run
		*	tupleSource is a member of class
		*	akka.stream.scaladsl.Source
		*/
		val tupleSource = Source() { implicit b =>
			import FlowGraph.Implicits._
			val zip = b.add(Zip[Int, Int]())
			val d = Seq.fill(25)(RND.nextInt(100)).toList
			def src = Source(s)
			// connect the graph
			src.filter(_ % 2 != 0) ~> zip.in0
			src.filter(_ % 2 == 0) ~> zip.in1
			// expose port
			zip.out
		}

		val firstPair:Future[(Int, Int)] = tupleSource
			.runWith(Sink.head)
			.value		// Option[Try[(Int, Int)]] = Some(Success((55,14)))

		/**
		* tupleSource is a Source so call its runWith
		*	method with a single parameter, a Sink
		*/
		val allPairs:Future[Unit] = tupleSource
			.runWith(Sink.foreach(println(_)))

		/**
		*	same as tupleSource above but a Flow is defined
		*	instead of a Source
		*/
		val tupleFlow = Flow() { implicit b =>
			import FlowGraph.Implicits._
			// create the graph elements
			val bcast = b.add(Broadcast[Int](2))
			val zmerge = b.add(Zip[Int, String]())
			// connect the elements
			bcast.out(0).map(identity) ~> zmerge.in0
			bcast.out(1).map(_.toString) ~> zmerge.in1
			// expose the ports
			(bcast.in, zmerge.out)
		}

		// define a source
		val d = Seq.fill(25)(RND.nextInt(100)).toList
		def src = Source(s)

		/**
		*	tupleFlow is a Flow so its runWith method
		* must be called with two paramaters
		*	a Source & a Sink
		*/
		tupleFlow
			.runWith(src, Sink.foreach(println(_)))
		/**
		* alternate way to create the same graph
		*/
		src
			.via(tupleFlow)
			.runWith(Sink.foreach(println(_)))






	}
}
