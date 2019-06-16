package me.wcy.mockhttp

import okhttp3.*
import okio.Buffer
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Created by wcy on 2019/5/23.
 */
class MockHttpInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (!MockHttp.get().hasInit()) {
            return chain.proceed(request)
        }

        val requestCopy = request.newBuilder().build()

        val path = requestCopy.url().encodedPath()
        val requestUrl = requestCopy.url().toString()
        val requestMethod = requestCopy.method()
        val requestHeader = requestCopy.headers().toString()
        val queryParameter = StringBuilder()
        for (name in requestCopy.url().queryParameterNames()) {
            if (queryParameter.isNotEmpty()) {
                queryParameter.append("\n")
            }
            queryParameter.append("$name: ${requestCopy.url().queryParameter(name)}")
        }

        val requestBody = bodyToString(requestCopy.body())

        val response: Response
        val mockResponseBody = MockHttp.get().getMockResponseBody(path)
        if (mockResponseBody != null) {
            try {
                TimeUnit.MILLISECONDS.sleep(MockHttp.get().getMockHttpOptions().getMockSleepTime())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            response = Response.Builder()
                    .body(ResponseBody.create(MediaType.parse("application/json"), mockResponseBody))
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .message("Mock")
                    .code(200)
                    .build()
        } else {
            response = chain.proceed(request)
        }

        val responseHeader = response.headers().toString()
        val statusCode = response.code()
        val isSuccessful = response.isSuccessful
        val message = response.message()
        val responseBody = response.body()
        val contentType = responseBody!!.contentType()
        var responseBodyString = ""

        var subtype: String? = null
        if (contentType != null) {
            subtype = contentType.subtype()
        }

        var newResponseBody: ResponseBody? = null
        if (isNotFileRequest(subtype)) {
            responseBodyString = responseBody.string()
            newResponseBody = ResponseBody.create(contentType, responseBodyString)
        }

        val httpEntity = MockHttpEntity(path)
        httpEntity.requestUrl = requestUrl
        httpEntity.requestMethod = requestMethod
        httpEntity.statusCode = statusCode
        httpEntity.requestHeader = requestHeader
        httpEntity.queryParameter = queryParameter.toString()
        httpEntity.requestBody = requestBody
        httpEntity.responseHeader = responseHeader
        httpEntity.responseBody = formatJson(responseBodyString)
        MockHttp.get().request(path, httpEntity)

        return response.newBuilder().body(newResponseBody).build()
    }

    private fun isNotFileRequest(subtype: String?): Boolean {
        return subtype != null && (subtype.contains("json")
                || subtype.contains("xml")
                || subtype.contains("plain")
                || subtype.contains("html"))
    }

    private fun bodyToString(body: RequestBody?): String {
        try {
            val buffer = Buffer()
            body?.writeTo(buffer)
            return formatJson(buffer.readUtf8())
        } catch (e: IOException) {
            return "{\"err\": \"" + e.message + "\"}"
        }

    }

    private fun formatJson(json: String): String {
        var string: String
        try {
            if (json.startsWith("{")) {
                string = JSONObject(json).toString(2)
            } else if (json.startsWith("[")) {
                string = JSONArray(json).toString(2)
            } else {
                string = json
            }
        } catch (e: JSONException) {
            string = json
        } catch (e1: OutOfMemoryError) {
            string = "Output omitted because of Object size."
        }

        return string
    }
}