package net.piotrturski.cron

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.lang.RuntimeException

internal class ParserTest {

    val p = Parser()

    @Test
    internal fun `should parse single field`() {
        with(SoftAssertions()) {

            assertThat(p.parseSingleField("1", 1..7)).containsExactly(1)
            assertThat(p.parseSingleField("*", 1..3)).containsExactly(1,2,3)
            assertThat(p.parseSingleField("1-3", 1..7)).containsExactly(1,2,3)
            assertThat(p.parseSingleField("1-3,7", 1..7)).containsExactly(1,2,3,7)
            assertThat(p.parseSingleField("*/2", 1..7)).containsExactly(1,3,5,7)
            assertThat(p.parseSingleField("*/2", 0..5)).containsExactly(0,2,4)
            assertThat(p.parseSingleField("3/2", 0..6)).containsExactly(3,5)
            assertThat(p.parseSingleField("3/2", 0..7)).containsExactly(3,5,7)
            assertThat(p.parseSingleField("13/2,1-2,4,*/11", 0..17))
                                .containsExactlyInAnyOrder(13,15,17,   1,2,   4,   0,11,)
            assertThat(p.parseSingleField("3/2", 0..7)).containsExactly(3,5,7)
            assertThat(p.parseSingleField("2-2", 0..7)).containsExactly(2)

            assertAll()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "0",
        "sdf",
        "*/8",
        "1/8",
        "0/3",
        "0/3",
        "1/3/4",
        "0-3",
        "1-3-3",
        "3-8",
        "3-2",
        "*,4",
        "1,,2"
    ])
    internal fun `should prevent illegal fields (for sample range 1-7)`(field: String) {

        assertThatThrownBy { p.parseSingleField(field, 1..7) }
            .describedAs("expected exception for $field")
            .isNotNull
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "* * * 0 * /dfv",
        "*",
        "* * * * * ",
    ])
    internal fun `should prevent illegal cron expressions`(input: String) {
        assertThatThrownBy { p.parseCron(input) }
            .describedAs("expected exception for $input")
            .isNotNull
    }

    @Test
    internal fun `should format output`() {
        assertThat(p.format(p.parseCron("*/15 0 1,15 * 1-5 /usr/bin/find")).joinToString(separator = "\n"))
            .isEqualTo(
                """
                    minute        0 15 30 45
                    hour          0
                    day of month  1 15
                    month         1 2 3 4 5 6 7 8 9 10 11 12
                    day of week   1 2 3 4 5
                    command       /usr/bin/find
                """.trimIndent()
            )
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "1 2 3 4 5 /usr/bin/find-something",
        "  1   2   3   4   5   /usr/bin/find something",
        "\t\t1 2\t3  4 5\t/usr/bin/find\t\tsomething"
    ])
    fun `should split fields`(input: String) {

        assertThat(p.splitFields(input))
            .hasSize(6)
            .startsWith("1", "2", "3", "4", "5")
            .last().satisfies {
                assertThat(it).startsWith("/usr/bin/find")
                    .endsWith("something")
            }
    }

    @Test
    fun `should preserve whitespaces in command field`() {
        assertThat(p.splitFields("1 2 3 4 5 a  b \t c  ")[5]).isEqualTo("a  b \t c  ")
    }

}