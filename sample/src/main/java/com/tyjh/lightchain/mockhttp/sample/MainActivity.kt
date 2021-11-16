package com.tyjh.lightchain.mockhttp.sample

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import com.tyjh.lightchain.mockhttp.MockHttp
import com.tyjh.lightchain.mockhttp.MockHttpInterceptor
import com.tyjh.lightchain.mockhttp.MockHttpOptions
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val okHttpClient = OkHttpClient()
            .newBuilder()
            .addInterceptor(MockHttpInterceptor())
            .build()
    private val executor = Executors.newFixedThreadPool(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val options = MockHttpOptions.Builder()
                .setMockServerPort(5000)
                .setMockSleepTime(500)
                .setLogEnable(true)
                .setLogTag("MAIN-TAG")
                .setLogLevel(Log.ERROR)
                .build()
        MockHttp.get().setMockHttpOptions(options)
    }

    override fun onDestroy() {
        super.onDestroy()
        MockHttp.get().stop()
    }

    override fun onClick(v: View?) {
        when (v) {
            btn_start_mock -> {
                MockHttp.get().start(applicationContext)
                log("\nMock 服务启动\nMock 后台地址：\n${MockHttp.get().getMockAddress()}")
            }
            btn_stop_mock -> {
                MockHttp.get().stop()
                log("\nMock 服务停止")
            }
            btn_request_1 -> {
                request("https://www.wanandroid.com/article/top/json")
            }
            btn_request_2 -> {
                request("https://www.wanandroid.com/hotkey/json")
            }
        }
    }

    private fun request(url: String) {
        executor.execute {
            val request = Request.Builder()
                    .url(url)
                    .header("MOCK-HTTP-HEADER", "TEST")
                    .build()

            log("\n开始请求：\n$url")

            var responseText: String
            try {
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body()
                val subtype = responseBody?.contentType()?.subtype()
                if (responseBody != null && isNotFileRequest(subtype)) {
                    responseText = responseBody.string()
                } else {
                    responseText = "response body is not text"
                }
            } catch (e: Exception) {
                responseText = e.message ?: "request fail"
            }

            log("\n请求结果：\n${formatJson(responseText)}")
        }
    }

    private fun log(log: String) {
        runOnUiThread {
            tv_network_log.append("\n$log")
            scroll_view.post {
                scroll_view.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun isNotFileRequest(subtype: String?): Boolean {
        return subtype != null && (subtype.contains("json")
                || subtype.contains("xml")
                || subtype.contains("plain")
                || subtype.contains("html"))
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
