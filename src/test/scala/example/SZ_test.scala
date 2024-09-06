package example

class SZ_test extends munit.FunSuite {
  test("say hello") {
    assertEquals(Hello.greeting, "hello")
  }
}
