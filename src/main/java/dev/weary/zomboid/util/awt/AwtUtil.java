package dev.weary.zomboid.util.awt;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import dev.weary.zomboid.agent.ui.UI;

public class AwtUtil {
	private static final Map<Long, Icon> WINDOWS_ID_TO_ICON = new HashMap<>();
	private static final Map<String, Long> WINDOWS_ICON_IDS = new HashMap<>();
	static {
		WINDOWS_ICON_IDS.put("OptionPane.warningIcon", 65581L);
		WINDOWS_ICON_IDS.put("OptionPane.questionIcon", 65583L);
		WINDOWS_ICON_IDS.put("OptionPane.errorIcon", 65587L); // was 65585
		WINDOWS_ICON_IDS.put("OptionPane.informationIcon", 65585L); // was 65587
	}

	private static Method methodGetIconBits;
	static {
		try {
			methodGetIconBits = Class.forName("sun.awt.shell.Win32ShellFolder2").getDeclaredMethod("getIconBits", long.class, int.class);
			methodGetIconBits.setAccessible(true);
		}
		catch (Exception ignored) {}
	}

	public static Icon getWindowsIconById(long iconId) {
		return WINDOWS_ID_TO_ICON.get(iconId);
	}

	public static void setNativeLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ignored) {}
	}

	public static void fixWindowsDialogIcons() {
		int iconSize = getScaledWindowsIconSize();

		WINDOWS_ICON_IDS.forEach((iconKey, iconId) -> {
			if (UIManager.get(iconKey) instanceof ImageIcon) {
				int[] iconBits = getIconBits(iconId, iconSize);
				if (iconBits != null) {
					BufferedImage iconImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
					iconImage.setRGB(0, 0, iconSize, iconSize, iconBits, 0, iconSize);
					ImageIcon newIcon = new ImageIcon(iconImage);
					WINDOWS_ID_TO_ICON.put(iconId, newIcon);
					UIManager.put(iconKey, newIcon);
				}
			}
		});
	}

	private static int[] getIconBits(long iconId, int iconSize) {
		try {
			return (int[]) methodGetIconBits.invoke(null, iconId, iconSize);
		}
		catch (Exception ignored) {}
		return null;
	}

	private static int getScaledWindowsIconSize() {
		final double dpiScalingFactor = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
		if (dpiScalingFactor == 1) {
			return 32;
		}
		else if (dpiScalingFactor == 1.25) {
			return 40;
		}
		else if (dpiScalingFactor == 1.5) {
			return 45;
		}

		return (int) (dpiScalingFactor * 32);
	}
}
