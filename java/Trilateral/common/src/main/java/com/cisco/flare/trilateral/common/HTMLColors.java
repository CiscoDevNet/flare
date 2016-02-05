package com.cisco.flare.trilateral.common;

import android.graphics.Color;

import java.util.HashMap;

/**
 * Created by azamlerc on 2/3/16.
 */
public class HTMLColors {
	public static HashMap<String, String> mHtmlColorNames;

	static {
		initHtmlColorNames();
	}

	private static void initHtmlColorNames() {
		mHtmlColorNames = new HashMap<>();
		mHtmlColorNames.put("aliceblue", "#F0F8FF");
		mHtmlColorNames.put("antiquewhite", "#FAEBD7");
		mHtmlColorNames.put("aqua", "#00FFFF");
		mHtmlColorNames.put("aquamarine", "#7FFFD4");
		mHtmlColorNames.put("azure", "#F0FFFF");
		mHtmlColorNames.put("beige", "#F5F5DC");
		mHtmlColorNames.put("bisque", "#FFE4C4");
		mHtmlColorNames.put("black", "#000000");
		mHtmlColorNames.put("blanchedalmond", "#FFEBCD");
		mHtmlColorNames.put("blue", "#0000FF");
		mHtmlColorNames.put("blueviolet", "#8A2BE2");
		mHtmlColorNames.put("brown", "#A52A2A");
		mHtmlColorNames.put("burlywood", "#DEB887");
		mHtmlColorNames.put("cadetblue", "#5F9EA0");
		mHtmlColorNames.put("chartreuse", "#7FFF00");
		mHtmlColorNames.put("chocolate", "#D2691E");
		mHtmlColorNames.put("coral", "#FF7F50");
		mHtmlColorNames.put("cornflowerblue", "#6495ED");
		mHtmlColorNames.put("cornsilk", "#FFF8DC");
		mHtmlColorNames.put("crimson", "#DC143C");
		mHtmlColorNames.put("cyan", "#00FFFF");
		mHtmlColorNames.put("darkblue", "#00008B");
		mHtmlColorNames.put("darkcyan", "#008B8B");
		mHtmlColorNames.put("darkgoldenrod", "#B8860B");
		mHtmlColorNames.put("darkgray", "#A9A9A9");
		mHtmlColorNames.put("darkgrey", "#A9A9A9");
		mHtmlColorNames.put("darkgreen", "#006400");
		mHtmlColorNames.put("darkkhaki", "#BDB76B");
		mHtmlColorNames.put("darkmagenta", "#8B008B");
		mHtmlColorNames.put("darkolivegreen", "#556B2F");
		mHtmlColorNames.put("darkorange", "#FF8C00");
		mHtmlColorNames.put("darkorchid", "#9932CC");
		mHtmlColorNames.put("darkred", "#8B0000");
		mHtmlColorNames.put("darksalmon", "#E9967A");
		mHtmlColorNames.put("darkseagreen", "#8FBC8F");
		mHtmlColorNames.put("darkslateblue", "#483D8B");
		mHtmlColorNames.put("darkslategray", "#2F4F4F");
		mHtmlColorNames.put("darkslategrey", "#2F4F4F");
		mHtmlColorNames.put("darkturquoise", "#00CED1");
		mHtmlColorNames.put("darkviolet", "#9400D3");
		mHtmlColorNames.put("deeppink", "#FF1493");
		mHtmlColorNames.put("deepskyblue", "#00BFFF");
		mHtmlColorNames.put("dimgray", "#696969");
		mHtmlColorNames.put("dimgrey", "#696969");
		mHtmlColorNames.put("dodgerblue", "#1E90FF");
		mHtmlColorNames.put("firebrick", "#B22222");
		mHtmlColorNames.put("floralwhite", "#FFFAF0");
		mHtmlColorNames.put("forestgreen", "#228B22");
		mHtmlColorNames.put("fuchsia", "#FF00FF");
		mHtmlColorNames.put("gainsboro", "#DCDCDC");
		mHtmlColorNames.put("ghostwhite", "#F8F8FF");
		mHtmlColorNames.put("gold", "#FFD700");
		mHtmlColorNames.put("goldenrod", "#DAA520");
		mHtmlColorNames.put("gray", "#808080");
		mHtmlColorNames.put("grey", "#808080");
		mHtmlColorNames.put("green", "#008000");
		mHtmlColorNames.put("greenyellow", "#ADFF2F");
		mHtmlColorNames.put("honeydew", "#F0FFF0");
		mHtmlColorNames.put("hotpink", "#FF69B4");
		mHtmlColorNames.put("indianred ", "#CD5C5C");
		mHtmlColorNames.put("indigo ", "#4B0082");
		mHtmlColorNames.put("ivory", "#FFFFF0");
		mHtmlColorNames.put("khaki", "#F0E68C");
		mHtmlColorNames.put("lavender", "#E6E6FA");
		mHtmlColorNames.put("lavenderblush", "#FFF0F5");
		mHtmlColorNames.put("lawngreen", "#7CFC00");
		mHtmlColorNames.put("lemonchiffon", "#FFFACD");
		mHtmlColorNames.put("lightblue", "#ADD8E6");
		mHtmlColorNames.put("lightcoral", "#F08080");
		mHtmlColorNames.put("lightcyan", "#E0FFFF");
		mHtmlColorNames.put("lightgoldenrodyellow", "#FAFAD2");
		mHtmlColorNames.put("lightgray", "#D3D3D3");
		mHtmlColorNames.put("lightgrey", "#D3D3D3");
		mHtmlColorNames.put("lightgreen", "#90EE90");
		mHtmlColorNames.put("lightpink", "#FFB6C1");
		mHtmlColorNames.put("lightsalmon", "#FFA07A");
		mHtmlColorNames.put("lightseagreen", "#20B2AA");
		mHtmlColorNames.put("lightskyblue", "#87CEFA");
		mHtmlColorNames.put("lightslategray", "#778899");
		mHtmlColorNames.put("lightslategrey", "#778899");
		mHtmlColorNames.put("lightsteelblue", "#B0C4DE");
		mHtmlColorNames.put("lightyellow", "#FFFFE0");
		mHtmlColorNames.put("lime", "#00FF00");
		mHtmlColorNames.put("limegreen", "#32CD32");
		mHtmlColorNames.put("linen", "#FAF0E6");
		mHtmlColorNames.put("magenta", "#FF00FF");
		mHtmlColorNames.put("maroon", "#800000");
		mHtmlColorNames.put("mediumaquamarine", "#66CDAA");
		mHtmlColorNames.put("mediumblue", "#0000CD");
		mHtmlColorNames.put("mediumorchid", "#BA55D3");
		mHtmlColorNames.put("mediumpurple", "#9370DB");
		mHtmlColorNames.put("mediumseagreen", "#3CB371");
		mHtmlColorNames.put("mediumslateblue", "#7B68EE");
		mHtmlColorNames.put("mediumspringgreen", "#00FA9A");
		mHtmlColorNames.put("mediumturquoise", "#48D1CC");
		mHtmlColorNames.put("mediumvioletred", "#C71585");
		mHtmlColorNames.put("midnightblue", "#191970");
		mHtmlColorNames.put("mintcream", "#F5FFFA");
		mHtmlColorNames.put("mistyrose", "#FFE4E1");
		mHtmlColorNames.put("moccasin", "#FFE4B5");
		mHtmlColorNames.put("navajowhite", "#FFDEAD");
		mHtmlColorNames.put("navy", "#000080");
		mHtmlColorNames.put("oldlace", "#FDF5E6");
		mHtmlColorNames.put("olive", "#808000");
		mHtmlColorNames.put("olivedrab", "#6B8E23");
		mHtmlColorNames.put("orange", "#FFA500");
		mHtmlColorNames.put("orangered", "#FF4500");
		mHtmlColorNames.put("orchid", "#DA70D6");
		mHtmlColorNames.put("palegoldenrod", "#EEE8AA");
		mHtmlColorNames.put("palegreen", "#98FB98");
		mHtmlColorNames.put("paleturquoise", "#AFEEEE");
		mHtmlColorNames.put("palevioletred", "#DB7093");
		mHtmlColorNames.put("papayawhip", "#FFEFD5");
		mHtmlColorNames.put("peachpuff", "#FFDAB9");
		mHtmlColorNames.put("peru", "#CD853F");
		mHtmlColorNames.put("pink", "#FFC0CB");
		mHtmlColorNames.put("plum", "#DDA0DD");
		mHtmlColorNames.put("powderblue", "#B0E0E6");
		mHtmlColorNames.put("purple", "#800080");
		mHtmlColorNames.put("rebeccapurple", "#663399");
		mHtmlColorNames.put("red", "#FF0000");
		mHtmlColorNames.put("rosybrown", "#BC8F8F");
		mHtmlColorNames.put("royalblue", "#4169E1");
		mHtmlColorNames.put("saddlebrown", "#8B4513");
		mHtmlColorNames.put("salmon", "#FA8072");
		mHtmlColorNames.put("sandybrown", "#F4A460");
		mHtmlColorNames.put("seagreen", "#2E8B57");
		mHtmlColorNames.put("seashell", "#FFF5EE");
		mHtmlColorNames.put("sienna", "#A0522D");
		mHtmlColorNames.put("silver", "#C0C0C0");
		mHtmlColorNames.put("skyblue", "#87CEEB");
		mHtmlColorNames.put("slateblue", "#6A5ACD");
		mHtmlColorNames.put("slategray", "#708090");
		mHtmlColorNames.put("slategrey", "#708090");
		mHtmlColorNames.put("snow", "#FFFAFA");
		mHtmlColorNames.put("springgreen", "#00FF7F");
		mHtmlColorNames.put("steelblue", "#4682B4");
		mHtmlColorNames.put("tan", "#D2B48C");
		mHtmlColorNames.put("teal", "#008080");
		mHtmlColorNames.put("thistle", "#D8BFD8");
		mHtmlColorNames.put("tomato", "#FF6347");
		mHtmlColorNames.put("turquoise", "#40E0D0");
		mHtmlColorNames.put("violet", "#EE82EE");
		mHtmlColorNames.put("wheat", "#F5DEB3");
		mHtmlColorNames.put("white", "#FFFFFF");
		mHtmlColorNames.put("whitesmoke", "#F5F5F5");
		mHtmlColorNames.put("yellow", "#FFFF00");
		mHtmlColorNames.put("yellowgreen", "#9ACD32");
		mHtmlColorNames.put("oak", "#DEB887");
		mHtmlColorNames.put("birch", "#F5DEB3");
	}

	public static int getHtmlColor(String name, int defaultValue) {
		try {
			String colorName = mHtmlColorNames.get(name);
			return Color.parseColor(colorName);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static int getHtmlColor(String name) {
		return getHtmlColor(name, Color.parseColor("white"));
	}
}
