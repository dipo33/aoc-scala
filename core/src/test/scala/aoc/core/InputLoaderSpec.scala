package aoc.core

import java.io.FileNotFoundException

class InputLoaderSpec extends munit.FunSuite:

  test("missing input raises a clear, actionable error") {
    val ex = intercept[FileNotFoundException] {
      InputLoader.load(2024, 99)
    }
    assert(ex.getMessage.contains("downloadInput 2024 99"))
  }
