package com.mybatis.timecost.idea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.table.JBTable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.datatransfer.StringSelection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class SqlToolWindowPanel extends JPanel implements SqlEventListener {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final SqlEventTableModel tableModel = new SqlEventTableModel();
    private final JBTable eventTable = new JBTable(tableModel);
    private final JBTextArea detailArea = new JBTextArea();
    private final JLabel statusLabel = new JLabel("", SwingConstants.LEFT);
    private final JLabel portLabel = new JLabel("", SwingConstants.LEFT);
    private final JCheckBox enableCaptureCheckBox = new JCheckBox("启用采集");
    private final SqlSettingsListener settingsListener = this::handleSettingsChanged;

    public SqlToolWindowPanel() {
        super(new BorderLayout());
        buildUi();
        loadExistingEvents();
        SqlEventStore.getInstance().addListener(this);
        SqlSettingsState.getInstance().addListener(settingsListener);
        refreshStatus();
    }

    private void buildUi() {
        enableCaptureCheckBox.addActionListener(event -> toggleCapture());

        JButton copyButton = new JButton("复制 SQL");
        copyButton.addActionListener(event -> copySelectedSql());

        JButton clearButton = new JButton("清空");
        clearButton.addActionListener(event -> clearEvents());

        JButton settingsButton = new JButton("设置");
        settingsButton.addActionListener(event ->
                ShowSettingsUtil.getInstance().showSettingsDialog(null, SqlSettingsConfigurable.class));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolbar.add(enableCaptureCheckBox);
        toolbar.add(copyButton);
        toolbar.add(clearButton);
        toolbar.add(settingsButton);
        toolbar.add(statusLabel);
        toolbar.add(portLabel);

        eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        eventTable.setDefaultRenderer(Object.class, new SqlEventCellRenderer());
        eventTable.getSelectionModel().addListSelectionListener(this::onSelectionChanged);
        eventTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        eventTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        eventTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        eventTable.getColumnModel().getColumn(3).setPreferredWidth(700);

        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);

        OnePixelSplitter splitter = new OnePixelSplitter(false, 0.45f);
        splitter.setFirstComponent(new JBScrollPane(eventTable));
        splitter.setSecondComponent(new JBScrollPane(detailArea));

        add(toolbar, BorderLayout.NORTH);
        add(splitter, BorderLayout.CENTER);
    }

    private void loadExistingEvents() {
        tableModel.setEvents(SqlEventStore.getInstance().getEvents());
        if (tableModel.getRowCount() > 0) {
            eventTable.setRowSelectionInterval(0, 0);
        }
    }

    @Override
    public void onSqlEvent(SqlEvent event) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (event == null) {
                tableModel.setEvents(SqlEventStore.getInstance().getEvents());
                detailArea.setText("");
            } else {
                tableModel.setEvents(SqlEventStore.getInstance().getEvents());
                if (tableModel.getRowCount() > 0) {
                    eventTable.setRowSelectionInterval(0, 0);
                }
            }
            refreshStatus();
        });
    }

    private void onSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = eventTable.getSelectedRow();
        SqlEvent sqlEvent = row >= 0 ? tableModel.getEventAt(row) : null;
        updateDetail(sqlEvent);
    }

    private void updateDetail(SqlEvent event) {
        if (event == null) {
            detailArea.setText("");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Time: ").append(TIME_FORMATTER.format(Instant.ofEpochMilli(event.getReceivedAt()))).append('\n');
        sb.append("Duration: ").append(DurationFormatUtil.formatWithRaw(event.getDurationMs())).append('\n');
        sb.append("Mapper: ").append(valueOrDefault(event.getMapperId())).append('\n');
        sb.append("Thread: ").append(valueOrDefault(event.getThreadName())).append("\n\n");
        sb.append(event.getSql());
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void toggleCapture() {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        settings.setCaptureEnabled(enableCaptureCheckBox.isSelected());
        SqlReceiverService.getInstance().reloadConfiguration();
        settings.notifySettingsChanged();
        refreshStatus();
    }

    @Override
    public void removeNotify() {
        SqlSettingsState.getInstance().removeListener(settingsListener);
        super.removeNotify();
    }

    private void handleSettingsChanged() {
        ApplicationManager.getApplication().invokeLater(this::refreshStatus);
    }

    private void copySelectedSql() {
        int row = eventTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        SqlEvent event = tableModel.getEventAt(row);
        CopyPasteManager.getInstance().setContents(new StringSelection(event.getSql()));
    }

    private void clearEvents() {
        SqlEventStore.getInstance().clear();
    }

    private void refreshStatus() {
        SqlReceiverService receiverService = SqlReceiverService.getInstance();
        SqlSettingsState settings = SqlSettingsState.getInstance();
        enableCaptureCheckBox.setSelected(settings.isCaptureEnabled());
        statusLabel.setText("事件数：" + tableModel.getRowCount() + " | 服务："
                + (receiverService.isStarted() ? "运行中" : "已停止"));
        int configuredPort = settings.getPort();
        int activePort = receiverService.getCurrentPort();
        if (receiverService.isStarted() && activePort > 0 && activePort != configuredPort) {
            portLabel.setText("端口：" + activePort + "（配置：" + configuredPort + "）");
            return;
        }
        portLabel.setText("端口：" + (receiverService.isStarted() ? activePort : configuredPort));
    }

    private static String valueOrDefault(String value) {
        return value != null ? value : "n/a";
    }

    private static String compact(String sql) {
        return sql.replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
    }

    private static final class SqlEventTableModel extends AbstractTableModel {
        private final String[] columns = {"时间", "耗时", "Mapper", "SQL"};
        private List<SqlEvent> events = new ArrayList<>();

        public void setEvents(List<SqlEvent> events) {
            this.events = new ArrayList<>(events);
            fireTableDataChanged();
        }

        public SqlEvent getEventAt(int rowIndex) {
            return events.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return events.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            SqlEvent event = events.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> TIME_FORMATTER.format(Instant.ofEpochMilli(event.getReceivedAt()));
                case 1 -> DurationFormatUtil.format(event.getDurationMs());
                case 2 -> valueOrDefault(event.getMapperId());
                case 3 -> compact(event.getSql());
                default -> "";
            };
        }
    }

    private static final class SqlEventCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && table.getModel() instanceof SqlEventTableModel model) {
                SqlEvent event = model.getEventAt(row);
                int slowThresholdMs = SqlSettingsState.getInstance().getSlowThresholdMs();
                if (event.getDurationMs() != null && event.getDurationMs() >= slowThresholdMs) {
                    component.setForeground(JBColor.RED);
                } else {
                    component.setForeground(JBColor.foreground());
                }
            }
            return component;
        }
    }
}
