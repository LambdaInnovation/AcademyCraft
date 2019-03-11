import java.net.URL

// Tests donation interface.
fun main(args: Array<String>) {
    val url = "http://144.34.208.247:8080/lambda/donation/sponsor"
    val con = URL(url).openConnection()
    con.doInput = true

    val wr = con.getInputStream()
    val text = wr.bufferedReader().readText()
    println(text)
}