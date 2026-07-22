import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.{URI, URLClassLoader}
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.file.Files

// Lives under project/, so this compiles against sbt's own meta-build Scala
// version (2.12.x) regardless of the main build's Scala 3 setting - keep the
// syntax here Scala-2.12-safe (no toIntOption, no top-level `enum`, etc.).
object AocTasks {

  // sbt recognizes this exception type and prints just the message, no stack trace.
  private def die(msg: String): Nothing = throw new sbt.internal.util.MessageOnlyException(msg)

  def pad(day: Int): String = "%02d".format(day)

  def solutionClassName(year: Int, day: Int): String = s"y$year.day${pad(day)}.Solution"

  // Parent MUST be null (bootstrap only). getClass.getClassLoader would resolve
  // scala.runtime.* from sbt's own Scala-2.12 meta-build classpath instead of the
  // project's Scala-3/2.13 one under parent-first delegation, causing NoSuchMethodError.
  def classLoaderFor(classpath: Seq[File]): URLClassLoader =
    new URLClassLoader(classpath.map(_.toURI.toURL).toArray, null)

  def invokePart(loader: ClassLoader, year: Int, day: Int, methodName: String, input: String): AnyRef = {
    val cls = Class.forName(solutionClassName(year, day), true, loader)
    val method = cls.getMethod(methodName, classOf[String])
    method.invoke(null, input)
  }

  def loadInputReflectively(loader: ClassLoader, year: Int, day: Int): String = {
    val cls = Class.forName("aoc.core.InputLoader", true, loader)
    val method = cls.getMethod("load", classOf[Int], classOf[Int])
    method.invoke(null, Integer.valueOf(year), Integer.valueOf(day)).asInstanceOf[String]
  }

  // isInstanceOf[NotImplementedError] silently fails to match here: the exception's
  // Class was loaded by the child URLClassLoader, a different Class object than any
  // scala.NotImplementedError the meta-build could statically reference. Compare by
  // fully-qualified name instead.
  def isNotImplemented(t: Throwable): Boolean =
    t != null && t.getClass.getName == "scala.NotImplementedError"

  private def parseIntOrDie(s: String, label: String): Int =
    try s.toInt
    catch {
      case _: NumberFormatException => die(s"expected an integer for $label, got `$s`")
    }

  def parseYearDay(args: Seq[String]): (Int, Int) = args match {
    case Seq(y, d, _*) => (parseIntOrDie(y, "year"), parseIntOrDie(d, "day"))
    case _              => die("usage: <task> <year> <day>")
  }

  def parseBenchArgs(args: Seq[String]): (Int, Int, Int, Int) = {
    val (year, day) = parseYearDay(args)
    val rest = args.drop(2)
    val warmup = rest.headOption.map(parseIntOrDie(_, "warmup")).getOrElse(5)
    val iters = rest.drop(1).headOption.map(parseIntOrDie(_, "iters")).getOrElse(20)
    (year, day, warmup, iters)
  }

  private def unknownYearMessage(year: Int, knownYears: Set[Int]): String =
    s"No sbt module for year $year. Known years: ${knownYears.toSeq.sorted.mkString(", ")}. " +
      s"Add it to build.sbt first (see README)."

  // ---------------- testDay ----------------

  // Builds the extra args string for `<project> / Test / testOnly`: the day's
  // SolutionSpec, plus (if words are given) a munit `--tests=` regex requiring
  // every word to appear somewhere in the test name, in any order - so
  // `testDay 2024 1 part1 real` narrows down to the single "part1 real input" test
  // without the caller needing to know munit's filter syntax or fight shell quoting.
  def testOnlyArgs(year: Int, day: Int, words: Seq[String]): String = {
    val className = s"y$year.day${pad(day)}.SolutionSpec"
    if (words.isEmpty) s" $className"
    else {
      val lookaheads = words.map(w => s"(?=.*${java.util.regex.Pattern.quote(w)})").mkString
      s" $className -- --tests=$lookaheads.*"
    }
  }

  private def ensureClassAvailable(loader: ClassLoader, year: Int, day: Int): Unit =
    try Class.forName(solutionClassName(year, day), false, loader)
    catch {
      case _: ClassNotFoundException =>
        die(s"No Solution found for y$year day${pad(day)} - run `newDay $year $day` first.")
    }

  // ---------------- runDay ----------------

  def runDay(classpath: Seq[File], year: Int, day: Int): Unit = {
    val loader = classLoaderFor(classpath)
    ensureClassAvailable(loader, year, day)
    println(s"=== y$year day${pad(day)} ===")
    try {
      val input = loadInputReflectively(loader, year, day)
      printPart(loader, year, day, "part1", input)
      printPart(loader, year, day, "part2", input)
    } catch {
      case e: InvocationTargetException => println(e.getCause.getMessage)
      case e: Exception                 => println(e.getMessage)
    }
  }

  private def printPart(loader: ClassLoader, year: Int, day: Int, part: String, input: String): Unit =
    try {
      val result = invokePart(loader, year, day, part, input)
      println(s"$part = $result")
    } catch {
      case e: InvocationTargetException if isNotImplemented(e.getCause) =>
        println(s"$part: not implemented")
      case e: InvocationTargetException =>
        println(s"$part: threw an exception:")
        e.getCause.printStackTrace()
    }

  // ---------------- bench ----------------

  def bench(classpath: Seq[File], year: Int, day: Int, warmup: Int, iters: Int): Unit = {
    val loader = classLoaderFor(classpath)
    ensureClassAvailable(loader, year, day)
    println(s"=== bench y$year day${pad(day)} (warmup=$warmup, iters=$iters) ===")
    try {
      val input = loadInputReflectively(loader, year, day)
      benchPart(loader, year, day, "part1", input, warmup, iters)
      benchPart(loader, year, day, "part2", input, warmup, iters)
    } catch {
      case e: InvocationTargetException => println(e.getCause.getMessage)
      case e: Exception                 => println(e.getMessage)
    }
  }

  private def timeMs(f: => AnyRef): Double = {
    val t0 = System.nanoTime()
    f
    (System.nanoTime() - t0) / 1e6
  }

  private def benchPart(loader: ClassLoader, year: Int, day: Int, part: String, input: String, warmup: Int, iters: Int): Unit =
    try {
      (1 to warmup).foreach(_ => invokePart(loader, year, day, part, input))
      val samples = (1 to iters).map(_ => timeMs(invokePart(loader, year, day, part, input))).sorted
      val min = samples.head
      val median = samples(samples.size / 2)
      println(f"$part: min=$min%.3fms median=$median%.3fms ($iters iters)")
    } catch {
      case e: InvocationTargetException if isNotImplemented(e.getCause) =>
        println(s"$part: not implemented, skipping")
      case e: InvocationTargetException =>
        println(s"$part: threw an exception, skipping:")
        e.getCause.printStackTrace()
    }

  // ---------------- newDay ----------------

  def newDay(baseDir: File, knownYears: Set[Int], args: Seq[String]): Unit = {
    val (year, day) = parseYearDay(args)
    if (!knownYears.contains(year)) die(unknownYearMessage(year, knownYears))

    val dd = pad(day)
    val yearDir = new File(baseDir, s"y$year")
    val solutionFile = new File(yearDir, s"src/main/scala/y$year/day$dd/Solution.scala")
    val specFile = new File(yearDir, s"src/test/scala/y$year/day$dd/SolutionSpec.scala")

    if (solutionFile.exists() || specFile.exists())
      die(s"y$year day$dd already has files - refusing to overwrite (${solutionFile.getPath})")

    writeFile(solutionFile, solutionTemplate(year, dd))
    writeFile(specFile, specTemplate(year, dd))

    println(s"created ${solutionFile.getPath}")
    println(s"created ${specFile.getPath}")
  }

  private def writeFile(file: File, contents: String): Unit = {
    file.getParentFile.mkdirs()
    Files.write(file.toPath, contents.getBytes("UTF-8"))
  }

  private def solutionTemplate(year: Int, dd: String): String =
    s"""package y$year.day$dd
       |
       |import aoc.core.Day
       |
       |object Solution extends Day[Long, Long]:
       |
       |  def part1(input: String): Long =
       |    ???
       |
       |  def part2(input: String): Long =
       |    ???
       |""".stripMargin

  private def specTemplate(year: Int, dd: String): String =
    s"""package y$year.day$dd
       |
       |class SolutionSpec extends munit.FunSuite:
       |
       |  val example: String =
       |    \"\"\"|
       |       |\"\"\".stripMargin.trim
       |
       |//  test("part1 example") {
       |//    assertEquals(Solution.part1(example), ???)
       |//  }
       |
       |//  test("part2 example") {
       |//    assertEquals(Solution.part2(example), ???)
       |//  }
       |
       |//  test("part1 real input") {
       |//    assertEquals(Solution.part1(aoc.core.InputLoader.load($year, ${dd.toInt})), ???)
       |//  }
       |
       |//  test("part2 real input") {
       |//    assertEquals(Solution.part2(aoc.core.InputLoader.load($year, ${dd.toInt})), ???)
       |//  }
       |""".stripMargin

  // ---------------- downloadInput ----------------

  private val httpClient: HttpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NEVER)
    .build()

  def downloadInput(baseDir: File, knownYears: Set[Int], args: Seq[String]): Unit = {
    val (year, day) = parseYearDay(args)
    val force = args.drop(2).contains("--force")

    if (!knownYears.contains(year)) die(unknownYearMessage(year, knownYears))

    val dest = new File(baseDir, s"y$year/src/main/resources/day${pad(day)}/input.txt").toPath

    if (Files.exists(dest) && !force) {
      println(s"cached: $dest (use --force to re-download)")
      return
    }

    sys.env.get("AOC_SESSION") match {
      case None =>
        die("AOC_SESSION env var is not set. Copy the `session` cookie value from adventofcode.com into .envrc (see .envrc.example) and run `direnv allow`.")
      case Some(session) =>
        val request = HttpRequest.newBuilder()
          .uri(URI.create(s"https://adventofcode.com/$year/day/$day/input"))
          .header("Cookie", s"session=$session")
          .header("User-Agent", "aoc-scala personal use (github.com/dipo33)")
          .GET()
          .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        response.statusCode() match {
          case 200 if looksLikeErrorBody(response.body()) =>
            die("Got HTTP 200 but the body looks like a login/error page, not puzzle input - AOC_SESSION is likely expired.")
          case 200 =>
            Files.createDirectories(dest.getParent)
            Files.writeString(dest, response.body())
            println(s"downloaded: $dest")
          case 302 | 301 =>
            die("Redirected (likely to /auth/login) - AOC_SESSION is missing or expired.")
          case 400 | 401 =>
            die(s"HTTP ${response.statusCode()} - AOC_SESSION is missing or invalid. Body: ${response.body().take(200)}")
          case 404 =>
            die(s"HTTP 404 - day $day of year $year isn't available yet (or doesn't exist).")
          case other =>
            die(s"HTTP $other - unexpected response. Body: ${response.body().take(200)}")
        }
    }
  }

  private def looksLikeErrorBody(body: String): Boolean =
    body.contains("Please log in") || body.stripLeading().startsWith("<") || body.isBlank
}
