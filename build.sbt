
name := "akka-stream-graph"

assemblyJarName in assembly := "EntityResolver.jar"

version := "1.0-SNAPSHOT"

organization := "org.dougybarbo"

scalaVersion := "2.11.7"

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= {
	Seq(
		"org.json4s"					%%		"json4s-jackson"						%			"3.3.0.RC6",
		"io.spray"						%% 		"spray-json"								% 		"1.3.2",
		"com.typesafe.akka"		%%		"akka-actor"								%			"2.4.0",
		"com.typesafe.akka"		%%		"akka-stream-experimental"	%			"1.0",
		"org.scalaz"					%%		"scalaz-core"								%			"7.2.0-M3"
	)
}

scalacOptions in Test ++= Seq("-Yrangepos")

mainClass in (Compile, run) := Some("org.dougybarbo.EntityResolver")

javacOptions ++= Seq(
	"-Xlint:unchecked",
	"-Xlint:deprecation",
	"-Xmx4096m",
	"-Xms512m",
	"-XX:MaxPerSize=512"
)

assemblyMergeStrategy in assembly := {
	case PathList("javax", "servlet", xs @ _*) 						=>		MergeStrategy.first
	case PathList(ps @ _*) if ps.last endsWith ".html"		=>		MergeStrategy.first
	case "application.conf"																=>		MergeStrategy.concat
	case "unwanted.txt" 																	=>		MergeStrategy.discard
	case x 																								=>
		val oldStrategy = (assemblyMergeStrategy in assembly).value
		oldStrategy(x)
}

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
	{
		case "application.conf" 														=>		MergeStrategy.concat
		case "reference.conf" 															=> 		MergeStrategy.concat
		case "META-INF/spring.tooling" 											=>		MergeStrategy.concat
		case "overview.html"																=> 		MergeStrategy.rename
		case PathList("javax", "servlet", xs @ _*)					=>		MergeStrategy.last
		case PathList("org", "apache", xs @ _*) 						=> 		MergeStrategy.last
		case PathList("META-INF", xs @ _*) 									=> 		MergeStrategy.discard
		case PathList("com", "esotericsoftware", xs @ _*) 	=> 		MergeStrategy.last
		case "about.html" 																	=> 		MergeStrategy.rename
		case x 																							=>		old(x)
	}
}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
	cp filter { f =>
		(f.data.getName contains "commons-logging") ||
		(f.data.getName contains "sbt-link")
	}
}
