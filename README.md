# aoc-scala

A personal Advent of Code monorepo. One sbt module per year, one shared `core`
module with wiring + helpers.

## One-time setup

1. Install a JDK (11+), [sbt](https://www.scala-sbt.org/), and [direnv](https://direnv.net/).
2. Get your AoC session cookie: log into [adventofcode.com](https://adventofcode.com),
   open dev tools → Application/Storage → Cookies, copy the value of `session`.
3. `cp .envrc.example .envrc`, paste the cookie in, `direnv allow`. `.envrc` is
   gitignored, so the cookie never gets committed.
   The cookie expires periodically (roughly monthly). When `downloadInput` starts
   failing with a session error, just re-copy it into `.envrc`.
4. Confirm `.gitignore` is committed before you ever run `downloadInput` for real —
   it's what keeps real puzzle input (which AoC's ToS forbids redistributing) out
   of this public repo.

## Day-to-day workflow

```
sbt "newDay 2024 5"          # scaffold Solution.scala + SolutionSpec.scala
sbt "downloadInput 2024 5"   # fetch & cache your real input (needs AOC_SESSION)
# ... implement part1, paste part 1's example + expected value into the spec ...
sbt "runDay 2024 5"          # run against your real input, see the answer
# ... once solved, add a regression test against the real input (see below) ...
# ... AoC unlocks part 2: paste its example (sometimes different from part 1's) ...
sbt "bench 2024 5"           # lightweight before/after timing for refactors
```

### Locking in a solved answer

Once you know the real answer for a part, add a test for it directly in that
day's `SolutionSpec.scala` — this is what protects you from a later refactor
silently changing the answer:

```scala
test("part1 real input") {
  assertEquals(Solution.part1(aoc.core.InputLoader.load(2024, 5)), 123456L)
}
```

### Solving a day

Each day is `object Solution extends Day[A, B]` (`core/src/main/scala/aoc/core/Day.scala`),
where `A`/`B` are whatever types `part1`/`part2` actually return — the scaffold
defaults to `Day[Long, Long]`, edit the type params for the rare day with a
non-numeric answer. `part1` and `part2` each take the raw puzzle input as a
`String` and parse it however they want — nothing is shared or forced between
them, so if the two parts genuinely see the input differently, just write that.

## Adding a new year

Once a year, by hand in `build.sbt`:

1. Add `lazy val y20XX = project.in(file("y20XX")).dependsOn(core).settings(commonSettings*)`
2. Add `20XX -> y20XX` to `yearProjects`
3. Add `y20XX` to `root`'s `.aggregate(...)`

This is deliberately manual rather than a generated task — it happens once a
year, and programmatically rewriting `build.sbt` for it was judged not worth
the fragility.

## Running tests

- `sbt test` — every test, in every module.
- `sbt "y2024/test"` — every test, in just that year's module.
- `sbt "testDay 2024 5"` — every test in day 5's `SolutionSpec` only.
- `sbt "testDay 2024 5 part1"` — narrowed further to tests whose name contains
  `part1` (so both "part1 example" and "part1 real input").
- `sbt "testDay 2024 5 part1 real"` — narrowed to the one test whose name
  contains both words: "part1 real input".

`testDay` exists because munit's own name filter (`testOnly ... -- --tests=<regex>`)
needs a regex and gets awkward to quote correctly from a shell once the test name
has spaces in it (as in `"part1 example"`). `testDay` takes plain words instead —
any order, any number of them — and builds that regex for you.

## Tasks

- `newDay <year> <day>` — scaffold a day, refuses to overwrite existing files
- `downloadInput <year> <day> [--force]` — fetch real input, cached locally, never
  re-downloads unless `--force`
- `runDay <year> <day>` — run a day's `Solution` against its real input
- `bench <year> <day> [warmup] [iters]` — min/median timing for `part1`/`part2`
  (defaults: 5 warmup, 20 timed iterations)
- `testDay <year> <day> [word...]` — run a day's tests, optionally narrowed to
  tests whose name contains every given word (see "Running tests" above)
- `compile` / `test` — the usual sbt tasks, work as normal across every module
