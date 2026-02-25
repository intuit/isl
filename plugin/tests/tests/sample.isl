import Customer from "../customer.isl";

// Sample ISL test file - tests are discovered by the Test Explorer

@setup
fun setup() {
    $x: 1;  // No-op; setup runs before each test
}

@test
fun test_simpleAssertion() {
    $value: 42;
    @.Assert.equal(42, $value, "Values should match");
}

@test
fun test_customer() {
    $c = @.Customer.transformCustomer();
    @.Assert.equal(
        {
            id: 123, name: "George",
        }, $c);
}

@test("Custom test name")
fun test_withCustomName() {
    $value: 2;
    @.Assert.equal(2, $value);
}

@test({ name: "Grouped test", group: "math" })
fun test_grouped() {
    $value: 30;
    @.Assert.equal(30, $value);
}

//@test("Failing test for error rendering")
//fun test_failsOnPurpose() {
//    $val:= { }  // compilation error
//}

@test("Failing test for error rendering")
fun test_failsOnPurpose() {
    @.Assert.equal(1, 2, "Expected 1 to equal 2 - this test fails on purpose");
}
