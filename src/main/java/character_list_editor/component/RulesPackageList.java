package character_list_editor.component;

import character_list_editor.utils.LocaleManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class RulesPackageList extends JPanel {
    private List<JCheckBox> checkBoxes;
    private List<Runnable> selectionListeners = new ArrayList<>(); // слушатели выбора

    public RulesPackageList(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        checkBoxes = new ArrayList<>();
        buildList(packageNames, authors, versions, descriptions);
    }

    /**
     * Добавляет слушатель, вызываемый при любом изменении состояния чекбокса.
     */
    public void addSelectionChangeListener(Runnable listener) {
        selectionListeners.add(listener);
    }

    private void buildList(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        removeAll();
        checkBoxes.clear();

        if (packageNames == null || authors == null || versions == null || descriptions == null) {
            throw new IllegalArgumentException(LocaleManager.inst().getString("error.array_null"));
        }
        if (packageNames.length != authors.length ||
                packageNames.length != versions.length ||
                packageNames.length != descriptions.length) {
            throw new IllegalArgumentException(LocaleManager.inst().getString("error.array_length_mismatch"));
        }

        for (int i = 0; i < packageNames.length; i++) {
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            itemPanel.setName("packageItemPanel_" + i);

            String description = descriptions[i];
            if (description != null && !description.trim().isEmpty()) {
                itemPanel.setToolTipText(description);
            }

            JCheckBox checkBox = new JCheckBox();
            checkBox.setName("checkBox_" + i);
            // Добавляем слушатель, уведомляющий всех подписчиков об изменении
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Runnable listener : selectionListeners) {
                        listener.run();
                    }
                }
            });
            itemPanel.add(checkBox);
            checkBoxes.add(checkBox);

            JLabel nameLabel = new JLabel(packageNames[i]);
            nameLabel.setName("nameLabel_" + i);
            itemPanel.add(nameLabel);

            JLabel authorLabel = new JLabel(authors[i]);
            authorLabel.setName("authorLabel_" + i);
            itemPanel.add(authorLabel);

            JLabel versionLabel = new JLabel(String.valueOf(versions[i]));
            versionLabel.setName("versionLabel_" + i);
            itemPanel.add(versionLabel);

            add(itemPanel);
        }

        revalidate();
        repaint();
    }

    public List<Integer> getSelectedIndices() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selected.add(i);
            }
        }
        return selected;
    }

    public void setPackages(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        buildList(packageNames, authors, versions, descriptions);
    }
}