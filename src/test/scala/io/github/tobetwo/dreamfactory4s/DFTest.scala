package io.github.tobetwo.dreamfactory4s

import org.scalatest.FunSuite
class DFTest extends FunSuite{
  test("base test") {
    println(DF ~ "user" ~ "session" ~ Map(
      "email" -> "978713973@qq.com",
      "password" -> "123456"
    ) postJson)
  }

  test("cache test") {
    assert(DF.cache.set("hello", "world"))
  }

  test("cache get test") {
    val cache = DF.cache get "hello"
    assert(cache.exists && cache.data == "world")
  }

  test("cache delete test") {
    assert(DF.cache delete "hello")
    assert(!(DF.cache get "hello" exists))
  }

  test("test mysql") {
    import io.github.tobetwo.dreamfactory4s.implicits._
    println(DF.mysql table "v_user_all" where ($"id" === 1 | $"roleName" ~ "测试") limit 10 list)
  }
}
