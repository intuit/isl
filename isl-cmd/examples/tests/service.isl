// Sample ISL module that calls an external "Api.Call" (mocked in tests).
// Run with: isl test service.tests.yaml

fun run($url) {
  $r : @.Api.Call($url);
  return $r;
}

fun getConfigLimit() {
  $limit : @.Config.GetLimit();
  return $limit;
}

fun getItems() {
  $first : @.Data.GetItems();
  $second : @.Data.GetItems();
  $third : @.Data.GetItems();
  return {
    first: $first,
    second: $second,
    third: $third
  };
}

// Two-parameter mocked function
fun computeSum($a, $b) {
  $r : @.Math.Compute($a, $b);
  return $r;
}

// Three-parameter mocked function
fun lookup($table, $key, $id) {
  $r : @.Lookup.Find($table, $key, $id);
  return $r;
}
