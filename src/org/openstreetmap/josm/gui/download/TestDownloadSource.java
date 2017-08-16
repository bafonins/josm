package org.openstreetmap.josm.gui.download;

import org.openstreetmap.josm.data.Bounds;

import javax.swing.JLabel;

public class TestDownloadSource implements DownloadSource<Object>{

    @Override
    public AbstractDownloadSourcePanel<Object> createPanel() {
        return new TestDownloadSourcePanel(this);
    }

    @Override
    public void doDownload(Bounds bbox, Object data, DownloadSettings settings) {

    }

    @Override
    public String getLabel() {
        return "test";
    }

    @Override
    public void addGui(DownloadDialog dialog) {

    }

    @Override
    public boolean onlyExpert() {
        return true;
    }

    static class TestDownloadSourcePanel extends AbstractDownloadSourcePanel<Object> {

        public TestDownloadSourcePanel(DownloadSource<Object> downloadSource) {
            super(downloadSource);
            add(new JLabel("test"));
        }

        @Override
        public Object getData() {
            return new Object();
        }

        @Override
        public void rememberSettings() {

        }

        @Override
        public void restoreSettings() {

        }

        @Override
        public boolean handleDownload(Bounds bbox, Object data, DownloadSettings settings) {
            return false;
        }
    }
}
