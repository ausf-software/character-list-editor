package character_list_editor.window;

import character_list_editor.component.TagPanel;
import character_list_editor.database.Character;
import character_list_editor.database.CharacterRepository;
import character_list_editor.database.Tag;
import character_list_editor.utils.LocaleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.ArrayList;

public class TagEditWindow extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(TagEditWindow.class);
    private static final int MAX_TAG_LENGTH = 50;

    private final Character character;
    private final LocaleManager localeManager;

    // UI Components
    private JLabel labelTagName;
    private JTextField tagNameTextField;
    private JLabel labelTagColor;
    private JButton chooseColorButton;
    private TagPanel tagPreviewPanel;
    private JButton addButton;
    private JButton cancelButton;

    private Color selectedColor = Color.LIGHT_GRAY;

    public TagEditWindow(Frame owner, Character character) {
        super(owner, "Добавить тег", true);

        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null");
        }

        this.character = character;
        this.localeManager = LocaleManager.inst();

        initComponents();
        setupLayout();
        setupListeners();
        updateAddButtonState();

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        labelTagName = new JLabel();
        labelTagName.setName("labelTagName");

        tagNameTextField = new JTextField(20);
        tagNameTextField.setName("tagNameTextField");

        labelTagColor = new JLabel();
        labelTagColor.setName("labelTagColor");

        chooseColorButton = new JButton();
        chooseColorButton.setName("chooseColorButton");
        chooseColorButton.setBackground(selectedColor);
        chooseColorButton.setOpaque(true);
        chooseColorButton.setPreferredSize(new Dimension(25, 25));

        tagPreviewPanel = new TagPanel(selectedColor, "");
        tagPreviewPanel.setName("tagPreviewPanel");

        addButton = new JButton();
        addButton.setName("addButton");
        addButton.setEnabled(false);

        cancelButton = new JButton();
        cancelButton.setName("cancelButton");

        updateTexts();
    }

    private void updateTexts() {
        labelTagName.setText(localeManager.getString("dialog.addtag.label.name"));
        labelTagColor.setText(localeManager.getString("dialog.addtag.label.color"));
        chooseColorButton.setText(localeManager.getString("dialog.addtag.button.choose"));
        addButton.setText(localeManager.getString("dialog.addtag.button.add"));
        cancelButton.setText(localeManager.getString("dialog.addtag.button.cancel"));
        setTitle(localeManager.getString("dialog.addtag.title"));
    }

    private void setupLayout() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(labelTagName, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(tagNameTextField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(labelTagColor, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JPanel colorButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        colorButtonPanel.add(chooseColorButton);
        mainPanel.add(colorButtonPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 5, 5, 5);
        mainPanel.add(tagPreviewPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addButton);
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        tagNameTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updatePreviewAndButton();
            }
        });

        chooseColorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(
                    TagEditWindow.this,
                    localeManager.getString("dialog.addtag.colorchooser.title"),
                    selectedColor
            );

            if (newColor != null) {
                selectedColor = newColor;
                chooseColorButton.setBackground(selectedColor);
                tagPreviewPanel.setBackgroundColor(selectedColor);
            }
        });

        addButton.addActionListener(e -> saveTag());
        cancelButton.addActionListener(e -> dispose());

        tagNameTextField.addActionListener(e -> {
            if (addButton.isEnabled()) {
                saveTag();
            }
        });
    }

    private void updatePreviewAndButton() {
        String text = tagNameTextField.getText();
        tagPreviewPanel.setText(text);
        updateAddButtonState();
    }

    private void updateAddButtonState() {
        String text = tagNameTextField.getText();
        boolean isValid = text != null && !text.trim().isEmpty() && text.trim().length() <= MAX_TAG_LENGTH;
        addButton.setEnabled(isValid);
    }

    private void saveTag() {
        String tagName = tagNameTextField.getText().trim();

        if (tagName.isEmpty() || tagName.length() > MAX_TAG_LENGTH) {
            return;
        }

        try {
            Tag tag = new Tag();
            tag.setName(tagName);
            tag.setColor(selectedColor);

            Tag savedTag = CharacterRepository.getInstance().addTag(tag);

            if (character.getTags() == null) {
                character.setTags(new ArrayList<>());
            }

            boolean tagExists = character.getTags().stream()
                    .anyMatch(t -> t.getId() == savedTag.getId() ||
                            t.getName().equalsIgnoreCase(savedTag.getName()));

            if (!tagExists) {
                character.getTags().add(savedTag);
                CharacterRepository.getInstance().updateCharacter(character);
            }

            logger.info("Tag '{}' added to character '{}'", tagName, character.getName());
            dispose();

        } catch (SQLException e) {
            logger.error("Error saving tag: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(
                    this,
                    localeManager.getString("dialog.addtag.error.database") + "\n" + e.getMessage(),
                    localeManager.getString("dialog.addtag.error.title"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // Getters for testing
    public JLabel getLabelTagName() { return labelTagName; }
    public JTextField getTagNameTextField() { return tagNameTextField; }
    public JLabel getLabelTagColor() { return labelTagColor; }
    public JButton getChooseColorButton() { return chooseColorButton; }
    public TagPanel getTagPreviewPanel() { return tagPreviewPanel; }
    public JButton getAddButton() { return addButton; }
    public JButton getCancelButton() { return cancelButton; }
}
