package etg.ipsipdown.launcher.net;

import java.util.List;

public class Manifest {
    public List<ManifestFile> files;

    public static class ManifestFile {
        public String path;
        public String url;
        public String hash;
        public long size;
    }
}