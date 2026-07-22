package y2024.day01

import aoc.core.Day

object Solution extends Day[Long, Long]:

  private def parse(input: String): (Seq[Long], Seq[Long]) =
    input.split("\\s+").map(num => num.toLong).grouped(2).collect {
      case Array(left, right) => (left, right)
    }.toList.unzip

  def part1(input: String): Long = {
    val (leftList, rightList) = parse(input)
    leftList.sorted
      .zip(rightList.sorted)
      .map((left, right) => (left - right).abs)
      .sum
  }

  def part2(input: String): Long =
    val (leftList, rightList) = parse(input)
    val occurrences = rightList
      .groupBy(identity)
      .map((key, value) => (key, value.size.toLong))

    leftList
      .map(num => num * occurrences.getOrElse(num, 0L))
      .sum
