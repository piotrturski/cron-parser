package net.piotrturski.cron

import java.util.TreeSet

class Parser {

    companion object {
        private val timeFieldRanges = mapOf(
            "minute" to 0..59,
            "hour" to 0..23,
            "day of month" to 1..31,
            "month" to 1..12,
            "day of week" to 1..7
        )
    }

    fun describeTimesOfRun(input: String) = format(parseCron(input))

    internal data class ParsedCron(val occurrences: List<Iterable<Int>>, val command: String)

    internal fun format(parsedCron: ParsedCron) =
        (parsedCron.occurrences.map { it.joinToString(separator = " ") } + parsedCron.command)
            .zip(timeFieldRanges.keys + "command")
            { value, fieldName -> fieldName.padEnd(14) + value } // string formatting might have better performance if we care

    internal fun parseCron(input: String): ParsedCron {

        val splitFields = splitFields(input)

        val map = splitFields.zip(timeFieldRanges. values)
            .map { (field, timeFieldRange) ->
                parseSingleField(field, timeFieldRange)
            }
        return ParsedCron(map, splitFields.last())
    }

    internal fun parseSingleField(input: String, availableNumbers: IntRange) : Iterable<Int>{

        val occurrences = TreeSet<Int>()
        val maxOccurrences = availableNumbers.last - availableNumbers.first + 1

        if (input == "*") return availableNumbers

        input.split(",").forEach {
            require(it.isNotEmpty())
            val occurrencesToAdd =
                when {
                    it.contains('/') -> repeatingPattern(it.split('/'), availableNumbers)
                    it.contains('-') -> rangePattern(it.split("-"), availableNumbers)
                    else -> listOf(it.toInt().also { require(it in availableNumbers) })
                }
            occurrences += occurrencesToAdd
            if (occurrences.size == maxOccurrences)
                return availableNumbers
        }
        return occurrences;
    }

    internal fun splitFields(input: String): List<String> {
        /* command may contain spaces, fields may be separated by tabs etc. so it
            can be more complex than split(" ")
         */
        val fieldsCount = timeFieldRanges.size
        val chunks = "[^ \t]+".toRegex().findAll(input).take(fieldsCount + 1)
            .also { require(it.count() == fieldsCount +1) }
        return chunks.take(fieldsCount).map{ it.value }.toList() +
                input.substring(chunks.last().range.start)
    }

    /**
     * computes occurrences for cron pattern: x-y
     */
    private fun rangePattern(arguments: List<String>, availableNumbers: IntRange): IntProgression {
        val (start,stop) = arguments.map { it.toInt().also {require(it in availableNumbers) } }
            .also { require(it.size == 2 && it[0] <= it[1]) }
        return IntProgression.fromClosedRange(start, stop, 1)
    }

    /**
     * computes occurrences for cron pattern: x/y
     */
    private fun repeatingPattern(arguments: List<String>, availableNumbers: IntRange): IntProgression {
        val (startSymbol,step) = arguments.also { require(it.size == 2) }
        val startNumber = if (startSymbol == "*") availableNumbers.first else startSymbol.toInt()
        val stepNumber = step.toInt()
        require(startNumber in availableNumbers && stepNumber in availableNumbers)
        return IntProgression.fromClosedRange(startNumber, availableNumbers.endInclusive, stepNumber)
    }

}

fun main(vararg args: String) {
    Parser().describeTimesOfRun(args[0]).forEach(::println)
}
