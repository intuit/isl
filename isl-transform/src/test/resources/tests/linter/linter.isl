// various linter rules for ISL

fun lint ( $s ){
    $result = $result
        | push( @.this.check_condition_duplicateSelection( $s[0].assignprop.value.condition ) )
        | push( @.this.check_toString_defaultDateFormat( $s[1].assignprop.value ) )

    return $result;
}

@lint( { instruction: 'if' } )
@lint( { instruction: 'condition' } )
fun check_condition_duplicateSelection( $s ){
	$iif = $s.['if'];

	$result = @.this.check_if_deepSelector( $iif );
	if( $result )
		return $result;
	else
		// recurse down
		if( $iif.left.['if'] )
			return @.this.check_condition_duplicateSelection( $iif.left )
		else
			return null
		endif
	endif
}

fun check_if_deepSelector( $iif ){
	$leftSelector = @.This.if_typeCheck( $iif.left, 'left', 'selector', 'exists' )
	$rightSelector = @.This.if_typeCheck( $iif.right, 'left', 'selector', 'exists' )
	$op = $iif.op;

	// left = $value and right = $value.something
	return if( $op == 'and' and $rightSelector startsWith `$leftSelector.` )
		{
			loc: $iif._loc,
			token: 'if',
			type: 'warning',
			message: 'Multiple null checks can be collapsed.',
			recommendation: `if( $rightSelector )`
		}
	else
		null
}


// if.left.selector && op=
fun if_typeCheck( $iif, $side, $type, $op ){
	$value = $iif.['if'] | getProperty( $side ) | getProperty( $type );
	$opv = $iif.['if'] | getProperty( 'op' )

	return if ( $value and $op == $opv )
		$value;
	else
		null;
}



// Modifier | to.string( default date format )
@lint( { instruction: 'call', filter: { "name": "to.string" } } )
fun check_toString_defaultDateFormat( $s ){
    @.Log.Info(`Testing to_string( $s )`)
    if( $s.call.args | length == 2 and
        $s.call.args[1].literal == "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" )
        return {
            loc: $s.call._loc,
            token: 'to.string',
            type: 'warning',
            message: `Modifier | to.string( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ) is already using the ISO date as a default.`,
            recommendation: `| to.string()`
        }
    endif
}

// CallApi or Shared.CallApi or something similar that has a `path` that has a `?`
@lint( { instruction: 'call', filter: { "name": "call.api" } } )
@lint( { instruction: 'call', filter: { "name": "shared.callapi" } } )
@lint( { instruction: 'call', filter: { "name": "util.callapi" } } )
fun check_callApi_pathWithNoQuery( $s ){
    $params = $s.call.args[0].object.statements | to.array; // sometimes this is an object if it's just one property
    $path = $params
        | filter ( $.assignprop.name == 'path' and $.assignprop.value.literal )
        | map ( $.assignprop.value.literal ) | at(0);

    @.Log.Info(`Testing CallApi( $path )`)

    if( $path contains "?" )
        $pathOnly = $path | subStringUpto( '?' )
        return {
               loc: $s.call._loc,
               token: $s.call.name,
               type: 'warning',
               message: `${ $s.call.name }( { path } is embedding a hardcoded query string using '?' .`,
               recommendation: `${ $s.call.name } ( {\n    path: "$pathOnly",\n    query: {\n        queryParam: value\n    }})`
           }
    endif
}


// @lint( { instruction: 'call', filter: { "name": "concat" } } )
fun check_concat_to_interpolate( $s ){
    @.Log.Info(`Testing Concat( $s )`)
    // contact ( prevValue, parameter1, parameter2)
    $prevVal = $s.call.args[0].literal ?? $s.call.args[0].selector;
    $p1 = $s.call.args[1].literal ?? $s.call.args[1].selector;
    $p2 = $s.call.args[2].literal ?? $s.call.args[2].selector ?? '';
    $iq = "`";

    if( $prevVal and $p1 and $prevVal !contains "{" )
        $loc = @.This.locStartEnd($s.call.args[0], $s.call)

        // we need few safety checks
        // literal after selector or selector with more than one value needs to be protected
        $prevIsSelector = if ( $s.call.args[0].selector ) true else false endif;
        if(    ($prevIsSelector and $prevVal contains ".")
            or ($prevIsSelector and $s.call.args[2].literal)    // delimiter is literal - protect prev
            or ($prevIsSelector and !$p2 and $s.call.args[1].literal ) // no delim, next is literal
          )
            $prevVal = @.This.protect_interpolate( $prevVal, true )
        endif

        $p1 = @.This.protect_interpolate( $p1 )
        $p2 = if( $s.call.args[2].selector )
                @.This.protect_interpolate( $p2, true )   // always needs to be protected
              else
                $p2 // literal

        // TODO???: If we detect interpolation of purely text with no variables convert from `` to text ""
        return {
              loc: $loc,
              token: $s.call.name,
              type: 'warning',
              message: `| contact( ) expressions can be simplified to use String Interpolation.`,
              help: "",
              recommendation: `$iq$prevVal$p2$p1$iq`
          }
    endif

    // more complex handling for deeper concat
    // $transaction.info.total | concat( "USD" ) | concat( $result );
    // if ( $s.call.args[0].call.name == "concat" )
        // @.Log.Info("complex concat") - Nice to have
        // TODO: let's go deep in the object structure until we find something else than a concat and interpolate the complete structure
        // until then we'll report one error at a time and re-interpolate after next save
    // endif
}
fun protect_interpolate( $value, $force ){
    return if( ($value startsWith "$" and $value contains ".") or ($value and $force) )
               "${ " | concat( $value ) | concat(" }");
           else
                $value;
}



//@lint( { instruction: 'call', filter: { "name": "concat" } } )
fun check_concat_to_append( $s ){
    @.Log.Info(`Testing Concat convert to Append( $s )`)
    // we might have multiple concat one after another
    $list = [ $s ]
    $item = $s;
    $depth = 1
    while ( $item.call.args[0].call.name == "concat" )
        $item = $item.call.args[0]
        $list = $list | push ( $item )
        $depth = {{ $depth + 1 }}
    endwhile

    if( $depth == 1 )
        return {}
    endif


    $end = $s.call._loc | substringAfter("/")

    // $item is the lowest - this would have the first arg as either literal, variable or maybe function
    // new code
    // @.Log.Info( $item )
    // @.Log.Info( $list | reverse )
    // $new = $item.call.args[0].literal ?? $item.call.args[0].variable;
    $new = "append( "
    $first = true;
    $start = "";
    foreach $v in $list | reverse
        @.Log.Info( `testing $v` )
        if( $first == true )
            $start = $v.call._loc | substringUpTo("/")
        endif

        if( $v.call.args | length > 2 ) // has 3rd param
            $extra = $v.call.args[2].literal ?? $v.call.args[2].variable;
            if( $first == false)
                $new = $new | append( ", " )
            endif

            if( $extra )
                $new = $new | append( $extra | keepType )
            else    // we can't lint this yet
                return { }
            endif
        endif

        if( $first == false )
            $new = $new | append( ", " )
        endif

        $val = $v.call.args[1].literal ?? $v.call.args[1].variable;
        if( $val )
            $new = $new | append( $val | keepType )
        else    // we can't lint this yet
            return { }
        endif

        $first = false
    endfor
    $new = $new | append( " )" )
    // $new =  | append( $item.call.args[1].literal ?? $item.call.args[1].variable )
    // @.Log.Info(`New Code $new`);
    // this is a bit tricky as we want to leave the original text - so start after the very first
    return {
      loc: `$start/$end`,
      token: "concat",
      id: "W_SA1",
      type: 'warning',
      message: `Replace multiple |concat with a single |append.`,
      recommendation: $new
  }
}

modifier keepType( $val ){
    return if( $val is "string" )
        `"$val"`;
    else
        $val
    endif
}



// try to detect statements where we assign the same variable multiple times
//       $result.value.prop1 = "123";
//       $result.value.prop2 = "234";
// this could be $result.value = {
//    prop1: "123",
//    prop2: "234"
// }
// @lint( { instruction: 'statements' } )
fun check_statements_shallowassignproperty( $s ){
    @.Log.Info(`Testing AssignProperty( $s )`)

    if( $path contains "?" )
        $pathOnly = $path | subStringUpto( '?' )

        return {
               loc: $s.call._loc,
               token: $s.call.name,
               type: 'warning',
               message: `${ $s.call.name }( { path } is embedding a hardcoded query string using '?' .`,
               recommendation: `${ $s.call.name } ( {\n    path: "$pathOnly",\n    query: {\n        queryParam: value\n    }})`
           }
    endif
}


fun locStartEnd( $start, $end ){
    return `${ $start._loc | substringUpTo( "/" ) }/${ $end._loc | substringAfter( "/" ) }`
}