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

import java.util.*;

public class Inventory extends Widget implements DTarget {

	public static final Tex invsq = Resource.loadtex("gfx/hud/invsq");
	public static final Coord sqsz = new Coord(33, 33);
	Coord isz;
	Map<GItem, WItem> wmap = new HashMap<GItem, WItem>();

	@RName("inv")
	public static class $_ implements Factory {

		public Widget create(Widget parent, Object[] args) {
			return (new Inventory((Coord) args[0]));
		}
	}

	public void draw(GOut g) {
		Coord c = new Coord();
		for (c.y = 0; c.y < isz.y; c.y++) {
			for (c.x = 0; c.x < isz.x; c.x++) {
				g.image(invsq, c.mul(sqsz));
			}
		}
		super.draw(g);
	}

	public Inventory(Coord sz) {
		super(invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1)));
		isz = sz;
	}

	public boolean mousewheel(Coord c, int amount) {
		if (ui.modshift) {
			Inventory minv = getparent(GameUI.class).maininv;
			if (minv != this) {
				if (amount < 0) {
					wdgmsg("invxf", minv.wdgid(), 1);
				} else if (amount > 0) {
					minv.wdgmsg("invxf", this.wdgid(), 1);
				}
			}
		}
		return (true);
	}

	public void addchild(Widget child, Object... args) {
		add(child);
		Coord c = (Coord) args[0];
		if (child instanceof GItem) {
			GItem i = (GItem) child;
			wmap.put(i, add(new WItem(i), c.mul(sqsz).add(1, 1)));
		}
	}

	public void cdestroy(Widget w) {
		super.cdestroy(w);
		if (w instanceof GItem) {
			GItem i = (GItem) w;
			ui.destroy(wmap.remove(i));
		}
	}

	public boolean drop(Coord cc, Coord ul) {
		wdgmsg("drop", ul.add(sqsz.div(2)).div(invsq.sz()));
		return (true);
	}

	public boolean iteminteract(Coord cc, Coord ul) {
		return (false);
	}

	public void uimsg(String msg, Object... args) {
		if (msg.equals("sz")) {
			isz = (Coord) args[0];
			resize(invsq.sz().add(new Coord(-1, -1)).mul(isz).add(new Coord(1, 1)));
		}
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if (msg.equals("transfer-same")) {
			process(getSame((String) args[0], (Boolean) args[1]), "transfer");
		} else if (msg.equals("drop-same")) {
			process(getSame((String) args[0], (Boolean) args[1]), "drop");
		} else {
			super.wdgmsg(sender, msg, args);
		}
	}

	private void process(List<WItem> items, String action) {
		for (WItem item : items) {
			item.item.wdgmsg(action, Coord.z);
		}
	}

	@SuppressWarnings("UnusedParameters")
	private List<WItem> getSame(String name, Boolean ascending) {
		List<WItem> items = new ArrayList<WItem>();
		for (Widget wdg = lchild; wdg != null; wdg = wdg.prev) {
			if (wdg.visible && wdg instanceof WItem) {
				if (((WItem) wdg).item.resname().equals(name)) {
					items.add((WItem) wdg);
				}
			}
		}
		return items;
	}

	public static Coord invsz(Coord sz) {
		return invsq.sz().add(new Coord(-1, -1)).mul(sz).add(new Coord(1, 1));
	}

	public static Coord sqroff(Coord c) {
		return c.div(invsq.sz());
	}

	public static Coord sqoff(Coord c) {
		return c.mul(invsq.sz());
	}
}
