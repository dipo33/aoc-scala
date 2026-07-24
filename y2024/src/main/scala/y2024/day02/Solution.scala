package y2024.day02

import aoc.core.Day

object Solution extends Day[Int, Int]:

  private def parse(input: String): Seq[Seq[Int]] =
    input.linesIterator
      .map(_.split("\\s+").map(_.toInt).toSeq)
      .toSeq

  private def isSafeStep(diff: Int): Boolean =
    diff.abs >= 1 && diff.abs <= 3

  private def isSafeReport(report: Seq[Int]): Boolean =
    val diffs =
      report
        .sliding(2, 1)
        .map { case Seq(a, b) =>
          b - a
        }
        .toList

    diffs.forall(d => isSafeStep(d) && d > 0) || diffs.forall(d => isSafeStep(d) && d < 0)

  def part1(input: String): Int =
    val reports = parse(input)
    reports.count(isSafeReport)

  def part2(input: String): Int =
    val reports = parse(input)
    reports.count { report =>
      val variations = report.indices
        .map(i => report.take(i) ++ report.drop(i + 1))

      (Iterator.single(report) ++ variations)
        .exists(isSafeReport)
    }
