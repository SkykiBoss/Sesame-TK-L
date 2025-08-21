package fansirsqi.xposed.sesame.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object FansirsqiUtil {
    // 定义一言API的URL
    private const val HITOKOTO_API_URL = "https://60s.cccccc.plus/v2/love"

    /**
     * 获取一言（挂起函数），推荐在协程中使用
     * @return 成功返回句子，失败返回默认句子
     */
    suspend fun getOneWord(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val connection = URL(HITOKOTO_API_URL).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }

             val jsonObject = JSONObject(response)
            val dataObject = jsonObject.getJSONObject("data")
            val hitokoto = dataObject.optString(
                "love", // 爱情语录
                " 好好吃饭。🍚"
            )
            "$hitokoto\n\n                    -----By：《 F ❤ C 》"
        } catch (e: Exception) {
            Log.printStackTrace(e)
            " 好好吃饭。\n\n                    -----By：《 F ❤ C 》"
        }
    }

    /**
     * 生成随机字符串
     * @param length 字符串长度
     */
    fun getRandomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

}
