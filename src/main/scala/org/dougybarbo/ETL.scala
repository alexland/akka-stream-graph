
package org.dougybarbo

import util.{
	Random => RND
}
import scala.util.{
	Failure,
	Success,
	Try
}
import scala.concurrent.{
	ExecutionContext,
	Future
}
import scala.collection.immutable
import scala.io.{
	Source => ioSource
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


object ETL {

	trait Record {
		def c1: Int
		def c2: Int
	}

	final case class RawDataLine(
		c1:Int,
		c2:Int,
		c3:Int,
		c4:Int
	) extends Record

	final case class RawDataLineSm(
		c1: Int,
		c2: Int
	) extends Record

	val fnx = () => (for (i <- (1 to 4)) yield RND.nextInt(100))
		.toVector
	val fnx0 = () => (for (i <- (1 to 2)) yield RND.nextInt(100))
		.toVector

	// val fnx1 = (q:Vector[Int]) => q match {
	// 	case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
	// 	case Vector(a, b) => RawDataLineSm(a, b)
	// }

	val fnx1 = (q:Vector[Int]) => q match {
		case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
	}

	val fnx2 = (q:Vector[Int]) => q match {
		case Vector(a, b) => RawDataLineSm(a, b)
	}

	val rawData = (1 to 10)
			.map(_ => fnx())
			.map(c => fnx1(c))

	val rawData0 = (1 to 10)
			.map(_ => fnx0())
			.map(c => fnx2(c))

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

	def oneFifthX(r:RawDataLineSm):RawDataLineSm = {
		r match {
			case RawDataLineSm(a, b) => RawDataLineSm(a/5, b/5)
		}
	}

	def rowSum(r:RawDataLineSm):Int = {
		r match {
			case RawDataLineSm(a, b) => a + b
		}
	}

	def rowSum1(r:RawDataLine):Int = {
		r match {
			case RawDataLine(a, b, c, d) => a + b + c + d
		}
	}

	def rowMul(r:RawDataLineSm):Int = {
		r match {
			case RawDataLineSm(a, b) => a * b
		}
	}

	def joinRows(u:RawDataLineSm, v:RawDataLineSm) = {
		val v1 = Vector(u.c1, u.c2, v.c1, v.c2)
		v1 match {
			case Vector(a, b, c, d) => RawDataLine(a, b, c, d)
		}
	}

	// commify printed integer values
	val formatter = java.text.NumberFormat.getIntegerInstance
	val fmt = (v:Int) => formatter.format(v)

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

}
