package com.casxduois.launcher;

import com.casxduois.launcher.ui.CasxDuoisLauncherGUI;

import javax.swing.SwingUtilities;

public class CasxDuoisLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CasxDuoisLauncherGUI gui = new CasxDuoisLauncherGUI();
            gui.setVisible(true);
        });
    }
}
