/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */
package haven;

import static haven.MCache.cmaps;
import static haven.MCache.tilesz;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;
import haven.resutil.Ridges;

public class LocalMiniMap extends Widget {

	public final MapView mv;
	private Coord cc = null;
	private MapTile cur = null;
	private final Map<Coord, Defer.Future<MapTile>> cache = new LinkedHashMap<Coord, Defer.Future<MapTile>>(5, 0.75f, true) {
		protected boolean removeEldestEntry(Map.Entry<Coord, Defer.Future<MapTile>> eldest) {
			if (size() > 5) {
				try {
					MapTile t = eldest.getValue().get();
					t.img.dispose();
				} catch (RuntimeException e) {
				}
				return (true);
			}
			return (false);
		}
	};

	public static class MapTile {

		public final Tex img;
		public final Coord ul, c;

		public MapTile(Tex img, Coord ul, Coord c) {
			this.img = img;
			this.ul = ul;
			this.c = c;
		}
	}

	private BufferedImage tileimg(int t, BufferedImage[] texes) {
		BufferedImage img = texes[t];
		if (img == null) {
			Resource r = ui.sess.glob.map.tilesetr(t);
			if (r == null) {
				return (null);
			}
			Resource.Image ir = r.layer(Resource.imgc);
			if (ir == null) {
				return (null);
			}
			img = ir.img;
			texes[t] = img;
		}
		return (img);
	}

	public BufferedImage drawmap(Coord ul, Coord sz) {
		BufferedImage[] texes = new BufferedImage[256];
		MCache m = ui.sess.glob.map;
		BufferedImage buf = TexI.mkbuf(sz);
		Coord c = new Coord();
		for (c.y = 0; c.y < sz.y; c.y++) {
			for (c.x = 0; c.x < sz.x; c.x++) {
				int t = m.gettile(ul.add(c));
				BufferedImage tex = tileimg(t, texes);
				int rgb = 0;
				if (tex != null) {
					rgb = tex.getRGB(Utils.floormod(c.x + ul.x, tex.getWidth()),
									Utils.floormod(c.y + ul.y, tex.getHeight()));
				}
				buf.setRGB(c.x, c.y, rgb);
			}
		}
		for (c.y = 1; c.y < sz.y - 1; c.y++) {
			for (c.x = 1; c.x < sz.x - 1; c.x++) {
				int t = m.gettile(ul.add(c));
				Tiler tl = m.tiler(t);
				if (tl instanceof Ridges.RidgeTile) {
					if (Ridges.brokenp(m, ul.add(c))) {
						for (int y = c.y - 1; y <= c.y + 1; y++) {
							for (int x = c.x - 1; x <= c.x + 1; x++) {
								Color cc = new Color(buf.getRGB(x, y));
								buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y)) ? 1 : 0.1).getRGB());
							}
						}
					}
				}
			}
		}
		for (c.y = 0; c.y < sz.y; c.y++) {
			for (c.x = 0; c.x < sz.x; c.x++) {
				int t = m.gettile(ul.add(c));
				if ((m.gettile(ul.add(c).add(-1, 0)) > t)
								|| (m.gettile(ul.add(c).add(1, 0)) > t)
								|| (m.gettile(ul.add(c).add(0, -1)) > t)
								|| (m.gettile(ul.add(c).add(0, 1)) > t)) {
					buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
				}
			}
		}
		return (buf);
	}

	public LocalMiniMap(Coord sz, MapView mv) {
		super(sz);
		this.mv = mv;
	}

	public Coord p2c(Coord pc) {
		return (pc.div(tilesz).sub(cc).add(sz.div(2)));
	}

	public Coord c2p(Coord c) {
		return (c.sub(sz.div(2)).add(cc).mul(tilesz).add(tilesz.div(2)));
	}

	public void drawicons(GOut g) {
		OCache oc = ui.sess.glob.oc;
		synchronized (oc) {
			for (Gob gob : oc) {
				try {
					GobIcon icon = gob.getattr(GobIcon.class);
					Coord gc = p2c(gob.rc);
					if (icon != null) {
						Tex tex = icon.tex();
						g.image(tex, gc.sub(tex.sz().div(2)));
					} else {
						Resource res = gob.getres();
						if (res != null) {
							if (CFG.UI_MINIMAP_PLAYERS.valb()) {
								if ("body".equals(res.basename()) && gob.id != mv.player().id) {
									g.chcolor(Color.BLACK);
									g.fellipse(gc, new Coord(6, 6));
									KinInfo kininfo = gob.getattr(KinInfo.class);
									g.chcolor(kininfo != null ? BuddyWnd.gc[kininfo.group] : Color.WHITE);
									g.fellipse(gc, new Coord(5, 5));
									g.chcolor();
								}
							}
							if (CFG.UI_MINIMAP_BOULDERS.valb()) {
								if (res.name.contains("bumlings")) {
									g.chcolor(Color.BLACK);
									g.fellipse(gc, new Coord(4, 4));
									g.chcolor(Color.LIGHT_GRAY);
									g.fellipse(gc, new Coord(3, 3));
									g.chcolor();
								}
							}
						}
					}
				} catch (Loading l) {
				}
			}
		}
	}

	public Gob findicongob(Coord c) {
		OCache oc = ui.sess.glob.oc;
		synchronized (oc) {
			for (Gob gob : oc) {
				try {
					GobIcon icon = gob.getattr(GobIcon.class);
					if (icon != null) {
						Coord gc = p2c(gob.rc);
						Coord sz = icon.tex().sz();
						if (c.isect(gc.sub(sz.div(2)), sz)) {
							return (gob);
						}
					}
				} catch (Loading l) {
				}
			}
		}
		return (null);
	}

	public void tick(double dt) {
		Gob pl = ui.sess.glob.oc.getgob(mv.plgob);
		if (pl == null) {
			this.cc = null;
			return;
		}
		this.cc = pl.rc.div(tilesz);
	}

	public void draw(GOut g) {
		if (cc == null) {
			return;
		}
		final Coord plg = cc.div(cmaps);
		if ((cur == null) || !plg.equals(cur.c)) {
			Defer.Future<MapTile> f;
			synchronized (cache) {
				f = cache.get(plg);
				if (f == null) {
					f = Defer.later(new Defer.Callable<MapTile>() {
						public MapTile call() {
							Coord ul = plg.mul(cmaps).sub(cmaps).add(1, 1);
							return (new MapTile(new TexI(drawmap(ul, cmaps.mul(3).sub(2, 2))), ul, plg));
						}
					});
					cache.put(plg, f);
				}
			}
			if (f.done()) {
				cur = f.get();
			}
		}
		if (cur != null) {
			g.image(MiniMap.bg, Coord.z);
			g.image(cur.img, cur.ul.sub(cc).add(sz.div(2)));
			try {
				synchronized (ui.sess.glob.party.memb) {
					for (Party.Member m : ui.sess.glob.party.memb.values()) {
						if (CFG.UI_MINIMAP_PLAYERS.valb()) {
							Gob pl = mv.player();
							if (pl != null && m.gobid != pl.id) {
								continue;
							}
						}
						Coord ptc;
						try {
							ptc = m.getc();
						} catch (MCache.LoadingMap e) {
							ptc = null;
						}
						if (ptc == null) {
							continue;
						}
						ptc = p2c(ptc);
						g.chcolor(m.col.getRed(), m.col.getGreen(), m.col.getBlue(), 128);
						g.image(MiniMap.plx.layer(Resource.imgc).tex(), ptc.add(MiniMap.plx.layer(Resource.negc).cc.inv()));
						g.chcolor();
					}
				}
			} catch (Loading l) {
			}
		} else {
			g.image(MiniMap.nomap, Coord.z);
		}
		drawicons(g);
	}

	public boolean mousedown(Coord c, int button) {
		if (cc == null) {
			return (false);
		}
		Gob gob = findicongob(c);
		if (gob == null) {
			mv.wdgmsg("click", rootpos().add(c), c2p(c), button, ui.modflags());
		} else {
			mv.wdgmsg("click", rootpos().add(c), c2p(c), button, ui.modflags(), 0, (int) gob.id, gob.rc, 0, -1);
		}
		return (true);
	}
}
