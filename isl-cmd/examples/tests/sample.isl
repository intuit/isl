// Annotation-based tests (@setup and @test) – discovered by: isl test .
// Uses @.Mock.Load for mocks; assertions via @.Assert.

@setup
fun setupTests() {
  @.Mock.Load("../mocks/sample-mocks.yaml")
}

@test
fun testApiCallMock() {
  $r : @.Api.Call("https://example.com")
  @.Assert.Equal({ status: 200, body: "ok" }, $r)
}

@test
fun testConfigLimit() {
  $limit : @.Config.GetLimit()
  @.Assert.Equal(100, $limit)
}
