package com.intuit.isl.transform.testing.commands

import com.intuit.isl.common.FunctionExecuteContext
import com.intuit.isl.common.OperationContext
import com.intuit.isl.utils.ConvertUtils
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@Suppress("unused")
class PaginationTest : BaseTransformTest() {
    companion object {
        @JvmStatic
        fun pagePaginationFixture(): Stream<Arguments> {
            val d = "\$";
            return Stream.of(
                Arguments.of(
                    """
                        ${d}arr = [];
                        ${d}totalPages = 3;
                        ${d}count = 1;
                        
                        @.Pagination.Page( ${d}page, { startIndex: 1, pageSize: 3 } ){
                            // range at * 10 
                            ${d}arr = ${d}arr | pushItems ( @.Array.Range( {{ ${d}page.page * 10 }}, ${d}page.pageSize ) );
                            
                            // simple counter for pagination
                            ${d}totalPages = {{ ${d}totalPages - 1 }};
                            ${d}page.hasMorePages = if( ${d}totalPages > 0 ) true else false;
                        }
                        result: ${d}arr;
                    """, """{"result":[10,11,12, 20,21,22, 30,31,32]}""", null
                ),

                Arguments.of(
                    """
                        ${d}arr = [];
                        @.Pagination.Page( ${d}page, { startIndex: 1, pageSize: 3 } ){
                            ${d}arr = ${d}arr | push( ${d}page.page );
                            ${d}page.hasMorePages = if( ${d}page.page < 5 ) true else false;
                        }
                        result: ${d}arr;
                    """, """{"result":[1, 2, 3, 4, 5]}""", null
                ),

                // NOP loop - finishes after one loop
                Arguments.of(
                    """
                        ${d}arr = [];
                        @.Pagination.Page( ${d}page, { startIndex: 1, pageSize: 3 } ){
                            // range at * 10 
                            ${d}arr = ${d}arr | pushItems ( @.Array.Range( {{ ${d}page.page * 10 }}, ${d}page.pageSize ) );
                            
                            // simple counter for pagination
                            ${d}totalPages = {{ ${d}totalPages - 1 }};
                        }
                        result: ${d}arr;
                    """, """{"result":[10,11,12]}""", null
                ),

                // default page size is 100
                Arguments.of(
                    """
                        ${d}limit = null;
                        @.Pagination.Page( ${d}page, { startIndex: 1 } ){
                            ${d}limit = ${d}page.pageSize;
                        }
                        result: ${d}limit;
                    """, """{"result":100}""", null
                ),

                // update page size to 100 when < 0
                Arguments.of(
                    """
                        ${d}limit = null;
                        @.Pagination.Page( ${d}page, { startIndex: 1, pageSize: -1 } ){
                            ${d}limit = ${d}page.pageSize;
                        }
                        result: ${d}limit;
                    """, """{"result":100}""", null
                )
            );
        }

        @JvmStatic
        fun cursorPaginationFixture(): Stream<Arguments> {
            val d = "\$";
            return Stream.of(
                Arguments.of(
                    """
                        @.Pagination.Cursor( ${d}cursor ){
                            ${d}arr = ${d}arr | pushItems ( [1,2,3] );
                            // do nothing
                        }
                        result: ${d}arr;
                    """, """{"result":[1,2,3]}""", null
                ),


                // go in circles
                Arguments.of(
                    """
                        @.Pagination.Cursor( ${d}cursor ){
                            ${d}arr = ${d}arr | pushItems ( [1,2,3] );
                            // go in circles - we'll only loop once
                            ${d}cursor.next = "next page";
                        }
                        result: ${d}arr;
                    """
                    // we have twice 1,2,3 because we have it for the null loop then for the "next page" then it stops.
                    , """{"result":[1,2,3,1,2,3]}""", null
                ),

                Arguments.of(
                    """
                        ${d}totalPages = 3;
                        ${d}c = "Start";
                        @.Pagination.Cursor( ${d}cursor ){
                            ${d}arr = ${d}arr | pushItems ( [1,2,3] );
                            
                            // simple counter for pagination
                            ${d}totalPages = {{ ${d}totalPages - 1 }};
                            if( ${d}totalPages > 0 )
                                ${d}cursor.next = `Next Page ${d}totalPages`;
                                ${d}c = `${d}c::Next Page ${d}totalPages ${d}{ @.External.Call( ${d}totalPages ) }` ;
                            endif
                        }
                        
                        result: ${d}arr;
                        finalCursor: ${d}c;
                    """, """{"result":[1,2,3, 1,2,3, 1,2,3], "finalCursor":"Start::Next Page 2 External:2::Next Page 1 External:1"}""", null
                )
            );
        }

        @JvmStatic
        fun datePaginationFixture(): Stream<Arguments> {
            val d = "\$";
            return Stream.of(
                // 3 days
                Arguments.of(
                    """
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-06" | date.parse, duration: "P1D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["03-12-21","04-12-21","05-12-21"],"end":["04-12-21","05-12-21","06-12-21"]}}""", null
                ),

                // 2 days -
                Arguments.of(
                    """
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-07" | date.parse, duration: "P2D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["03-12-21","05-12-21"],"end":["05-12-21","07-12-21"]}}""", null
                ),


                // 24h
                Arguments.of(
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-04" | date.parse, duration: "PT1H" } ){
                            ${d}arr = ${d}arr | push ( ${d}i );
                            ${d}dates = ${d}dates | push ( { s: ${d}date.startDate, e: ${d}date.endDate } );
                            ${d}i = {{ ${d}i + 1 }};
                        }
                        result: ${d}arr;
                        dates: ${d}dates;
                    """, """{
                            "result":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24],
                            "dates":[{"s":"2021-12-03T00:00:00.000Z","e":"2021-12-03T01:00:00.000Z"},{"s":"2021-12-03T01:00:00.000Z","e":"2021-12-03T02:00:00.000Z"},{"s":"2021-12-03T02:00:00.000Z","e":"2021-12-03T03:00:00.000Z"},{"s":"2021-12-03T03:00:00.000Z","e":"2021-12-03T04:00:00.000Z"},{"s":"2021-12-03T04:00:00.000Z","e":"2021-12-03T05:00:00.000Z"},{"s":"2021-12-03T05:00:00.000Z","e":"2021-12-03T06:00:00.000Z"},{"s":"2021-12-03T06:00:00.000Z","e":"2021-12-03T07:00:00.000Z"},{"s":"2021-12-03T07:00:00.000Z","e":"2021-12-03T08:00:00.000Z"},{"s":"2021-12-03T08:00:00.000Z","e":"2021-12-03T09:00:00.000Z"},{"s":"2021-12-03T09:00:00.000Z","e":"2021-12-03T10:00:00.000Z"},{"s":"2021-12-03T10:00:00.000Z","e":"2021-12-03T11:00:00.000Z"},{"s":"2021-12-03T11:00:00.000Z","e":"2021-12-03T12:00:00.000Z"},{"s":"2021-12-03T12:00:00.000Z","e":"2021-12-03T13:00:00.000Z"},{"s":"2021-12-03T13:00:00.000Z","e":"2021-12-03T14:00:00.000Z"},{"s":"2021-12-03T14:00:00.000Z","e":"2021-12-03T15:00:00.000Z"},{"s":"2021-12-03T15:00:00.000Z","e":"2021-12-03T16:00:00.000Z"},{"s":"2021-12-03T16:00:00.000Z","e":"2021-12-03T17:00:00.000Z"},{"s":"2021-12-03T17:00:00.000Z","e":"2021-12-03T18:00:00.000Z"},{"s":"2021-12-03T18:00:00.000Z","e":"2021-12-03T19:00:00.000Z"},{"s":"2021-12-03T19:00:00.000Z","e":"2021-12-03T20:00:00.000Z"},{"s":"2021-12-03T20:00:00.000Z","e":"2021-12-03T21:00:00.000Z"},{"s":"2021-12-03T21:00:00.000Z","e":"2021-12-03T22:00:00.000Z"},{"s":"2021-12-03T22:00:00.000Z","e":"2021-12-03T23:00:00.000Z"},{"s":"2021-12-03T23:00:00.000Z","e":"2021-12-04T00:00:00.000Z"}]
                        }""".trimMargin(), null
                ),

                // execute once when gap between the start and end date is less than the duration
                Arguments.of(
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-06" | date.parse, duration: "P2D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["03-12-21","05-12-21"],"end":["05-12-21","06-12-21"]}}""", null
                ),

                Arguments.of(
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-04" | date.parse, duration: "P2D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["03-12-21"],"end":["04-12-21"]}}""", null
                ),

                // nothing
                Arguments.of(
                    """
                        ${d}arr = [];
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-03" | date.parse, duration: "P1D" } ){
                            ${d}arr = ${d}arr | push ( 1 );
                        }
                        result: ${d}arr;
                    """, """{"result":[]}""", null
                ),

                Arguments.of(
                    """
                        ${d}arr = [];
                        @.Pagination.Date( ${d}date, { startDate: "2021-12-03" | date.parse, endDate: "2021-12-02" | date.parse, duration: "P1D" } ){
                            ${d}arr = ${d}arr | push ( 1 );
                        }
                        result: ${d}arr;
                    """, """{"result":[]}""", null
                ),

                Arguments.of(
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2025-07-07" | date.parse, endDate: "2025-07-04" | date.parse, duration: "-P2D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["07-07-25", "05-07-25"],"end":["05-07-25", "04-07-25"]}}""", null
                ),

                Arguments.of(
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2025-07-07" | date.parse, endDate: "2025-07-01" | date.parse, duration: "-P3D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":["07-07-25", "04-07-25"],"end":["04-07-25", "01-07-25"]}}""", null
                ),

                Arguments.of(   // End date not provided
                    """
                        ${d}i = 1;
                        @.Pagination.Date( ${d}date, { startDate: "2025-07-06" | date.parse, duration: "-P1D" } ){
                            ${d}start = ${d}start | push ( ${d}date.startDate | to.string("dd-MM-yy") );
                            ${d}end = ${d}end | push ( ${d}date.endDate | to.string("dd-MM-yy") );
                        }
                        result: {
                            start: ${d}start,
                            end: ${d}end
                        }
                    """, """{"result":{"start":null,"end":null}}""", null
                ),
            );
        }
    }

    @ParameterizedTest
    @MethodSource(
        "pagePaginationFixture",
        "cursorPaginationFixture",
        "datePaginationFixture"
    )
    fun runFixtures(script: String, expectedResult: String, map: Map<String, Any?>? = null) {
        run(script, expectedResult, map);
    }

    override fun onRegisterExtensions(context: OperationContext) {
        super.onRegisterExtensions(context)
        context.registerExtensionMethod("Log.Debug", this::logDebug);
        context.registerExtensionMethod("External.Call", this::externalCall);
    }

    fun logDebug(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(first);

        println(stringValue!!.replace("\\n", "\n"));

        return null;
    }

    fun externalCall(context: FunctionExecuteContext): Any? {
        val first = context.firstParameter;
        val stringValue = ConvertUtils.tryToString(first);

        return "External:" + stringValue;
    }
}




