package haven;

import static haven.Inventory.*;
import static haven.Inventory.sqoff;
import java.awt.Color;

public class EquipProxy extends Widget implements DTarget {

	public static final Color BG_COLOR = new Color(91, 128, 51, 202);
	private int[] slots;

	public EquipProxy(int[] slots) {
		super();
		setSlots(slots);
	}

	public void setSlots(int[] slots) {
		this.slots = slots;
		sz = invsz(new Coord(slots.length, 1));
	}

	private int slot(Coord c) {
		int slot = sqroff(c).x;
		if (slot < 0) {
			slot = 0;
		}
		if (slot >= slots.length) {
			slot = slots.length - 1;
		}
		return slots[slot];
	}

	@Override
	public boolean mousedown(Coord c, int button) {
		Equipory e = ui.gui.getEquipory();
		if (e != null) {
			WItem w = e.slots[slot(c)];
			if (w != null) {
				w.mousedown(Coord.z, button);
				return true;
			}
		}
		return false;
	}

	@Override
	public void draw(GOut g) {
		super.draw(g);
		Equipory e = ui.gui.getEquipory();
		if (e != null) {
			int k = 0;
			g.chcolor(BG_COLOR);
			g.frect(Coord.z, sz);
			g.chcolor();
			Coord c0 = new Coord(0, 0);
			for (int slot : slots) {
				c0.x = k;
				Coord c1 = sqoff(c0);
				g.image(invsq, c1);
				WItem w = e.slots[slot];
				if (w != null) {
					w.draw(g.reclipl(c1, g.sz));
				}
				k++;
			}
		}
	}

	@Override
	public Object tooltip(Coord c, Widget prev) {
		Equipory e = ui.gui.getEquipory();
		if (e != null) {
			WItem w = e.slots[slot(c)];
			if (w != null) {
				return w.tooltip(c, (prev == this) ? w : prev);
			}
		}
		return super.tooltip(c, prev);
	}

	@Override
	public boolean drop(Coord cc, Coord ul) {
		Equipory e = ui.gui.getEquipory();
		if (e != null) {
			e.wdgmsg("drop", slot(cc));
			return true;
		}
		return false;
	}

	@Override
	public boolean iteminteract(Coord cc, Coord ul) {
		Equipory e = ui.gui.getEquipory();
		if (e != null) {
			WItem w = e.slots[slot(cc)];
			if (w != null) {
				return w.iteminteract(cc, ul);
			}
		}
		return false;
	}

	public void activate(int i) {
		ui.modctrl = false;
		Coord c = sqoff(new Coord(i, 0)).add(rootpos());
		ui.mousedown(c, 1);
		ui.modctrl = true;
	}
}
