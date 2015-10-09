
package org.dougybarbo


import dispatch._
import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import org.json4s.JsonAST.{JValue, JString}
import scala.collection.immutable._


object RedditAPI {

	val linksToFetch = 5
	val subredditsToFetch = 2
	val commentsToFetch = 10
	val commentDepth = 2

	val useragent = "macosx: org.dy.redditapiclient4akkastreams:v1.0"

	val url1 = "http://www.reddit.com/r/$subreddit/top.json"

}




