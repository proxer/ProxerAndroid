package me.proxer.app.settings.status

import io.reactivex.Single
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toBodySingle
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * @author Ruben Gees
 */
class ServerStatusViewModel : BaseViewModel<List<ServerStatus>>() {

    private companion object {
        private val url = "https://proxer.de".toHttpUrl()
    }

    override val isLoginRequired = false

    override val dataSingle: Single<List<ServerStatus>>
        get() = Single.fromCallable { validate() }
            .flatMap { client.newCall(constructRequest()).toBodySingle() }
            .map { Jsoup.parse(it) }
            .map { scrape(it) }

    private val client by safeInject<OkHttpClient>()

    private fun constructRequest() = Request.Builder()
        .url(url)
        .header("User-Agent", USER_AGENT)
        .header("Connection", "close")
        .build()

    private fun scrape(document: Document): List<ServerStatus> {
        return document.getElementsByTag("td").asSequence()
            .filter { it.children().none { child -> child.tagName() == "img" } }
            .flatMap { it.childNodes().asSequence() }
            .zipWithNext()
            .filter { (first, second) -> isNameNode(first) && isOnlineNode(second) }
            .map { (first, second) -> first as TextNode to second as Element }
            .map { (nameNode, onlineNode) ->
                val trimmedName = nameNode.text().trim().removeSuffix(":").removeSuffix(" *")
                val number = trimmedName.removePrefix("Server ").substringBefore(' ').toIntOrNull() ?: -1

                val type = when {
                    trimmedName.contains("stream", ignoreCase = true) -> ServerType.STREAM
                    trimmedName.contains("manga", ignoreCase = true) -> ServerType.MANGA
                    else -> ServerType.MAIN
                }

                val online = when {
                    onlineNode.text().equals("online", ignoreCase = true) -> true
                    else -> false
                }

                ServerStatus(trimmedName, number, type, online)
            }
            .sortedWith(compareBy { it.number })
            .toList()
    }

    private fun isNameNode(node: Node) = node is TextNode && node.text().contains("server", ignoreCase = true)

    private fun isOnlineNode(node: Node) = node is Element &&
        (node.text().equals("online", ignoreCase = true) || node.text().equals("offline", ignoreCase = true))
}
