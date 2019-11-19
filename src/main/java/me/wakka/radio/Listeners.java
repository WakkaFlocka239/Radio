package me.wakka.radio;

import com.xxmicloxx.NoteBlockAPI.event.PlayerRangeStateChangeEvent;
import com.xxmicloxx.NoteBlockAPI.event.SongNextEvent;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

public class Listeners implements Listener {

	@EventHandler
	public void onSongNext(SongNextEvent e){
		SongPlayer sp = e.getSongPlayer();
		PositionSongPlayer radiusRadio = null;
		boolean isInRange = true;
		if(sp instanceof PositionSongPlayer){
			radiusRadio = (PositionSongPlayer) sp;
		}
		String song = sp.getSong().getTitle();
		Set<UUID> UUIDList = sp.getPlayerUUIDs();
		for (UUID uuid: UUIDList) {
			Player player = Utils.getOnlinePlayerByUuid(uuid);
			if(player != null) {
				if (radiusRadio != null) {
					isInRange = radiusRadio.isInRange(player);
				}
				if (isInRange) {
					Utils.actionBarMessage(player, song, true);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e){
		Player player = e.getPlayer();
		SongPlayer radio = Utils.getListenedRadio(player);
		if(radio != null) {
			Radio.removePlayer(player, radio);
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e){
		Player player = e.getPlayer();
		for (PositionSongPlayer radio: Radio.radiusRadios) {
			Radio.addPlayer(player, radio);
		}
	}

	@EventHandler
	public void onRangeChange(PlayerRangeStateChangeEvent e){
		Player player = e.getPlayer();
		SongPlayer radiusRadio = e.getSongPlayer();
		boolean isInRange = e.isInRange();
		SongPlayer listenedRadio = Utils.getListenedRadio(player);

		if(listenedRadio == Radio.serverRadio && isInRange){
			// Make the player leave the server radio if they are listening, and join the radius radio
			Radio.removePlayer(player, Radio.serverRadio);
			Radio.addPlayer(player, radiusRadio);
			Utils.actionBarMessage(player, radiusRadio.getSong().getTitle(), false);
		}else if(isInRange){
			// Add the player to the radius radio, just incase.
			Radio.addPlayer(player, radiusRadio);
			Utils.actionBarMessage(player, radiusRadio.getSong().getTitle(), false);
		}else if(Radio.isServerRadioListener(player)){
			// If player had radio on before entering a radius radio, turn it back on
			Radio.addPlayer(player, Radio.serverRadio);
		}
	}

}
