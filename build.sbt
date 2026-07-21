import complete.DefaultParsers._
import java.io.File

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val commonSettings = Seq(
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.4" % Test,
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val core = project.in(file("core")).settings(commonSettings*)

lazy val y2024 = project.in(file("y2024")).dependsOn(core).settings(commonSettings*)

// Single source of truth: year -> its sbt Project. Adding a new year means:
//   1. add a `lazy val y20XX = ...` above (dependsOn(core), commonSettings)
//   2. add an entry here
//   3. add it to `aggregate(...)` below
// This is a deliberate manual, once-a-year step (see README) rather than a
// generated one: programmatically rewriting build.sbt to add a year was
// considered and rejected as fragile string-surgery for something this rare.
lazy val yearProjects: Map[Int, Project] = Map(
  2024 -> y2024
)

lazy val root = project
  .in(file("."))
  .aggregate(core, y2024)
  .settings(
    name := "aoc",
    publish / skip := true
  )

lazy val downloadInput = inputKey[Unit]("downloadInput <year> <day> [--force] - fetch & cache real puzzle input")
lazy val newDay        = inputKey[Unit]("newDay <year> <day> - scaffold Solution.scala + SolutionSpec.scala")
lazy val runDay        = inputKey[Unit]("runDay <year> <day> - run a day's Solution, print part1/part2")
lazy val bench         = inputKey[Unit]("bench <year> <day> [warmup] [iters] - lightweight benchmark")

downloadInput := Def.inputTask {
  val args = spaceDelimited("<arg>").parsed
  AocTasks.downloadInput((ThisBuild / baseDirectory).value, yearProjects.keySet, args)
}.evaluated

newDay := Def.inputTask {
  val args = spaceDelimited("<arg>").parsed
  AocTasks.newDay((ThisBuild / baseDirectory).value, yearProjects.keySet, args)
}.evaluated

runDay := Def.inputTaskDyn {
  val args = spaceDelimited("<arg>").parsed
  val (year, day) = AocTasks.parseYearDay(args)
  classpathTaskFor(year).map(cp => AocTasks.runDay(cp, year, day))
}.evaluated

bench := Def.inputTaskDyn {
  val args = spaceDelimited("<arg>").parsed
  val (year, day, warmup, iters) = AocTasks.parseBenchArgs(args)
  classpathTaskFor(year).map(cp => AocTasks.bench(cp, year, day, warmup, iters))
}.evaluated

def classpathTaskFor(year: Int): Def.Initialize[Task[Seq[File]]] =
  yearProjects.get(year) match {
    case Some(p) =>
      (p / Compile / fullClasspath).map(_.files)
    case None =>
      Def.task {
        val knownYears = yearProjects.keySet.toSeq.sorted.mkString(", ")
        sys.error(s"No sbt module for year $year. Known years: $knownYears. Add it to build.sbt (see README).")
      }
  }
