package aoc.core

import java.io.FileNotFoundException

object InputLoader:
  private def pad(day: Int): String = "%02d".format(day)

  def load(year: Int, day: Int): String =
    val path = s"day${pad(day)}/input.txt"
    Option(getClass.getClassLoader.getResourceAsStream(path)) match
      case None =>
        throw new FileNotFoundException(
          s"Could not find `$path` on the classpath for y$year day${pad(day)}. " +
            s"Run `downloadInput $year $day` first (needs AOC_SESSION set), " +
            s"or place the file manually at y$year/src/main/resources/$path."
        )
      case Some(stream) =>
        try new String(stream.readAllBytes(), "UTF-8").stripTrailing()
        finally stream.close()
