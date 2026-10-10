package app.lokey0905.location.api

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Test

class DataHubVersionParserTest {
    private val downloadUrlFormat = "https://assets-v2.pgtools.net/data-hub-%1\$s.apk"

    @Test
    fun officialResponseUsesAppNameAndV2DownloadUrl() {
        val result = DataHubVersionParser.parseApiResponse(
            """{"appName":"2.5.3","appCode":25003}""",
            downloadUrlFormat
        )

        assertEquals("2.5.3", result.appVersion)
        assertEquals("https://assets-v2.pgtools.net/data-hub-2.5.3.apk", result.downloadUrl)
    }

    @Test
    fun futureVersionIsReadFromResponseRatherThanPinned() {
        val result = DataHubVersionParser.parseApiResponse(
            """{"appName":" 2.10.0 ","appCode":210000,"extra":"ignored"}""",
            downloadUrlFormat
        )

        assertEquals("2.10.0", result.appVersion)
        assertEquals("https://assets-v2.pgtools.net/data-hub-2.10.0.apk", result.downloadUrl)
    }

    @Test
    fun missingBlankOrInvalidAppNameIsRejected() {
        listOf(
            """{}""",
            """{"appName":null}""",
            """{"appName":" "}""",
            """{"appName":2.5}""",
            """{"appName":"2.5.3/other"}""",
            """{"appName":"ERROR"}"""
        ).forEach { response ->
            org.junit.Assert.assertThrows(JSONException::class.java) {
                DataHubVersionParser.parseApiResponse(response, downloadUrlFormat)
            }
        }
    }

    @Test(expected = JSONException::class)
    fun malformedResponseIsRejected() {
        DataHubVersionParser.parseApiResponse("not JSON", downloadUrlFormat)
    }
}
