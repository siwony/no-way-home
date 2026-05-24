package com.nowayhome.housecheck.application

import org.springframework.stereotype.Component
import org.w3c.dom.Document
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class MarketPriceXmlHttpClient(
    private val properties: MarketPriceProperties,
    private val safeXmlDocumentParser: SafeXmlDocumentParser,
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(properties.timeout)
        .build()

    fun getXml(
        baseUrl: String,
        params: Map<String, String>,
        rawValueKeys: Set<String> = emptySet(),
        timeout: Duration = properties.timeout,
    ): Document {
        try {
            val request = HttpRequest.newBuilder()
                .uri(buildUri(baseUrl, params, rawValueKeys))
                .timeout(timeout)
                .header("Accept", "application/xml, text/xml, */*")
                .GET()
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(Charsets.UTF_8))
            if (response.statusCode() !in 200..299) {
                throw HouseCheckExternalLookupException("Unexpected XML API HTTP status: ${response.statusCode()}")
            }
            return safeXmlDocumentParser.parse(response.body())
        } catch (exception: HouseCheckExternalLookupException) {
            throw exception
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw HouseCheckExternalLookupException("XML API request was interrupted", exception)
        } catch (exception: IOException) {
            throw HouseCheckExternalLookupException("XML API request failed", exception)
        } catch (exception: RuntimeException) {
            throw HouseCheckExternalLookupException("XML API response parsing failed", exception)
        }
    }

    private fun buildUri(baseUrl: String, params: Map<String, String>, rawValueKeys: Set<String>): URI {
        val query = params.entries.joinToString("&") { (key, value) ->
            val encodedValue = if (key in rawValueKeys) {
                value.trim()
            } else {
                URLEncoder.encode(value.trim(), Charsets.UTF_8).replace("+", "%20")
            }
            "${URLEncoder.encode(key, Charsets.UTF_8)}=$encodedValue"
        }
        return URI.create("$baseUrl?$query")
    }
}

class HouseCheckExternalLookupException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
