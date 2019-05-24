package me.wcy.mockhttp

import org.json.JSONObject

/**
 * Created by wcy on 2019/5/23.
 */
data class MockHttpEntity(
        var path: String = "",
        var requestHeader: String = "",
        var requestBody: String = "",
        var responseHeader: String = "",
        var responseBody: String = "") {

    companion object {
        fun fromJson(json: String?): MockHttpEntity? {
            if (json == null) {
                return null
            }

            try {
                val jsonObject = JSONObject(json)
                val entity = MockHttpEntity()
                entity.path = jsonObject.optString("path")
                entity.requestHeader = jsonObject.optString("requestHeader")
                entity.requestBody = jsonObject.optString("requestBody")
                entity.responseHeader = jsonObject.optString("responseHeader")
                entity.responseBody = jsonObject.getString("responseBody")
                return entity
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }

    fun toJson(): String {
        val map = mutableMapOf<String, String>()
        map["path"] = path
        map["requestHeader"] = requestHeader
        map["requestBody"] = requestBody
        map["responseHeader"] = responseHeader
        map["responseBody"] = responseBody
        return JSONObject(map).toString(2)
    }
}