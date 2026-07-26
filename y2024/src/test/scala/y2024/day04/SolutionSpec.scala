package y2024.day04

class SolutionSpec extends munit.FunSuite:

  val example: String =
    """|MMMSXXMASM
       |MSAMXMSMSA
       |AMXSXMAAMM
       |MSAMASMSMX
       |XMASAMXAMM
       |XXAMMXXAMA
       |SMSMSASXSS
       |SAXAMASAAA
       |MAMMMXMMMM
       |MXMXAXMASX
       |""".stripMargin.trim

  test("part1 example") {
    assertEquals(Solution.part1(example), 18)
  }

  test("part2 example") {
    assertEquals(Solution.part2(example), 9)
  }

  test("part1 real input") {
    assertEquals(Solution.part1(aoc.core.InputLoader.load(2024, 4)), 2521)
  }

  test("part2 real input") {
    assertEquals(Solution.part2(aoc.core.InputLoader.load(2024, 4)), 1912)
  }
