package aoc.core

trait Day[A, B]:
  def part1(input: String): A
  def part2(input: String): B
