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

import java.awt.event.KeyEvent;

public class RootWidget extends ConsoleHost {

	public static final Resource defcurs = Resource.local().loadwait("gfx/hud/curs/arw");
	Profile guprof, grprof, ggprof;

	public RootWidget(UI ui, Coord sz) {
		super(ui, new Coord(0, 0), sz);
		setfocusctl(true);
		cursor = defcurs.indir();
	}

	public boolean globtype(char key, KeyEvent ev) {
		if (!super.globtype(key, ev)) {
			int code = ev.getKeyCode();
			boolean CTRL = ui.modctrl;
			boolean ALT = ui.modmeta;
			if (key == '`') {
				GameUI gi = findchild(GameUI.class);
				if (Config.profile) {
					add(new Profwnd(guprof, "UI profile"), new Coord(100, 100));
					add(new Profwnd(grprof, "GL profile"), new Coord(450, 100));
					if ((gi != null) && (gi.map != null)) {
						add(new Profwnd(gi.map.prof, "Map profile"), new Coord(100, 250));
					}
				}
				if (Config.profilegpu) {
					add(new Profwnd(ggprof, "GPU profile"), new Coord(450, 250));
				}
			} else if (CTRL && code == KeyEvent.VK_1) {
				if (ui.gui != null) {
					ui.gui.eqproxy.activate(0);
				}
			} else if (CTRL && code == KeyEvent.VK_2) {
				if (ui.gui != null) {
					ui.gui.eqproxy.activate(1);
				}
			} else if (ALT && code == KeyEvent.VK_C) {
				if (ui.gui != null) {
					ui.gui.toggleCraftWnd();
				}
			} else if (CTRL && code == KeyEvent.VK_G) {
				if (ui.gui != null) {
					ui.gui.map.togglegrid();
				}
			} else if (key == ':') {
				entercmd();
			} else if (key != 0) {
				wdgmsg("gk", (int) key);
			}
		}
		return (true);
	}

	@Override
	public boolean mousedown(Coord c, int button) {
		return super.mousedown(c, button);
	}

	public void draw(GOut g) {
		super.draw(g);
		drawcmd(g, new Coord(20, sz.y - 20));
	}

	public void error(String msg) {
	}
}
