package character_list_editor.component;

import character_list_editor.utils.LocaleManager;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Компонент для отображения списка пакетов правил с возможностью множественного выбора.
 * Каждый элемент содержит чекбокс, название пакета, автора и версию.
 * При наведении на элемент отображается всплывающая подсказка с описанием пакета.
 */
public class RulesPackageList extends JPanel {
    private List<JCheckBox> checkBoxes; // список чекбоксов для быстрого доступа к состоянию выбора

    /**
     * Создаёт список пакетов правил.
     *
     * @param packageNames  массив названий пакетов (не null)
     * @param authors       массив авторов (длина должна совпадать с packageNames)
     * @param versions      массив версий (длина должна совпадать с packageNames)
     * @param descriptions  массив описаний для всплывающих подсказок (может содержать null)
     * @throws IllegalArgumentException если любой из массивов null или их длины не совпадают
     */
    public RulesPackageList(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        checkBoxes = new ArrayList<>();
        buildList(packageNames, authors, versions, descriptions);
    }

    /**
     * Внутренний метод построения/обновления списка.
     * Удаляет все текущие компоненты и создаёт новые на основе переданных массивов.
     */
    private void buildList(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        // Очистка текущего содержимого
        removeAll();
        checkBoxes.clear();

        // Проверка входных данных
        if (packageNames == null || authors == null || versions == null || descriptions == null) {
            throw new IllegalArgumentException(LocaleManager.inst().getString("error.array_null"));
        }
        if (packageNames.length != authors.length ||
                packageNames.length != versions.length ||
                packageNames.length != descriptions.length) {
            throw new IllegalArgumentException(LocaleManager.inst().getString("error.array_length_mismatch"));
        }

        // Создание панелей для каждого пакета
        for (int i = 0; i < packageNames.length; i++) {
            // Панель элемента
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            itemPanel.setName("packageItemPanel_" + i);

            // Установка всплывающей подсказки, если описание не пустое
            String description = descriptions[i];
            if (description != null && !description.trim().isEmpty()) {
                itemPanel.setToolTipText(description);
            }

            // Чекбокс выбора
            JCheckBox checkBox = new JCheckBox();
            checkBox.setName("checkBox_" + i);
            itemPanel.add(checkBox);
            checkBoxes.add(checkBox);

            // Метка с названием пакета
            JLabel nameLabel = new JLabel(packageNames[i]);
            nameLabel.setName("nameLabel_" + i);
            itemPanel.add(nameLabel);

            // Метка с автором
            JLabel authorLabel = new JLabel(authors[i]);
            authorLabel.setName("authorLabel_" + i);
            itemPanel.add(authorLabel);

            // Метка с версией
            JLabel versionLabel = new JLabel(String.valueOf(versions[i]));
            versionLabel.setName("versionLabel_" + i);
            itemPanel.add(versionLabel);

            add(itemPanel);
        }

        // Обновление отображения
        revalidate();
        repaint();
    }

    /**
     * Возвращает список индексов выбранных пакетов.
     *
     * @return список индексов (в порядке возрастания)
     */
    public List<Integer> getSelectedIndices() {
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selected.add(i);
            }
        }
        return selected;
    }

    /**
     * Обновляет отображаемые данные.
     *
     * @param packageNames  новый массив названий пакетов
     * @param authors       новый массив авторов
     * @param versions      новый массив версий
     * @param descriptions  новый массив описаний
     * @throws IllegalArgumentException если любой из массивов null или их длины не совпадают
     */
    public void setPackages(String[] packageNames, String[] authors, int[] versions, String[] descriptions) {
        buildList(packageNames, authors, versions, descriptions);
    }
}