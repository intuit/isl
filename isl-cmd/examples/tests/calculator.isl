// Sample ISL module for unit testing (no mocks required).
// Run with: isl test .  (discovers calculator.tests.yaml)

fun add($a, $b) {
  return {{ $a + $b }};
}

fun double($x) {
  return {{ $x * 2 }};
}

fun greet($name) {
  return `Hello, ${ $name }`;
}

fun echo($value) {
  return $value;
}
