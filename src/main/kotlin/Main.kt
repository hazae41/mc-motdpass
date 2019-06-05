package hazae41.minecraft.motdpass

import hazae41.minecraft.kotlin.*
import hazae41.minecraft.kotlin.bungee.*
import net.md_5.bungee.api.ChatColor.LIGHT_PURPLE
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.ServerPing.PlayerInfo
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.config.Configuration
import java.util.concurrent.TimeUnit

class Plugin : BungeePlugin() {

    val motds = mutableMapOf<String, ServerPing?>()

    override fun onEnable(){
        update(9651)
        init(Config)
        makeTimer()
        makeEvents()
    }
}

object Config: PluginConfigFile("config"){
    override var minDelay = 10000L

    val delay by string("delay")

    class Server(name: String): ConfigSection(this, "servers.$name"){
        val players by boolean("players")
        val favicon by boolean("favicon")
        val motd by boolean("motd")
        val version by boolean("version")
        val mods by boolean("mods")
    }
}

fun Plugin.makeTimer(){
    schedule(async = true){ pingAll() }
    val (value, unit) = Config.delay.toTimeWithUnit()
    val delay = unit.toSeconds(value).not(0) ?: 10
    schedule(
        delay = delay,
        unit = TimeUnit.SECONDS,
        callback = { makeTimer() }
    )
}

fun Plugin.ping(server: ServerInfo) = server.ping { res, _ -> motds[server.name] = res }
fun Plugin.pingAll() = proxy.servers.values.forEach(::ping)

val Plugin.playerInfos
    get() = proxy.players.map{
        PlayerInfo(it.name, it.uniqueId.toString())
    }.toTypedArray()

fun Plugin.makeEvents() = listen<ProxyPingEvent>{
    val listener = it.connection.listener
    val host = it.connection.virtualHost?.hostString?.lowerCase ?: return@listen
    val name = listener.forcedHosts[host] ?: listener.serverPriority[0] ?: return@listen
    val config = Config.Server(name)

    it.response = motds[name]?.apply {
        if(!config.players) players = ServerPing.Players(listener.maxPlayers, proxy.onlineCount, playerInfos)
        if(!config.favicon) setFavicon(proxy.config.faviconObject)
        if(!config.motd) description = listener.motd
        if(!config.version) version = ServerPing.Protocol("BungeeCord", it.connection.version)
        if(!config.mods) modinfo.apply {
            modList = emptyList()
            type = "FML"
        }
    }
}