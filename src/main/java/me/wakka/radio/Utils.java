package me.wakka.radio;

import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static me.wakka.radio.Radio.serverRadio;

class Utils {
	static void actionBarMessage(Player p, String songTitle, boolean nowPlaying){
		String message = "§2§lCurrently Playing: §f";
		if(nowPlaying) message = "§2§lNow Playing: §f";
		message += " " + songTitle;
		p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
	}

	static boolean isListening(Player player, SongPlayer radio){
		Set<UUID> uuids = radio.getPlayerUUIDs();
		if(!uuids.isEmpty())
			return uuids.contains(player.getUniqueId());
		return false;
	}

	static SongPlayer getListenedRadio(Player player){
		if(isListening(player, serverRadio)){
			return serverRadio;
		}else{
			List<PositionSongPlayer> radiusRadios = Radio.getRadiusRadios();
			for (PositionSongPlayer radio: radiusRadios) {
				if(isListening(player, radio) && isInRangeOfRadiusRadio(player, radio)){
					return radio;
				}
			}
		}
		return null;
	}

	static boolean isInRangeOfRadiusRadio(Player player, PositionSongPlayer radio) {
		return radio.isInRange(player);
	}

	static boolean isInRangeOfAnyRadiusRadio(Player player) {
		for (PositionSongPlayer radiusRadio : Radio.radiusRadios) {
			if(radiusRadio.isInRange(player)){
				return true;
			}
		}
		return false;
	}

	static File[] shuffleArray(File[] arr){
		Random rgen = new Random();
		for (int i=0; i<arr.length; i++) {
			int randomPosition = rgen.nextInt(arr.length);
			File temp = arr[i];
			arr[i] = arr[randomPosition];
			arr[randomPosition] = temp;
		}
		return arr;
	}

	static Player getOnlinePlayerByUuid(UUID uuid) {
		for(Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getUniqueId().equals(uuid)){
				return p;
			}
		}
		return null;
	}

}
