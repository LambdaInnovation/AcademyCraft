import java.net.URL

// Tests donation interface.
fun main(args: Array<String>) {
    val url = "https://ac.li-dev.cn/analytics"
    val con = URL(url).openConnection()
    con.doInput = true

    val wr = con.getInputStream()
    val text = wr.bufferedReader().readText()
    println(text)
}