package y2024.day02

class SolutionSpec extends munit.FunSuite:

  val example: String =
    """|7 6 4 2 1
       |1 2 7 8 9
       |9 7 6 2 1
       |1 3 2 4 5
       |8 6 4 4 1
       |1 3 6 7 9
       |""".stripMargin.trim

  test("part1 example") {
    assertEquals(Solution.part1(example), 2)
  }

  test("part2 example") {
    assertEquals(Solution.part2(example), 4)
  }

  test("part1 real input") {
    assertEquals(Solution.part1(aoc.core.InputLoader.load(2024, 2)), 314)
  }

  test("part2 real input") {
    assertEquals(Solution.part2(aoc.core.InputLoader.load(2024, 2)), 373)
  }
