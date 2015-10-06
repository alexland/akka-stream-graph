

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

/**
*	mocks data for the stream source;
*	data bound to variable rawEntities in
*	EntityResolver below
*/
object dataMock {

	def genRawEntity():RawEntity = {
		val genTickSym = (c:Int) => Seq.fill(c)(RND.alphanumeric(10))
																	.mkString
																	.toLowerCase

		val d = "a b c d e f g h i j k l m n o p q r s t u v w x y z"
							.split(" ")
		val genDba = (c:Int) => Seq.fill(c)(d(RND.nextInt(25))).mkString

		RawEntity(
			s"${genDba(8)} ${genTickSym(4)}"
		)
	}
}

/**
*	case class for the stream source
*/
case class RawEntity(record: String)

/**
*	case class for the stream sink
*/
case class ResolvedEntity(dba: String, tickSym: String)


object EntityResolver extends App {

	implicit val actorSystem = ActorSystem("entity-resolver")
	import actorSystem.dispatcher
	implicit val flowMaterializer = ActorMaterializer()


	val fnx = (x:List[String]) => List(x.head, x.reverse.head.toLowerCase)

	val rawEntities = Source(
											(1 to 100).map( _ => dataMock.genRawEntity())
										)

	val transform1 = Flow[RawEntity]
										.map(c => c.record.split(" ").toList)
										.map(fnx(_))
										.collect {
											case dba::tickSym::Nil =>
												ResolvedEntity(dba, tickSym)
										}

	// val transform2 = Flow[RawEntity]
	// 									.map(upCaseTickSym(_))

	val persistResolvedEntities = Sink.foreach[ResolvedEntity] { entity =>
		println(entity)
	}

	import FlowGraph.Implicits._

	rawEntities
		.via(transform1)
		.runWith(persistResolvedEntities)
		.andThen {
			case _ =>
				actorSystem.shutdown()
				actorSystem.awaitTermination()
		}
}









