package haven;

import haven.Glob.Pagina;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CraftWnd extends Window {

	private static final int SZ = 20;
	private static final int PANEL_H = 24;
	private static final Coord WND_SZ = new Coord(635, 360 + PANEL_H);
	private static final Coord ICON_SZ = new Coord(SZ, SZ);
	private RecipeListBox box;
	private Tex description;
	private Widget makewnd;
	private MenuGrid menu;
	private Pagina CRAFT;
	private Breadcrumbs<Pagina> breadcrumbs;
	private static Pagina current = null;
	private ItemData data;
	private Resource resd;
	private Pagina senduse = null;

	public CraftWnd() {
		super(WND_SZ.add(0, 5), "Craft window");
	}

	@Override
	protected void attach(UI ui) {
		super.attach(ui);
		init();
	}

	@Override
	public void destroy() {
		box.destroy();
		super.destroy();
	}

	private void init() {
		box = new RecipeListBox(200, (WND_SZ.y - PANEL_H) / SZ) {
			@Override
			protected void itemclick(Recipe recipe, int button) {
				Pagina item = recipe.p;
				if (button == 1) {
					if (item == MenuGrid.bk) {
						item = current;
						if (getPaginaChildren(current, null).size() == 0) {
							item = menu.getParent(item);
						}
						item = menu.getParent(item);
					}
					menu.use(item, false);
				}
			}
		};
		add(box, new Coord(0, PANEL_H + 5));
		CRAFT = paginafor("paginae/act/craft");
		menu = ui.gui.menu;
		breadcrumbs = add(new Breadcrumbs<Pagina>(new Coord(WND_SZ.x, SZ)) {
			@Override
			public void selected(Pagina data) {
				select(data, false);
				ui.gui.menu.use(data, false);
			}
		}, new Coord(0, -2));
		Pagina selected = current;
		if (selected == null) {
			selected = menu.cur;
			if (selected == null || !menu.isCrafting(selected)) {
				selected = CRAFT;
			}
		}
		select(selected, true);
	}

	@Override
	public void cdestroy(Widget w) {
		if (w == makewnd) {
			makewnd = null;
		}
		super.cdestroy(w);
	}

	@Override
	public void wdgmsg(Widget sender, String msg, Object... args) {
		if ((sender == this) && msg.equals("close")) {
			if (makewnd != null) {
				makewnd.wdgmsg("close");
				makewnd = null;
			}
			ui.destroy(this);
			ui.gui.craftwnd = null;
			return;
		}
		super.wdgmsg(sender, msg, args);
	}

	private List<Pagina> getPaginaChildren(Pagina parent, List<Pagina> buf) {
		if (buf == null) {
			buf = new LinkedList<Pagina>();
		}
		menu.cons(parent, buf);
		return buf;
	}

	public void select(Pagina p, boolean senduse) {
		if (!menu.isCrafting(p)) {
			return;
		}
		if (box != null) {
			List<Pagina> children = getPaginaChildren(p, null);
			if (children.size() == 0) {
				children = getPaginaChildren(menu.getParent(p), null);
			} else {
				closemake();
			}
			Collections.sort(children, MenuGrid.sorter);
			if (p != CRAFT) {
				children.add(0, MenuGrid.bk);
			}
			box.setitems(children);
			box.change(p);
			setCurrent(p);
		}
		if (senduse) {
			this.senduse = p;
		}
	}

	private void closemake() {
		if (makewnd != null) {
			makewnd.wdgmsg("close");
		}
		senduse = null;
	}

	@Override
	public void cdraw(GOut g) {
		super.cdraw(g);

		if (senduse != null) {
			Pagina p = senduse;
			closemake();
			menu.senduse(p);
		}
		drawDescription(g);
	}

	public void drawDescription(GOut g) {
		if (resd == null) {
			return;
		}
		if (description == null) {
			if (data != null) {
				try {
					description = data.longtip(resd);
				} catch (Resource.Loading ignored) {
				}
			} else {
				description = MenuGrid.rendertt(resd, true, false, true).tex();
			}
		}
		if (description != null) {
			g.image(description, new Coord(box.c.x + box.sz.x + 10, PANEL_H + 5));
		}
	}

	private void setCurrent(Pagina current) {
		CraftWnd.current = current;
		updateBreadcrumbs(current);
		updateDescription(current);
	}

	private void updateBreadcrumbs(Pagina p) {
		List<Breadcrumbs.Crumb<Pagina>> crumbs = new LinkedList<Breadcrumbs.Crumb<Pagina>>();
		List<Pagina> parents = getParents(p);
		Collections.reverse(parents);
		for (Pagina item : parents) {
			BufferedImage img = item.res().layer(Resource.imgc).img;
			Resource.AButton act = item.act();
			String name = "...";
			if (act != null) {
				name = act.name;
			}
			crumbs.add(new Breadcrumbs.Crumb<Pagina>(img, name, item));
		}
		breadcrumbs.setCrumbs(crumbs);
	}

	private List<Pagina> getParents(Pagina p) {
		List<Pagina> list = new LinkedList<Pagina>();
		if (getPaginaChildren(p, null).size() > 0) {
			list.add(p);
		}
		Pagina parent;
		while ((parent = menu.getParent(p)) != null) {
			list.add(parent);
			p = parent;
		}
		return list;
	}

	private void updateDescription(Pagina p) {
		if (description != null) {
			description.dispose();
			description = null;
		}

		resd = p.res();
		data = ItemData.get(resd.name);
	}

	public void setMakewindow(Widget widget) {
		makewnd = add(widget, new Coord(box.c.x + box.sz.x + 10, box.c.y + box.sz.y - widget.sz.y));
	}

	private Pagina paginafor(String name) {
		Resource.Named res = Resource.local().load(name);
		return paginafor(res);
	}

	private Pagina paginafor(Resource.Named res) {
		return ui.sess.glob.paginafor(res);
	}

	private static class Recipe {

		public final Pagina p;
		private Tex tex = null;

		public Recipe(Pagina p) {
			this.p = p;
		}

		public Tex tex() {
			if (tex == null) {
				Resource res = p.res();
				if (res != null) {
					BufferedImage icon = PUtils.convolvedown(res.layer(Resource.imgc).img, ICON_SZ, CharWnd.iconfilter);

					Resource.AButton act = p.act();
					String name = "...";
					if (act != null) {
						name = act.name;
					} else if (p == MenuGrid.bk) {
						name = "Back";
					}
					BufferedImage text = Text.render(name).img;

					tex = new TexI(ItemInfo.catimgsh(3, icon, text));
				}

			}
			return tex;
		}
	}

	private static class RecipeListBox extends Listbox<Recipe> {

		private static final Color BGCOLOR = new Color(0, 0, 0, 113);
		private List<Pagina> list;
		private List<Recipe> recipes;

		public RecipeListBox(int w, int h) {
			super(w, h, SZ);
			bgcolor = BGCOLOR;
		}

		@Override
		protected Recipe listitem(int i) {
			if (list == null) {
				return null;
			}
			return recipes.get(i);
		}

		public void setitems(List<Pagina> list) {
			if (list.equals(this.list)) {
				return;
			}
			this.list = list;
			recipes = new LinkedList<Recipe>();
			for (Pagina p : list) {
				recipes.add(new Recipe(p));
			}
			sb.max = listitems() - h;
			sb.val = 0;
		}

		public void change(Pagina p) {
			for (Recipe r : recipes) {
				if (r.p == p) {
					change(r);
					return;
				}
			}
		}

		@Override
		public void change(Recipe item) {
			super.change(item);
			int k = recipes.indexOf(item);
			if (k >= 0) {
				if (k < sb.val) {
					sb.val = k;
				}
				if (k >= sb.val + h) {
					sb.val = Math.min(sb.max, k - h + 1);
				}
			}
		}

		@Override
		protected int listitems() {
			if (list == null) {
				return 0;
			}
			return list.size();
		}

		@Override
		protected void drawitem(GOut g, Recipe item, int i) {
			if (item == null) {
				return;
			}
			Tex tex = item.tex();
			if (tex != null) {
				g.image(tex, Coord.z);
			}
		}
	}
}
