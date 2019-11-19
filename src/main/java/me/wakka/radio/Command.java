package me.wakka.radio;

import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static me.wakka.radio.Radio.*;

public class Command implements CommandExecutor {
	static final String PREFIX = "§8§l[§eRadio§8§l]";
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
		if(!(sender instanceof Player)){
			return false;
		}
		Player player = (Player) sender;
		String usage = "Usage: /radio join|leave|toggle|songinfo";
		String usageAdmin = "Usage: /radio admin players|songs|reload";

		if(!player.hasPermission("radio.player") || !player.hasPermission("radio.admin")){
			return false;
		}

		if(args.length >= 1) {
			firstarg:
			switch(args[0]){
				case "join":
					if(!Utils.isInRangeOfAnyRadiusRadio(player)){
						addPlayer(player, serverRadio);
						addServerRadioListener(player);
					}else{
						player.sendMessage(PREFIX + " §cYou are too close to another radio!");
					}
					break;
				case "leave":
					if(!Utils.isInRangeOfAnyRadiusRadio(player)){
						removePlayer(player,serverRadio);
						removeServerRadioListener(player);
					}else{
						player.sendMessage(PREFIX + " §cYou cannot leave this radio!");
					}
					break;
				case "toggle":
					if(Utils.isListening(player, serverRadio)){
						removePlayer(player, serverRadio);
						removeServerRadioListener(player);
					}else{
						addPlayer(player, serverRadio);
						addServerRadioListener(player);
					}
					break;
				case "songinfo":
					// Gets radio listened to or if in radius of a radius radio, gets the radius radio
					SongPlayer radio = Utils.getListenedRadio(player);

					if(radio == null) {
						player.sendMessage(PREFIX + " §cYou are not listening to a radio!");
						break;
					}

					Song song = radio.getSong();
					double songLen = radio.getSong().getLength();
					double current = radio.getTick();
					int percent = (int) ((current / songLen) * 100.0);

					player.sendMessage(PREFIX + "§3 Current Song Playing:");
					player.sendMessage("§3Title:§e " + song.getTitle());
					player.sendMessage("§3Author:§e " + song.getAuthor());
					player.sendMessage("§3Progress:§e " + percent + "§e%");
					player.sendMessage("");
					break;
				case "admin":
					if(!player.hasPermission("radio.admin") || args.length < 2)
						break;

					switch(args[1]){
						case "reload":
							// Remove old radius radios
							for (PositionSongPlayer tempRadio : radiusRadios) {
								tempRadio.setAutoDestroy(true);
								tempRadio.setPlaying(false);
								tempRadio.destroy();
							}

							radiusRadios.clear();
							player.performCommand("plugman reload radio");
							break firstarg;
						case "players":
							Set<UUID> uuids = serverRadio.getPlayerUUIDs();
							StringBuilder playerList = new StringBuilder();
							int uuidsSize = uuids.size();
							if(uuidsSize > 0) {
								int ndx = 1;
								for (UUID uuid : uuids) {
									Player p = Bukkit.getPlayer(uuid);
									playerList.append("§3").append(ndx).append(" §e").append(p.getName());
									if (uuidsSize > 1) {
										playerList.append("\n");
									}
									ndx++;
								}
								player.sendMessage(PREFIX + " §3Players listening:");
								player.sendMessage(playerList.toString());
							}else{
								player.sendMessage(PREFIX + " §cNo players are listening.");
							}
							player.sendMessage("");
							break firstarg;

						case "songs":
							List<Song> songs = serverRadio.getPlaylist().getSongList();
							int songListSize = songs.size();
							if(songListSize > 0) {
								StringBuilder songList = new StringBuilder();
								int ndx = 1;
								for (Song tempSong : songs) {
									File songFile = tempSong.getPath();
									songList.append("§3").append(ndx).append(" §e").append(songFile.getName());
									if (songListSize > 1) {
										songList.append("\n");
									}
									ndx++;
								}
								player.sendMessage(PREFIX + " §3Songs in playlist:");
								player.sendMessage(songList.toString());
							}else{
								player.sendMessage(PREFIX + " §cNo songs in playlist.");
							}
							player.sendMessage("");
							break firstarg;
					}
				default:
					player.sendMessage(usage);

			}
		}else {
			player.sendMessage(usage);
			if(player.hasPermission("radio.admin"))
				player.sendMessage(usageAdmin);
		}
		return true;
	}

}
