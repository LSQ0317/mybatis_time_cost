package com.mybatis.timecost.idea;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public final class SqlSettingsConfigurable implements Configurable {
    private JPanel panel;
    private JCheckBox captureEnabledCheckBox;
    private JCheckBox logCaptureCheckBox;
    private JCheckBox httpCaptureCheckBox;
    private JCheckBox autoCopyCheckBox;
    private JSpinner portSpinner;
    private JSpinner maxEventsSpinner;
    private JSpinner slowThresholdSpinner;

    @Override
    public @Nls String getDisplayName() {
        return "MyBatis SQL 监控";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel == null) {
            panel = new JPanel(new GridBagLayout());
            captureEnabledCheckBox = new JCheckBox("启用 SQL 采集");
            logCaptureCheckBox = new JCheckBox("启用控制台日志解析");
            httpCaptureCheckBox = new JCheckBox("启用本地 HTTP 接收");
            autoCopyCheckBox = new JCheckBox("自动复制最新 SQL 到剪贴板");
            portSpinner = new JSpinner(new SpinnerNumberModel(17777, 1, 65535, 1));
            maxEventsSpinner = new JSpinner(new SpinnerNumberModel(500, 50, 5000, 50));
            slowThresholdSpinner = new JSpinner(new SpinnerNumberModel(500, 1, 600000, 50));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(8, 8, 8, 8);
            panel.add(captureEnabledCheckBox, gbc);

            gbc.gridy++;
            panel.add(logCaptureCheckBox, gbc);

            gbc.gridy++;
            panel.add(httpCaptureCheckBox, gbc);

            gbc.gridy++;
            panel.add(autoCopyCheckBox, gbc);

            gbc.gridwidth = 1;
            gbc.gridy++;
            panel.add(new JLabel("监听端口："), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(portSpinner, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("最大事件数："), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(maxEventsSpinner, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
            panel.add(new JLabel("慢查询阈值（毫秒）："), gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(slowThresholdSpinner, gbc);

            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            panel.add(new JPanel(), gbc);
        }
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        return captureEnabledCheckBox.isSelected() != settings.isCaptureEnabled()
                || logCaptureCheckBox.isSelected() != settings.isLogCaptureEnabled()
                || httpCaptureCheckBox.isSelected() != settings.isHttpCaptureEnabled()
                || autoCopyCheckBox.isSelected() != settings.isAutoCopyToClipboard()
                || ((Integer) portSpinner.getValue()) != settings.getPort()
                || ((Integer) maxEventsSpinner.getValue()) != settings.getMaxEvents()
                || ((Integer) slowThresholdSpinner.getValue()) != settings.getSlowThresholdMs();
    }

    @Override
    public void apply() {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        settings.setCaptureEnabled(captureEnabledCheckBox.isSelected());
        settings.setLogCaptureEnabled(logCaptureCheckBox.isSelected());
        settings.setHttpCaptureEnabled(httpCaptureCheckBox.isSelected());
        settings.setAutoCopyToClipboard(autoCopyCheckBox.isSelected());
        settings.setPort((Integer) portSpinner.getValue());
        settings.setMaxEvents((Integer) maxEventsSpinner.getValue());
        settings.setSlowThresholdMs((Integer) slowThresholdSpinner.getValue());
        SqlEventStore.getInstance().trimToMaxEvents();
        SqlReceiverService.getInstance().reloadConfiguration();
    }

    @Override
    public void reset() {
        SqlSettingsState settings = SqlSettingsState.getInstance();
        captureEnabledCheckBox.setSelected(settings.isCaptureEnabled());
        logCaptureCheckBox.setSelected(settings.isLogCaptureEnabled());
        httpCaptureCheckBox.setSelected(settings.isHttpCaptureEnabled());
        autoCopyCheckBox.setSelected(settings.isAutoCopyToClipboard());
        portSpinner.setValue(settings.getPort());
        maxEventsSpinner.setValue(settings.getMaxEvents());
        slowThresholdSpinner.setValue(settings.getSlowThresholdMs());
    }
}
