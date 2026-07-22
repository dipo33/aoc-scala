package y2024.day01

class SolutionSpec extends munit.FunSuite:

  val example: String =
    """|3   4
       |4   3
       |2   5
       |1   3
       |3   9
       |3   3
       |""".stripMargin.trim

  test("part1 example") {
    assertEquals(Solution.part1(example), 11L)
  }

  test("part2 example") {
    assertEquals(Solution.part2(example), 31L)
  }

  test("part1 real input") {
    assertEquals(Solution.part1(aoc.core.InputLoader.load(2024, 1)), 2086478L)
  }

  test("part2 real input") {
    assertEquals(Solution.part2(aoc.core.InputLoader.load(2024, 1)), 24941624L)
  }
