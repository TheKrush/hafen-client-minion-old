package haven;

import me.ender.Reflect;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

public class QualityList {
    public static final String classname = "haven.res.ui.tt.q.qbuff.QBuff";

    private final List<Quality> qualities;
    private Tex tex;
    public final Quality max;

    public QualityList(List<ItemInfo> list) {
	qualities = new LinkedList<Quality>();
	for (ItemInfo inf : list) {
	    if(inf.getClass().getName().equals(classname)) {
		String name = Reflect.getFieldValueString(inf, "name");
		int q = Reflect.getFieldValueInt(inf, "q");
		try {
		    qualities.add(new Quality(QualityType.valueOf(name), q));
		} catch (IllegalArgumentException ignored){}
	    }
	}
	if(qualities.size() > 0) {
	    boolean equal = true;
	    Quality cmax = qualities.get(0);
	    for (int i = 1; i < qualities.size(); i++) {
		Quality current = qualities.get(i);
		equal = equal && (current.value == cmax.value);
		cmax = (cmax.value >= current.value) ? cmax : current;
	    }
	    max = equal ? new Quality(QualityType.Quality, cmax.value) : cmax;
	} else {
	    max = null;
	}
    }

    public Tex tex() {
	if(tex == null) {
	    BufferedImage[] imgs = new BufferedImage[qualities.size()];
	    for (int i = 0; i < qualities.size(); i++) {
		imgs[i] = qualities.get(i).tex().back;
	    }
	    tex = new TexI(ItemInfo.catimgs(-6, imgs));
	}
	return tex;
    }

    public static class Quality {
	private final QualityType type;
	public final int value;
	private TexI tex;

	public Quality(QualityType type, int value) {
	    this.type = type;
	    this.value = value;
	}

	public TexI tex() {
	    if(tex == null) {
		String text = String.format("%d%s", value, type.c);
		BufferedImage img = Text.render(text, type.color).img;
		tex = new TexI(Utils.outline2(img, type.outline, true));
		//tex = Utils.renderOutlinedFont(Text.std, text, type.color, type.outline, 1);
	    }
	    return tex;
	}
    }

    enum QualityType {
	Essence(new Color(243, 153, 255)),
	Substance(new Color(255, 243, 153)),
	Vitality(new Color(162, 255, 153)),
	Quality(new Color(214, 255, 255));
	public final Color color, outline;
	public final char c;

	QualityType(Color color) {
	    this.color = color;
	    this.outline = Utils.blendcol(color, Color.BLACK, 0.9);
	    this.c = name().toLowerCase().charAt(0);
	}
    }
}
