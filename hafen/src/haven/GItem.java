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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;

public class GItem extends AWidget implements ItemInfo.SpriteOwner, GSprite.Owner {

	public Indir<Resource> res;
	public MessageBuf sdt;
	public int meter = 0;
	public int num = -1;
	private GSprite spr;
	private Object[] rawinfo;
	private List<ItemInfo> info = Collections.emptyList();

	// meter time estimation
	public class MeterTime {

		private int current = -1;
		private int previous = -1;

		private Date lastChange = null;

		private final List<Double> meterPerSecList = new ArrayList<Double>();
		private Integer count = -1;

		private double calculateAverage() {
			return calculateAverage(meterPerSecList);
		}

		private double calculateAverage(List<Double> values) {
			Double sum = 0.0;
			if (!values.isEmpty()) {
				for (Double value : values) {
					sum += value;
				}
				return sum / values.size();
			}
			return sum;
		}

		public void update(int meter, GameUI gui) {
			Date meterPrevUpdate = lastChange;

			previous = current;
			current = meter;

			if (previous != meter) {
				if (current == 0) {
					count = -1;
					meterPrevUpdate = null;
					meterPerSecList.clear();
				} else {
					lastChange = new Date();
				}
				if (meterPrevUpdate != null) {
					if (count++ <= 0) {
					} else {
						int a = current - previous;

						long secondsSinceUpdate = (lastChange.getTime() - meterPrevUpdate.getTime()) / 1000;
						double meterPerSec = a * secondsSinceUpdate;

						meterPerSecList.add(meterPerSec);
						double averageSeconds = calculateAverage();

						long totalSecs = (long) ((double) (100 - meter) * averageSeconds);
						long hours = totalSecs / 3600;
						long minutes = (totalSecs % 3600) / 60;
						long seconds = totalSecs % 60;
						gui.syslog.append(String.format("[%s] avgSecs: %.2f | time remaining: %dh, %dm, %ds", resname(), averageSeconds, hours, minutes, seconds), Color.GRAY);
					}
				}
			}
		}
	}
	private final MeterTime meterTime = new MeterTime();

	@RName("item")
	public static class $_ implements Factory {

		public Widget create(Widget parent, Object[] args) {
			int res = (Integer) args[0];
			Message sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : Message.nil;
			return (new GItem(parent.ui.sess.getres(res), sdt));
		}
	}

	public interface ColorInfo {

		public Color olcol();
	}

	public interface NumberInfo {

		public int itemnum();
	}

	public class Amount extends ItemInfo implements NumberInfo {

		private final int num;

		public Amount(int num) {
			super(GItem.this);
			this.num = num;
		}

		public int itemnum() {
			return (num);
		}
	}

	public GItem(Indir<Resource> res, Message sdt) {
		this.res = res;
		this.sdt = new MessageBuf(sdt);
	}

	public GItem(Indir<Resource> res) {
		this(res, Message.nil);
	}

	private Random rnd = null;

	public Random mkrandoom() {
		if (rnd == null) {
			rnd = new Random();
		}
		return (rnd);
	}

	public Resource getres() {
		return (res.get());
	}

	public Glob glob() {
		return (ui.sess.glob);
	}

	public GSprite spr() {
		GSprite spr = this.spr;
		if (spr == null) {
			try {
				spr = this.spr = GSprite.create(this, res.get(), sdt.clone());
			} catch (Loading l) {
			}
		}
		return (spr);
	}

	public String resname() {
		Resource res = resource();
		if (res != null) {
			return res.name;
		}
		return "";
	}

	public void tick(double dt) {
		GSprite spr = spr();
		if (spr != null) {
			spr.tick(dt);
		}
	}

	public List<ItemInfo> info() {
		if (info == null) {
			info = ItemInfo.buildinfo(this, rawinfo);
		}
		return (info);
	}

	public Resource resource() {
		return (res.get());
	}

	public GSprite sprite() {
		if (spr == null) {
			throw (new Loading("Still waiting for sprite to be constructed"));
		}
		return (spr);
	}

	public void uimsg(String name, Object... args) {
		if (name == "num") {
			num = (Integer) args[0];
		} else if (name == "chres") {
			synchronized (this) {
				res = ui.sess.getres((Integer) args[0]);
				sdt = (args.length > 1) ? new MessageBuf((byte[]) args[1]) : MessageBuf.nil;
				spr = null;
			}
		} else if (name == "tt") {
			info = null;
			rawinfo = args;
		} else if (name == "meter") {
			meter = (Integer) args[0];
			meterTime.update(meter, ui.gui);
		}
	}
}
