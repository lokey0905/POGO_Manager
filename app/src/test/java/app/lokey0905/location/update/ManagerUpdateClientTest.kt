package app.lokey0905.location.update

import app.lokey0905.location.version.compareVersions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.CancellationException
import javax.net.ssl.SSLHandshakeException

class ManagerUpdateClientTest {
    @Test
    fun parsesActualTagInsteadOfReleaseTitle() {
        assertEquals(
            ManagerUpdateResult.Release("2.30", "v2.30", "Changes\n修正"),
            parseManagerRelease("""{"tag_name":"v2.30","name":"Friendly release title","body":"Changes\n修正"}""")
        )
    }

    @Test
    fun acceptsUppercasePrefixAndUnprefixedNumericTag() {
        assertEquals(ManagerUpdateResult.Release("2.30", "V2.30", ""),
            parseManagerRelease("""{"tag_name":"V2.30","body":null}"""))
        assertEquals(ManagerUpdateResult.Release("2.30.1", "2.30.1", ""),
            parseManagerRelease("""{"tag_name":"2.30.1"}"""))
    }

    @Test
    fun usesNameOnlyWhenTagIsAbsent() {
        assertEquals(ManagerUpdateResult.Release("2.30", "v2.30", ""),
            parseManagerRelease("""{"name":"v2.30"}"""))
        listOf("null", "42", "\"\"", "\"latest\"").forEach { invalidTag ->
            assertNull(parseManagerRelease("""{"tag_name":$invalidTag,"name":"v2.30"}"""))
        }
    }

    @Test
    fun rejectsMalformedJsonAndUnusableFields() {
        listOf("", "not json", "{", "[]", "null", "{}", "{\"tag_name\":",
            """{"tag_name":"v2.30"} trailing""",
            """{"tag_name":"v2.30"} {}""",
            """{"name":"Release 2.30"}""",
            """{"tag_name":"v2.30","body":123}"""
        ).forEach { body -> assertNull(body, parseManagerRelease(body)) }
    }

    @Test
    fun rejectsNonNumericPrereleaseAndPathTags() {
        listOf("latest", "v", "v2.30-beta", "2..30", ".2.30", "2.30.", "2/30",
            "v2.30/other", "2.30?query", "2.30#fragment", " 2.30", "2.30 ", "-2.30"
        ).forEach { tag -> assertNull(tag, parseManagerRelease("""{"tag_name":"$tag"}""")) }
    }

    @Test
    fun parsedVersionsUseNumericComparisonRatherThanLexicalOrdering() {
        val older = parseManagerRelease("""{"tag_name":"v2.9"}""")!!
        val newer = parseManagerRelease("""{"tag_name":"v2.10"}""")!!
        assertTrue(compareVersions(newer.version, older.version) > 0)
        assertEquals(0, compareVersions("2.30", parseManagerRelease(RELEASE)!!.version))
    }

    @Test
    fun sendsGetWithHeadersAndTimeoutsWithoutCredentials() {
        val connection = FakeConnection(200)
        var openedUrl: URL? = null
        val result = ManagerUpdateClient(openConnection = { openedUrl = it; connection }, nanoTime = { 0L })
            .fetch(ENDPOINT, "POGO_Manager/2.30", NOW)
        assertEquals(ManagerUpdateResult.Release("2.30", "v2.30", ""), result)
        assertEquals(ENDPOINT, openedUrl.toString())
        assertEquals("GET", connection.requestMethod)
        assertEquals(10_000, connection.connectTimeout)
        assertEquals(10_000, connection.readTimeout)
        assertEquals("application/vnd.github+json", connection.getRequestProperty("Accept"))
        assertEquals("POGO_Manager/2.30", connection.getRequestProperty("User-Agent"))
        assertNull(connection.getRequestProperty("Authorization"))
        assertTrue(connection.successStream.closed)
        assertTrue(connection.disconnected)
        assertEquals(listOf("status", "input"), connection.events)
    }

    @Test
    fun recognizesExhaustedQuota403AndResetDeadline() {
        val connection = FakeConnection(403, headers = mapOf(
            "X-RateLimit-Remaining" to "0", "X-RateLimit-Reset" to "${(NOW + 180_000) / 1_000}"
        ))
        assertEquals(ManagerUpdateResult.RateLimited(403, NOW + 180_000), fetch(connection))
        assertEquals(listOf("status", "error"), connection.events)
        assertTrue(connection.errorBody!!.closed)
        assertTrue(connection.disconnected)
        assertEquals(0, connection.inputReads)
    }

    @Test
    fun recognizesPrimaryAndSecondaryLimitMessagesWithoutHeaders() {
        listOf("API rate limit exceeded for 192.0.2.1.",
            "You have exceeded a secondary rate limit. Please wait a few minutes.",
            "Secondary limit reached"
        ).forEach { message ->
            val connection = FakeConnection(403, error = """{"message":"$message"}""")
            assertEquals(ManagerUpdateResult.RateLimited(403, NOW + 60_000), fetch(connection))
        }
    }

    @Test
    fun validRetryAfterAloneIdentifies403Limit() {
        val connection = FakeConnection(403, headers = mapOf("Retry-After" to "120"))
        assertEquals(ManagerUpdateResult.RateLimited(403, NOW + 120_000), fetch(connection))
    }

    @Test
    fun any429IsLimitedEvenWithMissingOrNonJsonBody() {
        listOf<String?>(null, "", "gateway error", """{"message":"Too many requests"}""").forEach { body ->
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000),
                fetch(FakeConnection(429, error = body)))
        }
    }

    @Test
    fun ordinary403RemainsHttpErrorDespiteResetHeader() {
        val connection = FakeConnection(403,
            headers = mapOf("X-RateLimit-Remaining" to "59", "X-RateLimit-Reset" to "${(NOW + 600_000) / 1_000}"),
            error = """{"message":"Resource not accessible by integration"}""")
        assertEquals(ManagerUpdateResult.HttpError(403), fetch(connection))
    }

    @Test
    fun notFoundAndServerErrorsDoNotReadSuccessStream() {
        listOf(404, 500).forEach { status ->
            val connection = FakeConnection(status)
            assertEquals(ManagerUpdateResult.HttpError(status), fetch(connection))
            assertEquals(0, connection.inputReads)
            assertEquals(listOf("status", "error"), connection.events)
            assertTrue(connection.errorBody!!.closed)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun serviceUnavailableHonorsRetryAfterWithoutBeingRateLimited() {
        val connection = FakeConnection(503, headers = mapOf("Retry-After" to "300"))
        val result = fetch(connection)
        assertEquals(ManagerUpdateResult.HttpError(503, NOW + 300_000), result)
        assertEquals(NOW + 300_000, nextManagerUpdateCheckAt(result, NOW))
    }

    @Test
    fun rateLimitUsesLatestOfRetryAfterAndReset() {
        val reset = "${(NOW + 180_000) / 1_000}"
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 180_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to "120", "X-RateLimit-Reset" to reset))))
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 300_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to "300", "X-RateLimit-Reset" to reset))))
    }

    @Test
    fun parsesRfc1123RetryAfterDateWithoutServerDate() {
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 240_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to httpDate(NOW + 240_000)))))
    }

    @Test
    fun serverDateCorrectsRetryAfterAndResetForClocksAheadOrBehind() {
        listOf(NOW - 86_400_000, NOW + 86_400_000).forEach { serverNow ->
            val commonHeaders = mapOf("Date" to httpDate(serverNow))
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 240_000), fetch(FakeConnection(429,
                headers = commonHeaders + ("Retry-After" to httpDate(serverNow + 240_000)))))
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 300_000), fetch(FakeConnection(429,
                headers = commonHeaders + ("X-RateLimit-Reset" to "${(serverNow + 300_000) / 1_000}"))))
        }
    }

    @Test
    fun relativeRetryAfterIsNotShiftedByServerClock() {
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 120_000), fetch(FakeConnection(429,
            headers = mapOf("Date" to httpDate(NOW + 86_400_000), "Retry-After" to "120"))))
    }

    @Test
    fun malformedHeadersCannotCrashOrTurnOrdinary403IntoRateLimit() {
        listOf("", "not a date", "-1", "1.5", "+10", "120, 240").forEach { value ->
            val headers = mapOf("Retry-After" to value, "X-RateLimit-Reset" to value,
                "X-RateLimit-Remaining" to value, "Date" to value)
            assertEquals(ManagerUpdateResult.HttpError(403), fetch(FakeConnection(403, headers)))
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000),
                fetch(FakeConnection(429, headers)))
        }
    }

    @Test
    fun badServerDateFallsBackToLocalClock() {
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 240_000), fetch(FakeConnection(429,
            headers = mapOf("Date" to "invalid", "Retry-After" to httpDate(NOW + 240_000)))))
    }

    @Test
    fun expiredAndZeroDeadlinesStillWaitAtLeastOneMinute() {
        listOf("0", "1", httpDate(NOW - 600_000)).forEach { retry ->
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000), fetch(FakeConnection(429,
                headers = mapOf("Retry-After" to retry, "X-RateLimit-Reset" to "1"))))
        }
        assertEquals(ManagerUpdateResult.RateLimited(403, NOW + 60_000), fetch(FakeConnection(403,
            headers = mapOf("Retry-After" to "0"))))
    }

    @Test
    fun unrepresentableNumericHeadersAreIgnoredInsteadOfPermanentlyBlockingChecks() {
        listOf(Long.MAX_VALUE.toString(), "9999999999999999999999999999999999",
            (Long.MAX_VALUE / 1_000L + 1).toString()
        ).forEach { huge ->
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000), fetch(FakeConnection(429,
                headers = mapOf("Retry-After" to huge))))
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000), fetch(FakeConnection(429,
                headers = mapOf("X-RateLimit-Reset" to huge))))
            assertEquals(ManagerUpdateResult.HttpError(403), fetch(FakeConnection(403,
                headers = mapOf("Retry-After" to huge))))
            assertEquals(ManagerUpdateResult.HttpError(503), fetch(FakeConnection(503,
                headers = mapOf("Retry-After" to huge))))
        }
    }

    @Test
    fun malformedDeadlineDoesNotDiscardOtherValidServerDeadline() {
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 180_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to Long.MAX_VALUE.toString(),
                "X-RateLimit-Reset" to "${(NOW + 180_000) / 1_000}"))))
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 120_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to "120", "X-RateLimit-Reset" to Long.MAX_VALUE.toString()))))
    }

    @Test
    fun largeRepresentableDeadlinesAreNotArbitrarilyClamped() {
        val largestEpochSeconds = Long.MAX_VALUE / 1_000L
        val largestRelativeSeconds = (Long.MAX_VALUE - NOW) / 1_000L
        assertEquals(ManagerUpdateResult.RateLimited(429, largestEpochSeconds * 1_000L),
            fetch(FakeConnection(429, headers = mapOf("X-RateLimit-Reset" to "$largestEpochSeconds"))))
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + largestRelativeSeconds * 1_000L),
            fetch(FakeConnection(429, headers = mapOf("Retry-After" to "$largestRelativeSeconds"))))
    }

    @Test
    fun relativeDeadlineAdditionOverflowIsAlsoIgnored() {
        val representableDelayWithUnrepresentableDeadline = Long.MAX_VALUE / 1_000L
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000), fetch(FakeConnection(429,
            headers = mapOf("Retry-After" to "$representableDelayWithUnrepresentableDeadline"))))
    }

    @Test
    fun relativeWaitIncludesConnectionAndErrorBodyLatency() {
        val connection = FakeConnection(429, headers = mapOf("Retry-After" to "120"))
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 132_000), fetchSlowResponse(connection))
        assertTrue(connection.errorBody!!.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun serverDateTranslationsIncludeEntireResponseLatency() {
        val serverNow = NOW - 86_400_000
        listOf(mapOf("Retry-After" to httpDate(serverNow + 180_000)),
            mapOf("X-RateLimit-Reset" to "${(serverNow + 180_000) / 1_000}")
        ).forEach { deadlineHeader ->
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 192_000),
                fetchSlowResponse(FakeConnection(429,
                    headers = deadlineHeader + ("Date" to httpDate(serverNow)))))
        }
    }

    @Test
    fun absoluteDeadlinesWithoutServerDateStayAbsoluteDespiteResponseLatency() {
        listOf(mapOf("Retry-After" to httpDate(NOW + 180_000)),
            mapOf("X-RateLimit-Reset" to "${(NOW + 180_000) / 1_000}")
        ).forEach { deadlineHeader ->
            assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 180_000),
                fetchSlowResponse(FakeConnection(429, headers = deadlineHeader)))
        }
    }

    @Test
    fun fallbackAndServiceUnavailableWaitsStartAfterErrorBodyFinishes() {
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 72_000), fetchSlowResponse(FakeConnection(429)))
        assertEquals(ManagerUpdateResult.HttpError(503, NOW + 132_000), fetchSlowResponse(FakeConnection(503,
            headers = mapOf("Retry-After" to "120"))))
    }

    @Test
    fun failedErrorBodyStillContributesItsElapsedTime() {
        val connection = FakeConnection(429, headers = mapOf("Retry-After" to "120"))
        connection.errorBody!!.readFailure = SocketTimeoutException("slow error body")
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 132_000), fetchSlowResponse(connection))
        assertTrue(connection.errorBody.closed)
    }

    @Test
    fun negativeElapsedClockCannotShortenWaitAndNanoTimeWrapIsHandled() {
        val backwards = listOf(10_000_000_000L, 5_000_000_000L).iterator()
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000),
            ManagerUpdateClient(openConnection = { FakeConnection(429) }, nanoTime = { backwards.next() })
                .fetch(ENDPOINT, "test", NOW))
        val wrapping = listOf(Long.MAX_VALUE - 2_000_000L, Long.MIN_VALUE + 3_000_000L).iterator()
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_005),
            ManagerUpdateClient(openConnection = { FakeConnection(429) }, nanoTime = { wrapping.next() })
                .fetch(ENDPOINT, "test", NOW))
    }

    @Test
    fun malformedSuccessIsInvalidResponseAndStillClosesResources() {
        listOf("{", "[]", """{"tag_name":"latest"}""").forEach { body ->
            val connection = FakeConnection(200, success = body)
            assertSame(ManagerUpdateResult.InvalidResponse, fetch(connection))
            assertTrue(connection.successStream.closed)
            assertTrue(connection.disconnected)
        }
    }

    @Test
    fun oversizedSuccessBodyIsBoundedAndRejected() {
        val connection = FakeConnection(200, success = """{"tag_name":"v2.30","body":"${"a".repeat(600 * 1024)}"}""")
        assertSame(ManagerUpdateResult.InvalidResponse, fetch(connection))
        assertTrue(connection.successStream.bytesRead <= 512 * 1024 + 1)
        assertTrue(connection.successStream.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun oversizedErrorBodyIsBoundedAndClosedWithoutLosingStatus() {
        val connection = FakeConnection(429, error = "a".repeat(32 * 1024))
        assertEquals(ManagerUpdateResult.RateLimited(429, NOW + 60_000), fetch(connection))
        assertTrue(connection.errorBody!!.bytesRead <= 16 * 1024 + 1)
        assertTrue(connection.errorBody.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun missingErrorStreamIsOrdinaryHttpError() {
        val connection = FakeConnection(404, error = null)
        assertEquals(ManagerUpdateResult.HttpError(404), fetch(connection))
        assertTrue(connection.disconnected)
    }

    @Test
    fun failingOptionalErrorBodyDoesNotEraseHttpStatus() {
        val connection = FakeConnection(503, headers = mapOf("Retry-After" to "120"))
        connection.errorBody!!.readFailure = IOException("body interrupted")
        assertEquals(ManagerUpdateResult.HttpError(503, NOW + 120_000), fetch(connection))
        assertTrue(connection.errorBody.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun mapsNetworkFailuresAndDisconnects() {
        listOf(
            UnknownHostException("dns") to UpdateNetworkFailure.DNS,
            SocketTimeoutException("timeout") to UpdateNetworkFailure.TIMEOUT,
            SSLHandshakeException("certificate") to UpdateNetworkFailure.TLS,
            ConnectException("refused") to UpdateNetworkFailure.CONNECTION,
            IOException("other") to UpdateNetworkFailure.CONNECTION
        ).forEach { (failure, expected) ->
            val connection = FakeConnection(200)
            connection.statusFailure = failure
            assertEquals(ManagerUpdateResult.NetworkError(expected), fetch(connection))
            assertTrue(connection.disconnected)
            assertEquals(0, connection.inputReads)
        }
    }

    @Test
    fun openingConnectionFailureIsAlsoClassified() {
        assertEquals(ManagerUpdateResult.NetworkError(UpdateNetworkFailure.DNS),
            ManagerUpdateClient(openConnection = { throw UnknownHostException("dns") }, nanoTime = { 0L })
                .fetch(ENDPOINT, "test", NOW))
    }

    @Test
    fun failingSuccessStreamIsClosedAndMapped() {
        val connection = FakeConnection(200)
        connection.successStream.readFailure = SocketTimeoutException("read timeout")
        assertEquals(ManagerUpdateResult.NetworkError(UpdateNetworkFailure.TIMEOUT), fetch(connection))
        assertTrue(connection.successStream.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun cancellationIsRethrownAndConnectionIsDisconnected() {
        val cancellation = CancellationException("cancelled")
        val connection = FakeConnection(200)
        connection.statusFailure = cancellation
        assertSame(cancellation, assertThrows(CancellationException::class.java) { fetch(connection) })
        assertTrue(connection.disconnected)
    }

    @Test
    fun cancellationDuringErrorReadIsNotSwallowed() {
        val cancellation = CancellationException("cancelled")
        val connection = FakeConnection(403)
        connection.errorBody!!.readFailure = cancellation
        assertSame(cancellation, assertThrows(CancellationException::class.java) { fetch(connection) })
        assertTrue(connection.errorBody.closed)
        assertTrue(connection.disconnected)
    }

    @Test
    fun successChecksAgainInOneHour() {
        assertEquals(NOW + 3_600_000,
            nextManagerUpdateCheckAt(ManagerUpdateResult.Release("2.30", "v2.30", ""), NOW))
    }

    @Test
    fun allFailuresThrottleForAtLeastOneMinute() {
        listOf(
            ManagerUpdateResult.InvalidResponse,
            ManagerUpdateResult.HttpError(404),
            ManagerUpdateResult.HttpError(503, NOW - 1),
            ManagerUpdateResult.RateLimited(403, NOW + 1),
            ManagerUpdateResult.NetworkError(UpdateNetworkFailure.CONNECTION)
        ).forEach { result -> assertEquals(NOW + 60_000, nextManagerUpdateCheckAt(result, NOW)) }
    }

    @Test
    fun throttleNeverShortensServerDeadline() {
        listOf(ManagerUpdateResult.RateLimited(429, NOW + 86_400_000),
            ManagerUpdateResult.HttpError(503, NOW + 86_400_000)
        ).forEach { result -> assertEquals(NOW + 86_400_000, nextManagerUpdateCheckAt(result, NOW)) }
    }

    @Test
    fun throttleAndFetchGuardLongOverflow() {
        listOf(ManagerUpdateResult.Release("2.30", "v2.30", ""),
            ManagerUpdateResult.InvalidResponse, ManagerUpdateResult.RateLimited(429, Long.MAX_VALUE),
            ManagerUpdateResult.HttpError(503, Long.MAX_VALUE)
        ).forEach { result ->
            assertEquals(Long.MAX_VALUE, nextManagerUpdateCheckAt(result, Long.MAX_VALUE - 100))
        }
        assertEquals(ManagerUpdateResult.RateLimited(429, Long.MAX_VALUE),
            fetch(FakeConnection(429, headers = mapOf("Retry-After" to "120")), Long.MAX_VALUE - 1))
    }

    private fun fetch(connection: FakeConnection, now: Long = NOW): ManagerUpdateResult =
        ManagerUpdateClient(openConnection = { connection }, nanoTime = { 0L })
            .fetch(ENDPOINT, "POGO_Manager/test", now)

    private fun fetchSlowResponse(connection: FakeConnection): ManagerUpdateResult {
        var clockNanos = 0L
        connection.onStatus = { clockNanos = 5_000_000_000L }
        connection.errorBody!!.onClose = { clockNanos = 12_000_000_000L }
        return ManagerUpdateClient(openConnection = { connection }, nanoTime = { clockNanos })
            .fetch(ENDPOINT, "POGO_Manager/test", NOW)
    }

    private fun httpDate(millis: Long): String = DateTimeFormatter.RFC_1123_DATE_TIME
        .format(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC))

    private class TrackingStream(body: String) : ByteArrayInputStream(body.toByteArray(Charsets.UTF_8)) {
        var closed = false
        var bytesRead = 0
        var readFailure: Throwable? = null
        var onClose: () -> Unit = {}

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            readFailure?.let { throw it }
            return super.read(buffer, offset, length).also { if (it > 0) bytesRead += it }
        }

        override fun close() {
            closed = true
            onClose()
            super.close()
        }
    }

    private class FakeConnection(
        private val status: Int,
        private val headers: Map<String, String> = emptyMap(),
        success: String = RELEASE,
        error: String? = "{}"
    ) : HttpURLConnection(URL(ENDPOINT)) {
        val successStream = TrackingStream(success)
        val errorBody = error?.let(::TrackingStream)
        val events = mutableListOf<String>()
        var statusFailure: Throwable? = null
        var onStatus: () -> Unit = {}
        var inputReads = 0
        var disconnected = false
        private var statusRead = false

        override fun getResponseCode(): Int {
            events += "status"
            onStatus()
            statusFailure?.let { throw it }
            statusRead = true
            return status
        }

        override fun getInputStream(): InputStream {
            events += "input"
            inputReads++
            assertTrue("HTTP status must be read before input", statusRead)
            if (status !in 200..299) throw FileNotFoundException("HTTP $status")
            return successStream
        }

        override fun getErrorStream(): InputStream? {
            events += "error"
            assertTrue("HTTP status must be read before error body", statusRead)
            return errorBody
        }

        override fun getHeaderField(name: String?): String? =
            headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

        override fun connect() { connected = true }
        override fun disconnect() { disconnected = true }
        override fun usingProxy(): Boolean = false
    }

    companion object {
        private const val NOW = 1_700_000_000_000L
        private const val ENDPOINT = "https://api.github.com/repos/example/manager/releases/latest"
        private const val RELEASE = """{"tag_name":"v2.30"}"""
    }
}
