package app.yitap.search.algorithms.data.calculator.internal

import java.math.BigDecimal

fun interface Function {

    fun call(arguments: List<BigDecimal>): BigDecimal
}
