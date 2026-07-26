package y2024.day04

import aoc.core.Day

object Solution extends Day[Int, Int]:

  private val XMAS_PATTERN = "XMAS".r

  private case class Position(x: Int, y: Int)
  private type PositionalChar = (Position, Char)
  private type Grid = Vector[String]
  private type FlatGrid = Vector[PositionalChar]

  private def parse(input: String): Grid =
    input.linesIterator.toVector

  private def flattenGrid(grid: Grid): FlatGrid =
    grid.zipWithIndex
      .flatMap((line, y) => line.zipWithIndex.map((char, x) => (Position(x, y), char)))

  private enum Direction:
    private case Horizontal
    private case Vertical
    private case DiagonalA
    private case DiagonalB

    def bucket(pos: Position): Int =
      this match
        case Horizontal => pos.y
        case Vertical   => pos.x
        case DiagonalA  => pos.x + pos.y
        case DiagonalB  => pos.x - pos.y

  private def getAllLines(direction: Direction, grid: FlatGrid): Seq[String] =
    grid.groupBy((pos, _) => direction.bucket(pos))
      .values
      .map(_.map(_._2).mkString)
      .toSeq

  def part1(input: String): Int =
    val grid = flattenGrid(parse(input))
    Direction.values.iterator
      .map(getAllLines(_, grid))
      .flatMap(lines => Iterator(lines, lines.map(_.reverse)))
      .flatten
      .map(XMAS_PATTERN.findAllMatchIn(_).length)
      .sum

  private def isInBounds(pos: Position, grid: Grid): Boolean =
    pos.x >= 0 && pos.y >= 0 && pos.x < grid.head.length && pos.y < grid.length

  private def checkForMS(grid: Grid, posA: Position, posB: Position): Boolean =
    if (!isInBounds(posA, grid) || !isInBounds(posB, grid))
      false
    else
      val charA = grid(posA.y).charAt(posA.x)
      val charB = grid(posB.y).charAt(posB.x)
      (charA == 'M' && charB == 'S') || (charA == 'S' && charB == 'M')

  private def checkForMAS(pos: Position, grid: Grid): Boolean =
    checkForMS(grid, Position(pos.x - 1, pos.y - 1), Position(pos.x + 1, pos.y + 1)) &&
      checkForMS(grid, Position(pos.x - 1, pos.y + 1), Position(pos.x + 1, pos.y - 1))

  def part2(input: String): Int =
    val grid = parse(input)
    val flatGrid = flattenGrid(grid)

    flatGrid
      .collect {
        case (pos, 'A') => pos
      }.count(checkForMAS(_, grid))
