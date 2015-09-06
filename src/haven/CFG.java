package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public enum CFG {

	CAMERA_BRIGHT("camera.bright", Utils.getpreff("brightness", 0f)),
	DISPLAY_KINNAMES("display.kinnames", Utils.getprefb("showkinnames", true)),
	DISPLAY_FLAVOR("display.flavor", Utils.getprefb("showflavor", false)),
	FREE_CAMERA_ROTATION("general.freecamera", Utils.getprefb("freecamera", true)),
	STORE_MAP("general.storemap", Utils.getprefb("storemap", false)),
	STUDY_LOCK("ui.studylock", Utils.getprefb("studylock", false)),
	SHOW_CHAT_TIMESTAMP("ui.chat.timestamp", true),
	UI_MINIMAP_PLAYERS("ui.minimap.players", Utils.getprefb("showplayers", true)),
	UI_MINIMAP_BOULDERS("ui.minimap.boulders", Utils.getprefb("showboulders", true)),
	Q_SHOW_ALL_MODS("ui.q.allmods", 7),
	Q_SHOW_SINGLE("ui.q.showsingle", true),
	Q_MAX_SINGLE("ui.q.maxsingle", false);

	private static final String CONFIG_JSON = "config.json";
	private static final Map<String, Object> cfg;
	private static final Map<String, Object> cache = new HashMap<String, Object>();
	private static final Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
	private final String path;
	public final Object def;

	static {
		Map<String, Object> tmp = new HashMap<String, Object>();
		try {
			Type type = new TypeToken<Map<Object, Object>>() {
			}.getType();
			String json = Config.loadFile(CONFIG_JSON);
			if (json != null) {
				tmp = gson.fromJson(json, type);
			}
		} catch (Exception e) {
		}
		cfg = tmp;
	}

	CFG(String path, Object def) {
		this.path = path;
		this.def = def;
	}

	public Object val() {
		return CFG.get(this);
	}

	public boolean valb() {
		return CFG.getb(this);
	}

	public int vali() {
		return CFG.geti(this);
	}

	public float valf() {
		return CFG.getf(this);
	}

	public void set(Object value) {
		CFG.set(this, value);
	}

	public static synchronized Object get(CFG name) {
		if (cache.containsKey(name.path)) {
			return cache.get(name.path);
		} else {
			Object value = retrieve(name);
			cache.put(name.path, value);
			return value;
		}
	}

	public static boolean getb(CFG name) {
		return (Boolean) get(name);
	}

	public static int geti(CFG name) {
		return ((Number) get(name)).intValue();
	}

	public static float getf(CFG name) {
		return ((Number) get(name)).floatValue();
	}

	@SuppressWarnings("unchecked")
	public static synchronized void set(CFG name, Object value) {
		cache.put(name.path, value);
		String[] parts = name.path.split("\\.");
		int i;
		Object cur = cfg;
		for (i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			if (cur instanceof Map) {
				Map<Object, Object> map = (Map<Object, Object>) cur;
				if (map.containsKey(part)) {
					cur = map.get(part);
				} else {
					cur = new HashMap<String, Object>();
					map.put(part, cur);
				}
			}
		}
		if (cur instanceof Map) {
			Map<Object, Object> map = (Map) cur;
			map.put(parts[parts.length - 1], value);
		}
		store();
	}

	private static synchronized void store() {
		try {
			Config.saveFile(CONFIG_JSON, gson.toJson(cfg));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private static Object retrieve(CFG name) {
		String[] parts = name.path.split("\\.");
		Object cur = cfg;
		for (String part : parts) {
			if (cur instanceof Map) {
				Map map = (Map) cur;
				if (map.containsKey(part)) {
					cur = map.get(part);
				} else {
					return name.def;
				}
			} else {
				return name.def;
			}
		}
		return cur;
	}
}