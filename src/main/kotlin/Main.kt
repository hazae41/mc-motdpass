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

fun Plugin.makeEvents() = listen<ProxyPingEvent>{
    val listener = it.connection.listener
    val host = it.connection.virtualHost?.hostString?.lowerCase ?: return@listen
    val name = listener.forcedHosts[host] ?: listener.serverPriority[0] ?: return@listen
    val config = Config.Server(name)
    val sub = motds[name] ?: return@listen

    it.response.apply {
        if(config.players) players = sub.players
        if(config.favicon) setFavicon(sub.faviconObject)
        if(config.motd) descriptionComponent = sub.descriptionComponent
        version = run {
            if (config.version) sub.version
            else ServerPing.Protocol("BungeeCord", it.connection.version)
        }
        if(config.mods) modinfo.apply {
            modList = sub.modinfo.modList
            type = sub.modinfo.type
        }
    }
}