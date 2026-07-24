package y2024.day03

class SolutionSpec extends munit.FunSuite:

  val example_1: String =
    """|xmul(2,4)%&mul[3,7]!@^do_not_mul(5,5)+mul(32,64]then(mul(11,8)mul(8,5))
       |""".stripMargin.trim

  val example_2: String =
    """|xmul(2,4)&mul[3,7]!^don't()_mul(5,5)+mul(32,64](mul(11,8)undo()?mul(8,5))
       |""".stripMargin

  test("part1 example") {
    assertEquals(Solution.part1(example_1), 161L)
  }

  test("part2 example") {
    assertEquals(Solution.part2(example_2), 48L)
  }

  test("part1 real input") {
    assertEquals(Solution.part1(aoc.core.InputLoader.load(2024, 3)), 156388521L)
  }

  test("part2 real input") {
    assertEquals(Solution.part2(aoc.core.InputLoader.load(2024, 3)), 75920122L)
  }
