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
import haven.Audio.CS;
import haven.Audio.VolAdjust;

public class ClipAmbiance implements Rendered {

	public final Desc desc;
	public double bvol;
	private Glob glob = null;

	public ClipAmbiance(Desc desc) {
		this.desc = desc;
		this.bvol = desc.bvol;
	}

	public static class Glob implements ActAudio.Global {

		public final Desc desc;
		private boolean dead = false;
		private Desc[] chans = {null};
		private VolAdjust[][] cur = {null};
		private int curn, ns;
		private int[] n = {0};
		private double vacc, cvol;
		private double lastupd = System.currentTimeMillis() / 1000.0;

		public Glob(Desc desc) {
			this.desc = desc;
		}

		public int hashCode() {
			return (desc.hashCode());
		}

		public boolean equals(Object other) {
			return ((other instanceof Glob) && (((Glob) other).desc == this.desc));
		}

		private void addclip(final int chan, final int idx) {
			Resource.Audio clip = AudioSprite.randoom(chans[chan].getres(), chans[chan].cnms[idx]);
			final VolAdjust[] clist = cur[chan];
			synchronized (this) {
				clist[idx] = new VolAdjust(new Audio.Monitor(clip.stream()) {
					protected void eof() {
						synchronized (Glob.this) {
							clist[idx] = null;
							curn--;
						}
					}
				}, 0.0);
				curn++;
			}
		}

		private void addmin() {
			while ((curn < desc.minc) && (chans[0] != null)) {
				double wsum = 0.0;
				for (int i = 0; (i < chans.length) && (chans[i] != null); i++) {
					for (int o = 0; o < chans[i].cnms.length; o++) {
						if (cur[i][o] == null) {
							wsum += chans[i].ieps[o];
						}
					}
				}
				double p = Math.random() * wsum;
				for (int i = 0; (i < chans.length) && (chans[i] != null); i++) {
					for (int o = 0; o < chans[i].cnms.length; o++) {
						if (cur[i][o] != null) {
							continue;
						}
						if ((p -= chans[i].ieps[o]) <= 0) {
							addclip(i, o);
							break;
						}
					}
				}
			}
		}

		private void addsome(double td) {
			if (curn >= desc.maxc) {
				return;
			}
			for (int i = 0; (i < chans.length) && (chans[i] != null); i++) {
				for (int o = 0; o < chans[i].cnms.length; o++) {
					if ((cur[i][o] != null) || (n[i] < 1)) {
						continue;
					}
					if (Math.random() < ((chans[i].ieps[o] * td * Math.min(ns, desc.maxi)) / desc.maxi)) {
						addclip(i, o);
						return;
					}
				}
			}
		}

		private boolean playing(int ch) {
			for (int i = 0; i < cur[ch].length; i++) {
				if (cur[ch][i] != null) {
					return (true);
				}
			}
			return (false);
		}

		private void trim() {
			int i = 0, o = 0;
			for (; (i < chans.length) && (chans[i] != null); i++) {
				if ((n[i] > 0) || playing(i)) {
					chans[o] = chans[i];
					cur[o] = cur[i];
					n[o] = n[i];
					o++;
				}
			}
			for (; o < chans.length; o++) {
				chans[o] = null;
				cur[o] = null;
				n[o] = 0;
			}
		}

		public boolean cycle(ActAudio list) {
			double now = System.currentTimeMillis() / 1000.0;
			double td = Math.max(now - lastupd, 0.0);
			trim();
			addmin();
			addsome(td);
			if (vacc < cvol) {
				cvol = Math.max(cvol - (td * 0.5), 0.0);
			} else if (vacc > cvol) {
				cvol = Math.min(cvol + (td * 0.5), 1.0);
			}
			if ((ns == 0) && (cvol < 0.005)) {
				dead = true;
				return (true);
			}
			vacc = 0.0;
			ns = 0;
			for (int i = 0; i < n.length; i++) {
				n[i] = 0;
			}
			lastupd = now;
			for (int i = 0; (i < cur.length) && (cur[i] != null); i++) {
				for (VolAdjust clip : cur[i]) {
					if (clip == null) {
						continue;
					}
					clip.vol = cvol;
					list.amb.add(clip);
				}
			}
			return (false);
		}

		public void add(Desc ch, double vol) {
			int i;
			for (i = 0; i < chans.length; i++) {
				if ((chans[i] == null) || (chans[i] == ch)) {
					break;
				}
			}
			if (i == chans.length) {
				int nn = chans.length * 2;
				chans = Utils.extend(chans, nn);
				cur = Utils.extend(cur, nn);
				n = Utils.extend(n, nn);
			}
			if (chans[i] == null) {
				chans[i] = ch;
				cur[i] = new VolAdjust[ch.cnms.length];
				n[i] = 0;
			}
			vacc += vol;
			n[i]++;
			ns++;
		}
	}

	public void draw(GOut g) {
		g.apply();
		if ((glob == null) || glob.dead) {
			ActAudio list = g.st.cur(ActAudio.slot);
			if (list == null) {
				return;
			}
			try {
				glob = list.intern(new Glob(desc.parent.get().layer(Desc.class)));
			} catch (Loading l) {
				return;
			}
		}
		Coord3f pos = PView.mvxf(g).mul4(Coord3f.o);
		double pd = Math.sqrt((pos.x * pos.x) + (pos.y * pos.y));
		double svol = Math.min(1.0, 50.0 / pd);
		glob.add(desc, svol * bvol);
	}

	public boolean setup(RenderList rl) {
		return (true);
	}

	@Resource.LayerName("clamb")
	public static class Desc extends Resource.Layer {

		public final Indir<Resource> parent;
		public final int minc, maxc, maxi;
		public final double bvol;
		public final String[] cnms;
		public final double[] ieps;

		public Desc(Resource res, Message buf) {
			res.super();
			int ver = buf.uint8();
			if ((ver < 1) || (ver > 2)) {
				throw (new Resource.LoadException("Unknown clip-ambiance version: " + ver, getres()));
			}
			if (ver >= 2) {
				String pnm = buf.string();
				if (pnm.length() == 0) {
					parent = res.indir();
				} else {
					parent = res.pool.load(pnm, buf.uint16());
				}
			} else {
				parent = res.indir();
			}
			minc = buf.uint8();
			maxc = buf.uint8();
			maxi = buf.uint16();
			bvol = buf.float32();
			cnms = new String[buf.uint8()];
			ieps = new double[cnms.length];
			for (int i = 0; i < cnms.length; i++) {
				cnms[i] = buf.string().intern();
				ieps[i] = 1.0 / buf.float32();
			}
		}

		public void init() {
		}
	}
}
