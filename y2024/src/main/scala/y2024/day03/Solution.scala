package y2024.day03

import aoc.core.Day
import fastparse.*
import fastparse.NoWhitespace.noWhitespaceImplicit

import scala.util.matching.Regex

object Solution extends Day[Long, Long]:

  private val multiplyPattern: Regex = """mul\((\d{1,3}),(\d{1,3})\)""".r

  def part1(input: String): Long =
    multiplyPattern.findAllMatchIn(input)
      .map(exp => exp.group(1).toLong * exp.group(2).toLong)
      .sum

  private enum Operation:
    case Multiply(a: Long, b: Long)
    case Do
    case DoNot

  private def number[$: P]: P[Long] =
    P(CharIn("0-9").rep(min = 1, max = 3).!.map(_.toLong))

  private def operationMultiply[$: P]: P[Operation] =
    P(("mul(" ~ number ~ "," ~ number ~ ")").map((a, b) => Operation.Multiply(a, b)))

  private def operationDo[$: P]: P[Operation] =
    P("do()").map(_ => Operation.Do)

  private def operationDoNot[$: P]: P[Operation] =
    P("don't()").map(_ => Operation.DoNot)

  private def operation[$: P]: P[Operation] =
    P(operationMultiply | operationDo | operationDoNot)

  private def program[$: P]: P[Seq[Operation]] =
    P((operation.map(Some(_)) | AnyChar.map(_ => None)).rep ~ End)
      .map(_.flatten)

  private case class State(enabled: Boolean, result: Long)

  private def execute(state: State, op: Operation): State =
    op match
      case Operation.Do             => State(true, state.result)
      case Operation.DoNot          => State(false, state.result)
      case Operation.Multiply(a, b) =>
        if state.enabled
        then State(state.enabled, state.result + a * b)
        else State(state.enabled, state.result)

  def part2(input: String): Long =
    val prog: Seq[Operation] = parse(input, program(_)).get.value
    prog.foldLeft(State(true, 0L))(execute).result
