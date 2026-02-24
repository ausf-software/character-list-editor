package character_list_editor.component;

import character_list_editor.database.Character;
import character_list_editor.utils.CharacterManager;

import javax.swing.*;
import java.awt.BorderLayout; // добавлен импорт
import java.util.List;
import java.util.ArrayList;
import java.util.function.Predicate;

public class CharacterSheetList extends JPanel {
    private JList<Character> list;
    private DefaultListModel<Character> listModel;
    private List<Character> fullList; // полный список всех персонажей
    private Predicate<Character> currentFilter;

    public CharacterSheetList() {
        setLayout(new BorderLayout());
        listModel = new DefaultListModel<>();
        list = new JList<>(listModel);
        // Настройка отображения: показываем имя персонажа
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof Character) {
                    value = ((Character) value).getName(); // показываем имя
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    /**
     * Обновляет список персонажей из репозитория и сбрасывает фильтр.
     */
    public void refresh() {
        // Здесь должен быть вызов загрузки всех персонажей
        // Предположим, что CharacterManager имеет метод getAllCharacters()
        fullList = CharacterManager.getInstance().getAllCharacters(); // или другой источник
        if (fullList == null) {
            fullList = new ArrayList<>();
        }
        currentFilter = null;
        updateList();
    }

    /**
     * Устанавливает фильтр для отображения списка.
     * @param filter предикат для фильтрации, null - показать всех
     */
    public void setFilter(Predicate<Character> filter) {
        this.currentFilter = filter;
        updateList();
    }

    private void updateList() {
        listModel.clear();
        if (fullList == null) return;
        if (currentFilter == null) {
            for (Character ch : fullList) {
                listModel.addElement(ch);
            }
        } else {
            for (Character ch : fullList) {
                try {
                    if (currentFilter.test(ch)) {
                        listModel.addElement(ch);
                    }
                } catch (Exception e) {
                    // Логируем ошибку, но продолжаем
                    e.printStackTrace();
                }
            }
        }
    }

    public Character getSelectedCharacter() {
        return list.getSelectedValue();
    }
}