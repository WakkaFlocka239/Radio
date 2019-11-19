package me.wakka.radio;

import com.xxmicloxx.NoteBlockAPI.model.*;
import com.xxmicloxx.NoteBlockAPI.songplayer.*;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Radio extends JavaPlugin {
	private static Radio instance;

	private final File SONGS_DIR = new File(getDataFolder() + "/songs");
	private static File[] allSongFiles;

	static RadioSongPlayer serverRadio;
	private static List<Player> serverRadioListeners = new ArrayList<>();

	static List<PositionSongPlayer> radiusRadios = new ArrayList<>();

	private static FileConfiguration serverRadioFC;
	private static FileConfiguration radiusRadioFC;

	public Radio(){
		if (instance == null){
			instance = this;
		}else{
			throw new IllegalStateException();
		}
	}

	@Override
	public void onEnable(){
		// NoteBlockAPI
		boolean NoteBlockAPI = true;
		if (!Bukkit.getPluginManager().isPluginEnabled("NoteBlockAPI")){
			getLogger().severe("*** NoteBlockAPI is not installed or not enabled. ***");
			NoteBlockAPI = false;
			return;
		}

		// Create Files
		try{
			createFiles();
		} catch (Exception e){
			e.printStackTrace();
		}

		// Register Listeners
		getServer().getPluginManager().registerEvents(new Listeners(), this);

		// Setup Command Executor
		Objects.requireNonNull(this.getCommand("radio")).setExecutor(new Command());

		// Load All Songs
		allSongFiles = SONGS_DIR.listFiles();

		// Setup Radius Radios
		setupRadiusRadios();

		// Setup Server Radio
		setupServerRadio();

	}

	@Override
	public void onDisable() {
		super.onDisable();
		Set<UUID> uuids = serverRadio.getPlayerUUIDs();
		for (UUID uuid: uuids) {
			serverRadio.removePlayer(uuid);
		}
	}

	private static File[] getAllSongFiles(){
		return allSongFiles;
	}

	private void setupServerRadio(){
		// Load Songs From Config
		File[] serverRadioSongs = getServerRadioSongs();
		//
		Song song;
		String songPath;
		int songCount;
		if (serverRadioSongs != null) {
			songCount = serverRadioSongs.length;

			File[] songFiles = Utils.shuffleArray(serverRadioSongs);
			Song[] songList = new Song[songCount];

			for (int i = 0; i < songCount; i++) {
				if (serverRadioSongs[i] == null) {
					getLogger().severe("Song index " + i + " in server-radio.yml is invalid.");
				}

				songPath = songFiles[i].getPath();
				song = NBSDecoder.parse(new File(songPath));
				songList[i] = song;
			}
			Playlist playlist = new Playlist(songList);
			serverRadio = new RadioSongPlayer(playlist);
			setRadioDefaults(serverRadio);
		}else{
			getLogger().severe("Server Radio playlist is empty!");
		}
	}

	private File[] getServerRadioSongs(){
		File[] allAsList = getAllSongFiles();
		List<String> songFiles = serverRadioFC.getStringList("playlist");
		int count = songFiles.size();
		Bukkit.getLogger().info("<ServerRadio> " + count + " Loaded Songs: " + songFiles.toString());

		// If song in config == loaded song, add song to array
		int i = 0;
		File[] arr = new File[count];

		for (String songFile: songFiles){
			for (File song: allAsList) {
				if (song.getName().equals(songFile)) {
					arr[i] = song;
					break;
				}
			}
			i++;
		}
		return arr;
	}

	private void setupRadiusRadios() {
		Set<String> radios = radiusRadioFC.getConfigurationSection("radius_radios.").getKeys(false);

		for (String radioName : radios) {
			boolean enable = radiusRadioFC.getBoolean("radius_radios." + radioName + ".enabled");
			if(!enable) {
				continue;
			}
			int x = radiusRadioFC.getInt("radius_radios." + radioName + ".location.x");
			int y = radiusRadioFC.getInt("radius_radios." + radioName + ".location.y");
			int z = radiusRadioFC.getInt("radius_radios." + radioName + ".location.z");
			String world = radiusRadioFC.getString("radius_radios." + radioName + ".location.world");
			if(world == null) world = "world";
			int radius = radiusRadioFC.getInt("radius_radios." + radioName + ".radius");

			File[] radiusRadioSongs = getRadiusRadioSongs(radioName);
			Song song;
			String songPath;
			int songCount;
			if (radiusRadioSongs != null && radiusRadioSongs.length > 0) {
				songCount = radiusRadioSongs.length;

				File[] songFiles = Utils.shuffleArray(radiusRadioSongs);
				Song[] songList = new Song[songCount];

				for (int i = 0; i < songCount; i++) {
					if (radiusRadioSongs[i] == null) {
						getLogger().severe("Song index " + i + " for " + radioName + " in radius-radio.yml is invalid.");
					}

					songPath = songFiles[i].getPath();
					song = NBSDecoder.parse(new File(songPath));
					songList[i] = song;
				}

				Playlist playlist = new Playlist(songList);
				PositionSongPlayer radio = new PositionSongPlayer(playlist);
				radio.setTargetLocation(new Location(getServer().getWorld(world), x, y, z));
				radio.setDistance(radius);
				setRadioDefaults(radio);

				// Add all online players to the radius radio
				Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
				for (Player player : onlinePlayers) {
					radio.addPlayer(player);
				}

				radiusRadios.add(radio);
			}
		}
	}

	private File[] getRadiusRadioSongs(String radioName){
		File[] allAsList = getAllSongFiles();
		List<String> songFiles = radiusRadioFC.getStringList("radius_radios." + radioName + ".playlist");
		int count = songFiles.size();
		Bukkit.getLogger().info("<"+radioName+"> " + count + " Loaded Songs: " + songFiles.toString());

		// If song in config == loaded song, add song to array
		int i = 0;
		File[] arr = new File[count];

		for (String songFile: songFiles){
			for (File song: allAsList) {
				if (song.getName().equals(songFile)) {
					arr[i] = song;
					break;
				}
			}
			i++;
		}
		return arr;

	}

	static List<PositionSongPlayer> getRadiusRadios() {
		return radiusRadios;
	}

	private void setRadioDefaults(SongPlayer radio){
		if(radio instanceof  RadioSongPlayer){
			((RadioSongPlayer) radio).setStereo(true);
		}

		Fade fadeIn = radio.getFadeIn();
		fadeIn.setType(FadeType.LINEAR);
		fadeIn.setFadeDuration(60); //in ticks

		Fade fadeOut = radio.getFadeOut();
		fadeOut.setType(FadeType.LINEAR);
		fadeOut.setFadeDuration(60); //in ticks

		radio.setRandom(true);
		radio.setRepeatMode(RepeatMode.ALL);

		radio.setCategory(SoundCategory.RECORDS);
		radio.setPlaying(true);
	}

	static void addPlayer(Player player, SongPlayer radio){
		if(!Utils.isListening(player, radio)){
			radio.addPlayer(player);
			Utils.actionBarMessage(player, radio.getSong().getTitle(), false);
		}
	}

	static void removePlayer(Player player, SongPlayer radio){
		if(Utils.isListening(player, radio)) {
			radio.removePlayer(player);
		}else{
			player.sendMessage(Command.PREFIX + " §cYou are not listening to a radio!");
		}
	}

	// These three methods control the server radio listeners list
	// Used for turning back on the server radio when exiting a radius radio,
	// 		ONLY IF they had it on before entering.
	static void addServerRadioListener(Player player){
		if(!isServerRadioListener(player)) {
			serverRadioListeners.add(player);
		}
	}

	static void removeServerRadioListener(Player player){
		if(isServerRadioListener(player)) {
			serverRadioListeners.remove(player);
		}
	}

	static boolean isServerRadioListener(Player player){
		return serverRadioListeners.contains(player);
	}
	//

	private void createFiles() throws IOException, InvalidConfigurationException {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}

		File songDir = new File(instance.getDataFolder() + "/songs");
		if (!songDir.exists())
			if (!songDir.mkdir())
				getLogger().severe("&cCould not generate songs directory!");


		File serverRadioF = new File(getDataFolder(), "server-radio.yml");
		serverRadioFC = new YamlConfiguration();
		File radiusRadioF = new File(getDataFolder(), "radius-radio.yml");
		radiusRadioFC = new YamlConfiguration();

		if (!serverRadioF.exists()) {
			getLogger().info("server-radio.yml not found, creating!");
			if (serverRadioF.createNewFile()){
				List<String> defaultSongs = new ArrayList<>();
				defaultSongs.add("Frosty_The_Snowman.nbs");
				defaultSongs.add("Let_It_Snow.nbs");
				serverRadioFC.set("playlist", defaultSongs);
				serverRadioFC.save(serverRadioF);
				getLogger().info("server-radio.yml generated.");
			}else{
				getLogger().severe("&cCould not generate server-radio.yml");
			}
		}

		if (!radiusRadioF.exists()) {
			getLogger().info("radius-radio.yml not found, creating!");
			if (radiusRadioF.createNewFile()){
				radiusRadioFC.set("radius_radios.spawn.enabled", true);
				radiusRadioFC.set("radius_radios.spawn.location.x", -2);
				radiusRadioFC.set("radius_radios.spawn.location.y", 128);
				radiusRadioFC.set("radius_radios.spawn.location.z", -20);
				radiusRadioFC.set("radius_radios.spawn.location.world", "world");
				radiusRadioFC.set("radius_radios.spawn.radius", 200);

				radiusRadioFC.set("radius_radios.pugmas.enabled", true);
				radiusRadioFC.set("radius_radios.pugmas.location.x", 462);
				radiusRadioFC.set("radius_radios.pugmas.location.y", 128);
				radiusRadioFC.set("radius_radios.pugmas.location.z", 1054);
				radiusRadioFC.set("radius_radios.pugmas.location.world", "world");
				radiusRadioFC.set("radius_radios.pugmas.radius", 300);

				List<String> defaultSongs = new ArrayList<>();
				defaultSongs.add("Frosty_The_Snowman.nbs");
				defaultSongs.add("Let_It_Snow.nbs");
				radiusRadioFC.set("radius_radios.spawn.playlist", defaultSongs);

				defaultSongs = new ArrayList<>();
				defaultSongs.add("Frosty_The_Snowman.nbs");
				defaultSongs.add("Let_It_Snow.nbs");
				radiusRadioFC.set("radius_radios.pugmas.playlist", defaultSongs);

				radiusRadioFC.save(radiusRadioF);

				getLogger().info("radius-radio.yml generated.");
			}else{
				getLogger().severe("&cCould not generate server-radio.yml");
			}
		}

		serverRadioFC.load(serverRadioF);
		radiusRadioFC.load(radiusRadioF);
	}

}
