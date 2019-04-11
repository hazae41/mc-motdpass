package hazae41.minecraft.motdpass

import hazae41.minecraft.kotlin.bungee.*
import hazae41.minecraft.kotlin.catch
import hazae41.minecraft.kotlin.get
import hazae41.minecraft.kotlin.lowerCase
import hazae41.minecraft.kotlin.not
import net.md_5.bungee.api.ChatColor.LIGHT_PURPLE
import net.md_5.bungee.api.ServerPing
import net.md_5.bungee.api.ServerPing.PlayerInfo
import net.md_5.bungee.api.config.ServerInfo
import net.md_5.bungee.api.event.ProxyPingEvent
import net.md_5.bungee.config.Configuration

class Plugin : BungeePlugin() {

    companion object { var instance: Plugin? = null }

    override fun onEnable() = catch<Exception>{
        update(9651, LIGHT_PURPLE)
        config = loadConfig(configFile)
        schedule()
        listen()
        command()
        instance = this
    }

    override fun onDisable() { cancelTasks() }

    fun schedule(){
        cancelTasks()
        val delay = config.getLong("delay").not(0) ?: 1000L
        schedule(period = delay){pingAll()}
    }

    val motds = mutableMapOf<String, ServerPing?>()

    val configFile get() = dataFolder["config.yml"]
    lateinit var config: Configuration

    fun ping(server: ServerInfo)
            = server.ping { res, _ -> motds[server.name] = res }
    fun pingAll() = proxy.servers.values.forEach(::ping)

    fun command() = command("motdpass") h@ { args ->
        if (!hasPermission("motdpass.reload"))
            return@h msg("§cYou do not have permission.")
        if (args.isEmpty() || args[0] != "reload")
            return@h msg("&c/motdpass <reload>")
        config = loadConfig(configFile, "config.yml")
        schedule(); msg("§bConfig reloaded")
    }

    val playerInfos get()
    = proxy.players.map{
            PlayerInfo(it.name, it.uniqueId.toString())
        }.toTypedArray()

    fun listen() = listen<ProxyPingEvent> h@ {
        val listener = it.connection.listener
        val host = it.connection.virtualHost?.hostString?.lowerCase ?: return@h
        val hosts = listener.forcedHosts

        val useForcedHosts = config.getBoolean("useForcedHosts")

        val name =
            if (useForcedHosts && host in hosts) hosts[host]
            else listener.serverPriority[0]

        config.getSection("servers.$name")?.apply {
            it.response = motds[name]?.apply {
                if (getBoolean("showProxyPlayers"))
                    players = ServerPing.Players(listener.maxPlayers, proxy.onlineCount, playerInfos)
                if (getBoolean("showProxyFavicon"))
                    setFavicon(proxy.config.faviconObject)
                if (getBoolean("showProxyMotd"))
                    description = listener.motd
                if (getBoolean("showProxyVersion"))
                    version = ServerPing.Protocol("BungeeCord", proxy.protocolVersion)
            }
        }
    }
}